package uk.gov.companieshouse.charges.data.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.charges.ScottishAlterationsApi;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NotNullFieldObjectSerializerTest {

    @Mock
    private SerializerProvider serializerProvider;
    @Mock
    private JsonGenerator jsonGenerator;

    private NotNullFieldObjectSerializer apiSerializer;

    @BeforeEach
    void setUp() {
        apiSerializer = new NotNullFieldObjectSerializer();
    }

    @Test
    void testSerialiseScottishAlterationsApiObject() throws IOException {
        ScottishAlterationsApi alterationsApi = new ScottishAlterationsApi();
        alterationsApi.setDescription("test");
        apiSerializer.serialize(alterationsApi, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeRawValue("{\"description\":\"test\"}");
    }

    @Test
    void testEmptyScottishAlterationsApiObject() {
        ScottishAlterationsApi alterationsApi = new ScottishAlterationsApi();

        assertTrue(apiSerializer.isEmpty(serializerProvider, alterationsApi));
        assertTrue(apiSerializer.isEmpty(serializerProvider, null));
    }

    @Test
    void testNonEmptyScottishAlterationsApiObject() {
        ScottishAlterationsApi alterationsApi = new ScottishAlterationsApi();
        alterationsApi.setDescription("some description");
        alterationsApi.setHasAlterationsToProhibitions(true);

        assertFalse(apiSerializer.isEmpty(serializerProvider, alterationsApi));
    }

}