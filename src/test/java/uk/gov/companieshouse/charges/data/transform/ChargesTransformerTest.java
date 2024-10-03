package uk.gov.companieshouse.charges.data.transform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.Before;
import org.junit.Test;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.charges.InternalData;

public class ChargesTransformerTest {

    private ChargesTransformer chargesTransformer;

    @Before
    public void init() {
        this.chargesTransformer = new ChargesTransformer();
    }

    @Test
    public void shouldTransformPayloadCorrectly(){
        String companyNumber = "companyNumber";
        String chargeId = "MzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng==";
        InternalChargeApi requestBody = new InternalChargeApi();
        var internalData = new InternalData();
        internalData.setDeltaAt(OffsetDateTime.now(ZoneOffset.UTC));
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
        assertNotNull(result.getDeltaAt());
        assertEquals(result.getDeltaAt(), internalData.getDeltaAt());
        assertNotNull(result.getUpdated());
        assertEquals(result.getUpdated().getBy(), internalData.getUpdatedBy());
    }

}
