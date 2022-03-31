package uk.gov.companieshouse.charges.data.tranform;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.Updated;
import uk.gov.companieshouse.logging.Logger;

@Component
public class ChargesTransformer {

    static String type = "mortgage_delta";

    private Logger logger;
    private final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public ChargesTransformer(final Logger logger) {
        this.logger = logger;
    }

    /**
     * Transform the incoming model to wrapper to be used to store in DB collection.
     *
     * @param companyNumber company number.
     * @param requestBody   incoming request.
     * @return response to be returned.
     */
    public ChargesDocument transform(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        logger.debug(String.format(
                "Started: transforming incoming request body to model used by database", chargeId,
                companyNumber));
        OffsetDateTime at = requestBody.getInternalData().getDeltaAt();

        String by = requestBody.getInternalData().getUpdatedBy();
        var externalData = requestBody.getExternalData();
        externalData.setEtag(GenerateEtagUtil.generateEtag());
        final Updated updated =
                new Updated().setAt(at.toLocalDate()).setType(type).setBy(by);
        var chargesDocument = new ChargesDocument().setId(chargeId)
                .setCompanyNumber(companyNumber).setData(externalData)
                .setUpdated(updated);
        logger.debug(String.format("Finished: Transformation complete successfully", chargeId,
                companyNumber));
        return chargesDocument;
    }

}
