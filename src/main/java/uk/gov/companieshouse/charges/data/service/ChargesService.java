package uk.gov.companieshouse.charges.data.service;

import static uk.gov.companieshouse.charges.data.ChargesDataApiApplication.NAMESPACE;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;
import uk.gov.companieshouse.charges.data.model.ChargesAggregate;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.RequestCriteria;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class ChargesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

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
    }

    /**
     * Retrieve a company charge details using a company number and chargeId.
     *
     * @param companyNumber the company number of the company.
     * @param chargeId      the chargeId.
     * @return charge details.
     */
    public Optional<ChargeApi> getChargeDetails(final String companyNumber, final String chargeId) {
        Optional<ChargesDocument> chargesDocuments =
                this.chargesRepository.findChargeDetails(companyNumber, chargeId);
        if (chargesDocuments.isEmpty()) {
            LOGGER.trace("Company charges not found for company", DataMapHolder.getLogMap());
            return Optional.empty();
        }
        return chargesDocuments.map(ChargesDocument::getData);
    }

    /**
     * Find charges for company number.
     *
     * @param companyNumber company Number.
     * @return charges.
     */
    public Optional<ChargesApi> findCharges(final String companyNumber,
            final RequestCriteria requestCriteria) {
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
        return Optional.of(createChargesApi(chargesAggregate, companyMetrics));
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
        ApiResponse<Void> res = chargesApiService.invokeChsKafkaApi(contextId,
                companyNumber,
                chargeId);
        HttpStatus httpStatus = res != null ? HttpStatus.resolve(res.getStatusCode()) : null;
        if (httpStatus == null || !httpStatus.is2xxSuccessful()) {
            throw new ResponseStatusException(httpStatus != null
                    ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR, "invokeChsKafkaApi");
        }
        LOGGER.info("ChsKafka api CHANGED invoked successfully", DataMapHolder.getLogMap());

    }

    /**
     * Delete charge from company mortgages.
     *
     * @param contextId the x-request-id.
     * @param chargeId  the charge identifier.
     */
    public void deleteCharge(String contextId,
            String chargeId) {
        try {
            Optional<ChargesDocument> chargesDocumentOptional =
                    chargesRepository.findById(chargeId);

            if (chargesDocumentOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        String.format("Company charge doesn't exist in company mortgages with %s header x-request-id %s",
                        chargeId, contextId));
            }
            Optional<String> companyNumberOptional = Optional.ofNullable(
                            chargesDocumentOptional.get().getCompanyNumber())
                    .filter(Predicate.not(String::isEmpty));

            String companyNumber = companyNumberOptional.orElseThrow(
                    () -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            String.format("Company number doesn't exist in document for the given charge id %s",
                                    chargeId)));

            ChargeApi chargeApi =
                    chargesDocumentOptional.map(ChargesDocument::getData)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    String.format("ChargeApi object doesn't exist for %s", chargeId)));

            ApiResponse<Void> apiResponse = chargesApiService.invokeChsKafkaApiWithDeleteEvent(
                    contextId,
                    chargeId,
                    companyNumber,
                    chargeApi);
            LOGGER.info("ChsKafka api DELETED invoked successfully", DataMapHolder.getLogMap());

            if (apiResponse == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        " error response received from ChsKafkaApi");
            }

            HttpStatus statusCode = HttpStatus.valueOf(apiResponse.getStatusCode());
            if (!statusCode.is2xxSuccessful()) {
                throw new ResponseStatusException(HttpStatus.valueOf(apiResponse.getStatusCode()),
                        " error response received from ChsKafkaApi");
            }

            chargesRepository.deleteById(chargeId);
            LOGGER.info("Company charge deleted successfully in MongoDB", DataMapHolder.getLogMap());

        } catch (DataAccessException dbException) {
            LOGGER.error("Error occurred during a DB call for delete", DataMapHolder.getLogMap());
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, dbException.getMessage());
        }
    }

}
