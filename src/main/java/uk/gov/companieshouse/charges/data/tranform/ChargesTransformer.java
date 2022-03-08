package uk.gov.companieshouse.charges.data.tranform;

import java.time.OffsetDateTime;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.Updated;

public class ChargesTransformer {

    /**
     * Transform the incoming model to wrapper to be used to store in DB collection.
     * @param companyNumber company number.
     * @param requestBody incoming request.
     * @return response to be returned.
     */
    public static ChargesDocument transform(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        OffsetDateTime at = requestBody.getInternalData().getDeltaAt();
        String type = "mortgage_delta"; //TODO check whether this correct?
        String by = requestBody.getInternalData().getUpdatedBy();
        final Updated updated = new Updated().setAt(at).setType(type).setBy(by);
        return new ChargesDocument().setId(chargeId)
                .setCompanyNumber(companyNumber).setData(requestBody.getExternalData())
                .setUpdated(updated);
    }

}
