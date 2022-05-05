package uk.gov.companieshouse.charges.data.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.api.metrics.MortgageApi;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.api.CompanyMetricsApiService;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesService {

    private final Logger logger;
    private final ChargesApiService chargesApiService;
    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
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
            final ChargesTransformer chargesTransformer, ChargesApiService chargesApiService,
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
    @Transactional
    public void upsertCharges(String contextId, String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        logger.debug(String.format("Started :upsertCharges for chargeId %s company number %s ",
                chargeId,
                companyNumber));

        Optional<ChargesDocument> chargesDocumentFromDbOptional =
                chargesRepository.findById(chargeId);

        chargesDocumentFromDbOptional.map(x -> {
            OffsetDateTime dateFromBodyRequest = requestBody
                    .getInternalData()
                    .getDeltaAt();

            ChargesDocument chargesDocumentFromDb = chargesDocumentFromDbOptional
                    .get();

            LocalDateTime deltaAtFromDbLocalDateTime = chargesDocumentFromDb
                    .getDeltaAt();

            OffsetDateTime deltaAtFromDb =
                    OffsetDateTime.of(LocalDateTime.from(deltaAtFromDbLocalDateTime),
                            ZoneOffset.UTC);

            if (dateFromBodyRequest.isAfter(deltaAtFromDb)) {
                ChargesDocument charges =
                        this.chargesTransformer.transform(companyNumber, chargeId, requestBody);
                logger.debug(String.format("Started : Saving charges in DB "));
                this.chargesRepository.save(charges);
                logger.debug(
                        String.format("Finished : upsertCharges for chargeId %s company number %s ",
                                chargeId,
                                companyNumber));
            } else {
                logger.debug(
                        "Finished : upsertCharges, charge not saved "
                                + "as record provided is older than the one already stored.");
            }
            return null;
        }).orElseGet(() -> {
            if (chargesDocumentFromDbOptional.isPresent()) {
                ChargesDocument charges =
                        this.chargesTransformer.transform(companyNumber, chargeId, requestBody);
                logger.debug("Started : Saving charges in DB ");
                this.chargesRepository.save(charges);
                logger.debug(
                        String.format("Finished : upsertCharges for chargeId %s company number %s ",
                                chargeId,
                                companyNumber));
                ApiResponse<Void> res = chargesApiService.invokeChsKafkaApi(contextId,
                        companyNumber,
                        chargeId);
                // Code is not 2xx
                if (res.getStatusCode() < 200 || res.getStatusCode() > 299) {
                    throw new ResponseStatusException(HttpStatus.resolve(res.getStatusCode()),
                            "invokeChsKafkaApi");
                }
            }
            return null;
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
        logger.debug(String.format("Started : get Charge Details for Company Number %s "
                        + " Charge Id %s ",
                companyNumber,
                chargeId
        ));
        Optional<ChargesDocument> chargesDocuments =
                this.chargesRepository.findChargeDetails(companyNumber, chargeId);
        if (chargesDocuments.isEmpty()) {
            logger.trace(
                    String.format(
                            "Finished: Company charges not found for company %s with charge id %s",
                            companyNumber, chargeId));
            return Optional.empty();
        }
        Optional<ChargeApi> chargeDetails =
                chargesDocuments.map(ChargesDocument::getData);
        logger.debug(String.format("Finished : Charges details found for Company Number %s "
                        + "with Charge id %s",
                companyNumber,
                chargeId
        ));
        return chargeDetails;
    }

    /**
     * Find charges for company number.
     *
     * @param companyNumber company Number.
     * @return charges.
     */
    public Optional<ChargesApi> findCharges(final String companyNumber, final Pageable pageable) {
        logger.debug(String.format("Started : findCharges for Company Number %s ",
                companyNumber
        ));

        Page<ChargesDocument> page = chargesRepository.findCharges(companyNumber, pageable);
        List<ChargesDocument> charges = page == null ? Collections.emptyList() : page.getContent();
        if (charges.isEmpty()) {
            logger.error(
                    String.format(
                            "Finished: findCharges No charges found for company %s ",
                            companyNumber));
            return Optional.empty();
        }

        Optional<MetricsApi> companyMetrics =
                companyMetricsApiService.getCompanyMetrics(companyNumber);

        if (companyMetrics.isEmpty()) {
            logger.error(
                    String.format(
                            "Finished: findCharges No company metrics data found for company %s ",
                            companyNumber));
            return Optional.empty();
        }
        var result = companyMetrics.map(metrics -> {
            var chargesApi = new ChargesApi();
            charges.stream().forEach(charge -> chargesApi.addItemsItem(charge.getData()));
            MortgageApi mortgage = metrics.getMortgage();
            int totalCount = chargesApi.getItems().size();
            int satisfiedCount = mortgage.getSatisfiedCount();
            int partSatisfiedCount = mortgage.getPartSatisfiedCount();
            int unfilteredCount = mortgage.getTotalCount();
            chargesApi.setTotalCount(totalCount);
            chargesApi.setSatisfiedCount(satisfiedCount);
            chargesApi.setEtag(metrics.getEtag());
            chargesApi.setPartSatisfiedCount(partSatisfiedCount);
            chargesApi.setUnfilteredCount(unfilteredCount);
            return chargesApi;
        });
        logger.debug(String.format("Finished : findCharges charges found for Company Number %s ",
                companyNumber
        ));
        return result;
    }

}
