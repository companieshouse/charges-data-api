package uk.gov.companieshouse.charges.data.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.charges.InternalData;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;

public class ChargesTransformerTest {

    @Test
    public void shouldTransformPayloadCorrectly(){
        String companyNumber = "companyNumber";
        String chargeId = "L5bvSq3ligF_V84zh-ExMxCeU";
        InternalChargeApi requestBody = new InternalChargeApi();
        var internalData = new InternalData();
        var externalData = new ChargeApi();
        requestBody.setInternalData(internalData);
        requestBody.setExternalData(externalData);
        var result = ChargesTransformer.transform(companyNumber, chargeId, requestBody);
        assertNotNull(result);
        assertNotNull(result.getId());
        assertEquals(result.getId(), chargeId);
        assertNotNull(result.getCompanyNumber());
        assertEquals(result.getCompanyNumber(), companyNumber);
        assertNotNull(result.getData());
        assertEquals(result.getData(), externalData);
        assertNotNull(result.getUpdated());
        assertEquals(result.getUpdated().getBy(), internalData.getUpdatedBy());
    }

}
