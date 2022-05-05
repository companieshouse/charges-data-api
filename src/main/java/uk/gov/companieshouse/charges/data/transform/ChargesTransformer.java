package uk.gov.companieshouse.charges.data.transform;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.Updated;
import uk.gov.companieshouse.logging.Logger;

@Component
public class ChargesTransformer {

    static final String type = "mortgage_delta";

    private final Logger logger;

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
        logger.debug(String.format("Started: transforming incoming request body "
                + "to model for chargeId: %s and companyNumber: %s", chargeId, companyNumber));

        OffsetDateTime at = requestBody.getInternalData().getDeltaAt();
        String by = requestBody.getInternalData().getUpdatedBy();
        OffsetDateTime deltaAt = requestBody.getInternalData().getDeltaAt();
        var externalData = requestBody.getExternalData();
        externalData.setEtag(GenerateEtagUtil.generateEtag());
        final Updated updated =
                new Updated().setAt(at.toLocalDateTime()).setType(type).setBy(by);
        var chargesDocument = new ChargesDocument().setId(chargeId)
                .setCompanyNumber(companyNumber)
                .setData(externalData)
                .setDeltaAt(deltaAt.toLocalDateTime())
                .setUpdated(updated);
        logger.debug(String.format("Finished: Transformation complete successfully for chargeId: "
                       + "%s and companyNumber: %s", chargeId, companyNumber));
        return chargesDocument;
    }

}
