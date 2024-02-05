package uk.gov.companieshouse.charges.data.service;

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
import uk.gov.companieshouse.charges.data.model.ChargesAggregate;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.RequestCriteria;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesService {

    private final Logger logger;
    private final ChargesApiService chargesApiService;
    private final ChargesTransformer chargesTransformer;
    private final ChargesRepository chargesRepository;
    private final CompanyMetricsApiService companyMetricsApiService;


    /**
     * ChargesService constructor.
     *
     * @param logger            Logger.
     * @param chargesRepository chargesRepository.
     */
    public ChargesService(final Logger logger, final ChargesRepository chargesRepository,
                          final ChargesTransformer chargesTransformer,
                          ChargesApiService chargesApiService,
                          CompanyMetricsApiService companyMetricsApiService) {
        this.logger = logger;
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

        chargesDocumentOptional.ifPresentOrElse(chargesDocument -> {
            OffsetDateTime dateFromBodyRequest = requestBody.getInternalData().getDeltaAt();
            OffsetDateTime deltaAtFromDb = chargesDocument.getDeltaAt();

            if (dateFromBodyRequest == null
                    || deltaAtFromDb == null || dateFromBodyRequest.isAfter(deltaAtFromDb)) {
                ChargesDocument charges =
                        this.chargesTransformer.transform(companyNumber, chargeId, requestBody);

                saveAndInvokeChsKafkaApi(contextId, companyNumber, chargeId, charges,
                        chargesDocument);
            } else {
                logger.error("Charge not saved "
                        + "as record provided is older than the one already stored"
                );
            }
        },
                () -> {
                    ChargesDocument charges =
                            this.chargesTransformer.transform(companyNumber, chargeId, requestBody);
                    saveAndInvokeChsKafkaApi(contextId, companyNumber, chargeId, charges, null);
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
            logger.trace(
                    String.format(
                            "Company charges not found for company %s with charge id %s",
                            companyNumber, chargeId));
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
        List<ChargeApi.StatusEnum> statusFilter = new ArrayList<>();
        if ("outstanding".equals(requestCriteria.getFilter())) {
            statusFilter.add(ChargeApi.StatusEnum.SATISFIED);
            statusFilter.add(ChargeApi.StatusEnum.FULLY_SATISFIED);
        }
        ChargesAggregate chargesAggregate =
                chargesRepository.findCharges(companyNumber, statusFilter,
                Optional.ofNullable(requestCriteria.getStartIndex()).orElse(0),
                Math.min(Optional.ofNullable(requestCriteria.getItemsPerPage()).orElse(25), 100));

        Optional<MetricsApi> companyMetrics =
                companyMetricsApiService.getCompanyMetrics(companyNumber);

        if (companyMetrics.isEmpty()) {
            logger.error(String.format("No company metrics data found for company %s ",
                    companyNumber));
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
                                          String chargeId, ChargesDocument charges,
                                          ChargesDocument existingCharges) {

        chargesRepository.save(charges);
        logger.debug(
                String.format("Invoking chs-kafka-api for chargeId %s company number %s ",
                        chargeId,
                        companyNumber));
        ApiResponse<Void> res = null;
        try {
            res = chargesApiService.invokeChsKafkaApi(contextId,
                    companyNumber,
                    chargeId);
        } catch (ResponseStatusException ex) {
            Optional.ofNullable(existingCharges)
                    .ifPresentOrElse(chargesRepository::save,
                            () -> chargesRepository.deleteById(chargeId));
            throw ex;
        }
        HttpStatus httpStatus = res != null ? HttpStatus.resolve(res.getStatusCode()) : null;
        if (httpStatus == null || !httpStatus.is2xxSuccessful()) {
            throw new ResponseStatusException(httpStatus != null
                    ? httpStatus : HttpStatus.INTERNAL_SERVER_ERROR, "invokeChsKafkaApi");
        }
        logger.info(String.format("ChsKafka api CHANGED invoked "
                        + "successfully for contextId %s and company number %s",
                contextId,
                companyNumber));

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
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, String.format(
                        "Company charge doesn't exist in company mortgages"
                                + " with %s header x-request-id %s",
                        chargeId, contextId));
            }
            Optional<String> companyNumberOptional = Optional.ofNullable(
                            chargesDocumentOptional.get().getCompanyNumber())
                    .filter(Predicate.not(String::isEmpty));

            String companyNumber = companyNumberOptional.orElseThrow(
                    () -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Company number doesn't exist in document for the given "
                                    + "charge id" + chargeId));

            ChargeApi chargeApi =
                    chargesDocumentOptional.map(ChargesDocument::getData)
                            .orElseThrow(() -> new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "ChargeApi object doesn't exist for" + chargeId));

            ApiResponse<Void> apiResponse = chargesApiService.invokeChsKafkaApiWithDeleteEvent(
                    contextId,
                    chargeId,
                    companyNumber,
                    chargeApi);
            logger.info(String.format("ChsKafka api DELETED "
                            + "invoked successfully for contextId %s and company number %s",
                    contextId,
                    companyNumber));


            if (apiResponse == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        " error response received from ChsKafkaApi");
            }

            HttpStatus statusCode = HttpStatus.valueOf(apiResponse.getStatusCode());
            if (!statusCode.is2xxSuccessful()) {
                throw new ResponseStatusException(HttpStatus
                        .valueOf(apiResponse.getStatusCode()),
                        " error response received from ChsKafkaApi");
            }

            chargesRepository.deleteById(chargeId);
            logger.info(String.format(
                    "Company charge is deleted in "
                            + "MongoDB with contextId %s and company number %s",
                    contextId,
                    companyNumber));

        } catch (DataAccessException dbException) {
            logger.error(String.format(
                    "Error occurred during a DB call for deleting "
                            + "charge id %s and x-request-id %s", chargeId, contextId));
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, dbException.getMessage());
        }
    }

}
