package uk.gov.companieshouse.charges.data.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.OffsetDateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.charges.InternalData;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

public class ChargesTransformerTest {

    private Logger logger;
    private ChargesTransformer chargesTransformer;

    @Before
    public void init() {
        this.logger = Mockito.mock(Logger.class);
        this.chargesTransformer = new ChargesTransformer(this.logger);
    }

    @Test
    public void shouldTransformPayloadCorrectly(){
        String companyNumber = "companyNumber";
        String chargeId = "L5bvSq3ligF_V84zh-ExMxCeU";
        InternalChargeApi requestBody = new InternalChargeApi();
        var internalData = new InternalData();
        internalData.setDeltaAt(OffsetDateTime.now());
        var externalData = new ChargeApi();
        requestBody.setInternalData(internalData);
        requestBody.setExternalData(externalData);
        var result = this.chargesTransformer.transform(companyNumber, chargeId, requestBody);
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
