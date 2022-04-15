package uk.gov.companieshouse.charges.data.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.metrics.MetricsApi;
import uk.gov.companieshouse.api.metrics.MortgageApi;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.api.CompanyMetricsApiService;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;
import uk.gov.companieshouse.charges.data.util.DateFormatter;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesService {

    private final Logger logger;
    private final ChargesApiService chargesApiService;
    private ChargesTransformer chargesTransformer;
    private ChargesRepository chargesRepository;
    private CompanyMetricsApiService companyMetricsApiService;


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
        boolean latestRecord = isLatestRecord(companyNumber, chargeId, requestBody);
        if (latestRecord) {

            ChargesDocument charges =
                    this.chargesTransformer.transform(companyNumber, chargeId, requestBody);
            logger.debug(String.format("Started : Saving charges in DB "));
            this.chargesRepository.save(charges);
            logger.debug(
                    String.format("Finished : upsertCharges for chargeId %s company number %s ",
                            chargeId,
                            companyNumber));
            chargesApiService.invokeChsKafkaApi(contextId, companyNumber);
            logger.info(
                    String.format("DSND-542: ChsKafka api invoked successfully for company number"
                            + " %s", companyNumber));
        } else {
            logger.debug(
                    "Finished : upsertCharges, charge not saved "
                            + "as record provided is not a latest record.");
        }
    }

    private boolean isLatestRecord(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        OffsetDateTime localDate = requestBody.getInternalData().getDeltaAt();
        String format = DateFormatter.format(localDate.toLocalDate());
        Optional<ChargesDocument> chargesDelta =
                this.chargesRepository.findCharge(companyNumber, chargeId,
                        format);
        return chargesDelta.isEmpty();
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
                chargesDocuments.map(chargeDocument -> chargeDocument.getData());
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

        List<ChargesDocument> charges =
                this.chargesRepository.findCharges(companyNumber, pageable).getContent();
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
