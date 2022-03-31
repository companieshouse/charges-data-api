package uk.gov.companieshouse.charges.data.service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.api.ChargesApiService;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@Service
public class ChargesService {

    private final Logger logger;
    private final ChargesApiService chargesApiService;
    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private ChargesTransformer chargesTransformer;
    private ChargesRepository chargesRepository;


    /**
     * ChargesService constructor.
     *
     * @param logger            Logger.
     * @param chargesRepository chargesRepository.
     */
    public ChargesService(final Logger logger, final ChargesRepository chargesRepository,
            final ChargesTransformer chargesTransformer, ChargesApiService chargesApiService) {
        this.logger = logger;
        this.chargesRepository = chargesRepository;
        this.chargesTransformer = chargesTransformer;
        this.chargesApiService = chargesApiService;
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
        logger.debug(String.format("Started : Save or Update charge %s with company number %s ",
                chargeId,
                companyNumber));
        boolean latestRecord = isLatestRecord(companyNumber, chargeId, requestBody);
        if (latestRecord) {

            ChargesDocument charges =
                    this.chargesTransformer.transform(companyNumber, chargeId, requestBody);
            logger.debug(String.format("Started : Saving charges in DB "));
            this.chargesRepository.save(charges);
            logger.debug(String.format("Finished : Save or Update charge %s with company number %s",
                    chargeId,
                    companyNumber));

            chargesApiService.invokeChsKafkaApi(contextId, companyNumber);
        } else {
            logger.debug("Record is not a latest.");
        }

        logger.info(String.format("DSND-542: ChsKafka api invoked successfully for company number"
                + " %s", companyNumber));
    }

    private boolean isLatestRecord(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        OffsetDateTime localDate = requestBody.getInternalData().getDeltaAt();
        String format = localDate.format(dateTimeFormatter);
        List chargesDelta =
                this.chargesRepository.findChargesDelta(companyNumber, chargeId,
                        format);
        return chargesDelta != null && chargesDelta.isEmpty();
    }

}
