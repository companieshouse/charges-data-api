package uk.gov.companieshouse.charges.data.service;

import static uk.gov.companieshouse.charges.data.ChargesDataApiApplication.NAMESPACE;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.api.metrics.MortgageApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.api.CompanyMetricsApiService;
import uk.gov.companieshouse.charges.data.exception.BadRequestException;
import uk.gov.companieshouse.charges.data.exception.ConflictException;
import uk.gov.companieshouse.charges.data.exception.NotFoundException;
import uk.gov.companieshouse.charges.data.exception.ServiceUnavailableException;
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;
import uk.gov.companieshouse.charges.data.model.ChargesAggregate;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.RequestCriteria;
import uk.gov.companieshouse.charges.data.model.ResourceChangedRequest;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;
import uk.gov.companieshouse.charges.data.util.DateUtils;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class ChargesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String GET_CHARGE_MESSAGE = "Charge: %s not found for company: %s";
    private static final String FIND_CHARGES_MESSAGE = "Charges does not exist for company: %s";

    private final ChargesApiService chargesApiService;
    private final ChargesTransformer chargesTransformer;
    private final ChargesRepository chargesRepository;
    private final CompanyMetricsApiService companyMetricsApiService;


    /**
     * ChargesService constructor.
     *
     * @param chargesRepository chargesRepository.
     */
    public ChargesService(final ChargesRepository chargesRepository,
            final ChargesTransformer chargesTransformer,
            ChargesApiService chargesApiService,
            CompanyMetricsApiService companyMetricsApiService) {
        this.chargesRepository = chargesRepository;
        this.chargesTransformer = chargesTransformer;
        this.chargesApiService = chargesApiService;
        this.companyMetricsApiService = companyMetricsApiService;
    }

    /**
     * Save or Update charges.
     *
     * @param companyNumber company number for charge.
     * @param chargeId      charges Id.
     * @param requestBody   request body.
     */
    public void upsertCharges(String contextId, String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        try {
            Optional<ChargesDocument> chargesDocumentOptional = chargesRepository.findById(chargeId);

            chargesDocumentOptional.ifPresentOrElse(existingDocument -> {
                        OffsetDateTime deltaAtFromBodyRequest = requestBody.getInternalData().getDeltaAt();
                        OffsetDateTime deltaAtFromDb = existingDocument.getDeltaAt();

                        if (deltaAtFromBodyRequest == null
                                || deltaAtFromDb == null || !deltaAtFromBodyRequest.isBefore(deltaAtFromDb)) {
                            ChargesDocument charges =
                                    this.chargesTransformer.transform(companyNumber, chargeId, requestBody);

                            saveAndInvokeChsKafkaApi(contextId, companyNumber, chargeId, charges);
                            } else {
                            LOGGER.error("Charge not saved as record provided is older than the one already stored",
                                    DataMapHolder.getLogMap());
                        }
                    },
                    () -> {
                        ChargesDocument charges =
                                this.chargesTransformer.transform(companyNumber, chargeId, requestBody);
                        saveAndInvokeChsKafkaApi(contextId, companyNumber, chargeId, charges);
                    });
        } catch (DataAccessException ex) {
            LOGGER.error("Error occurred during a DB call for PUT charges");
            throw new ServiceUnavailableException("Error occurred during a DB call for PUT charges");
        }

    }

    /**
     * Retrieve a company charge details using a company number and chargeId.
     *
     * @param companyNumber the company number of the company.
     * @param chargeId      the chargeId.
     * @return charge details.
     */
    public ChargeApi getChargeDetails(final String companyNumber, final String chargeId) {
        try {
            Optional<ChargesDocument> chargesDocuments =
                    this.chargesRepository.findChargeDetails(companyNumber, chargeId);
            return chargesDocuments.map(ChargesDocument::getData).orElseThrow(() -> new NotFoundException(
                    String.format(GET_CHARGE_MESSAGE, chargeId, companyNumber)));
        } catch (DataAccessException ex) {
            LOGGER.error("Error occurred during a DB call for GET charge", ex);
            throw new ServiceUnavailableException("Error occurred during a DB call for GET charge");
        }
    }

    /**
     * Find charges for company number.
     *
     * @param companyNumber company Number.
     * @return charges.
     */
    public ChargesApi findCharges(final String companyNumber,
            final RequestCriteria requestCriteria) {
        try {
            List<String> statusFilter = new ArrayList<>();
            if ("outstanding".equals(requestCriteria.getFilter())) {
                statusFilter.add(ChargeApi.StatusEnum.SATISFIED.toString());
                statusFilter.add(ChargeApi.StatusEnum.FULLY_SATISFIED.toString());
            }
            ChargesAggregate chargesAggregate =
                    chargesRepository.findCharges(companyNumber, statusFilter,
                            Optional.ofNullable(requestCriteria.getStartIndex()).orElse(0),
                            Math.min(Optional.ofNullable(requestCriteria.getItemsPerPage()).orElse(25), 100));

            Optional<MetricsApi> companyMetrics =
                    companyMetricsApiService.getCompanyMetrics(companyNumber);

            if (companyMetrics.isEmpty()) {
                LOGGER.error("No company metrics data found for company", DataMapHolder.getLogMap());
            }
            return createChargesApi(chargesAggregate, companyMetrics);
        } catch (DataAccessException ex) {
            LOGGER.error("Error occurred during a DB call for GET charges", ex);
            throw new ServiceUnavailableException("Error occurred during a DB call for GET charges");
        }
    }

    private ChargesApi createChargesApi(ChargesAggregate chargesAggregate,
            Optional<MetricsApi> metrics) {
        var chargesApi = new ChargesApi();
        chargesAggregate.getChargesDocuments().forEach(
                charge -> chargesApi.addItemsItem(charge.getData()));

        if (chargesAggregate.getTotalCharges().isEmpty()) {
            chargesApi.setTotalCount(0);
        } else {
            chargesApi.setTotalCount(
                    chargesAggregate.getTotalCharges().get(0).getCount().intValue());
        }

        MortgageApi mortgage = null;

        if (metrics.isPresent()) {
            chargesApi.setEtag(metrics.get().getEtag());
            mortgage = metrics.get().getMortgage();
        }

        chargesApi.setSatisfiedCount(integerDefaultZero(mortgage == null ? null :
                mortgage.getSatisfiedCount()));
        chargesApi.setPartSatisfiedCount(integerDefaultZero(mortgage == null ? null :
                mortgage.getPartSatisfiedCount()));
        chargesApi.setUnfilteredCount(integerDefaultZero(mortgage == null ? null :
                mortgage.getTotalCount()));

        return chargesApi;
    }

    private int integerDefaultZero(Integer integer) {
        return integer == null ? 0 : integer;
    }

    private void saveAndInvokeChsKafkaApi(String contextId, String companyNumber,
            String chargeId, ChargesDocument charges) {
        chargesRepository.save(charges);
        chargesApiService.invokeChsKafkaApi(new ResourceChangedRequest(contextId, chargeId, companyNumber,
                null, false));
        LOGGER.info("ChsKafka api CHANGED invoked successfully", DataMapHolder.getLogMap());
    }

    /**
     * Delete charge from company mortgages.
     *
     * @param contextId the x-request-id.
     * @param chargeId  the charge identifier.
     * @param companyNumber the requests' company number.
     * @param requestDeltaAt the requests' deltaAt.
     */
    public void deleteCharge(String contextId, String companyNumber, String chargeId, String requestDeltaAt) {
        if (StringUtils.isBlank(requestDeltaAt)) {
            LOGGER.error("deltaAt missing from delete request");
            throw new BadRequestException("deltaAt missing from delete request");
        }

        try {
            Optional<ChargesDocument> chargesDocumentOptional = chargesRepository.findById(chargeId);

            chargesDocumentOptional.ifPresentOrElse(doc -> {
                OffsetDateTime existingDeltaAt = doc.getDeltaAt();
                if (DateUtils.isDeltaStale(requestDeltaAt, existingDeltaAt)) {
                    LOGGER.error(String.format("Stale delta received; request delta_at: [%s] is not after existing delta_at: [%s]",
                            requestDeltaAt, existingDeltaAt));
                    throw new ConflictException("Stale delta received.");
                }

                chargesRepository.deleteById(chargeId);
                LOGGER.info("Company charge deleted successfully in MongoDB", DataMapHolder.getLogMap());

                chargesApiService.invokeChsKafkaApiDelete(new ResourceChangedRequest(contextId, chargeId,
                        companyNumber, doc.getData(), true));
            }, () -> {
                LOGGER.info(String.format("Company charge doesn't exist in company mortgages "
                        + "with %s header x-request-id %s", chargeId, contextId));

                chargesApiService.invokeChsKafkaApiDelete(new ResourceChangedRequest(contextId, chargeId,
                        companyNumber, null, true));
            });
            LOGGER.info("ChsKafka api DELETED invoked successfully", DataMapHolder.getLogMap());
        } catch (DataAccessException dbException) {
            LOGGER.error("Error occurred during a DB call for delete", DataMapHolder.getLogMap());
            throw new ServiceUnavailableException("Error occurred during a DB call for delete");
        }
    }

}
