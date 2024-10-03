package uk.gov.companieshouse.charges.data.transform;

import static uk.gov.companieshouse.charges.data.ChargesDataApiApplication.NAMESPACE;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.GenerateEtagUtil;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.ChargesDocument.Updated;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class ChargesTransformer {

    static final String type = "mortgage_delta";

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    /**
     * Transform the incoming model to wrapper to be used to store in DB collection.
     *
     * @param companyNumber company number.
     * @param requestBody   incoming request.
     * @return response to be returned.
     */
    public ChargesDocument transform(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {
        LOGGER.debug("Transforming incoming request body", DataMapHolder.getLogMap());

        String by = requestBody.getInternalData().getUpdatedBy();
        OffsetDateTime deltaAt = requestBody.getInternalData().getDeltaAt();
        var externalData = requestBody.getExternalData();
        externalData.setEtag(GenerateEtagUtil.generateEtag());
        final Updated updated =
                new Updated().setAt(LocalDateTime.now()).setType(type).setBy(by);
        var chargesDocument = new ChargesDocument().setId(chargeId)
                .setCompanyNumber(companyNumber)
                .setData(externalData)
                .setDeltaAt(deltaAt)
                .setUpdated(updated);
        LOGGER.debug("Transformation complete successfully", DataMapHolder.getLogMap());
        return chargesDocument;
    }

}
