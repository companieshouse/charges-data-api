package uk.gov.companieshouse.charges.data.serialization;

import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.charges.data.util.DateUtils;

@ExtendWith(MockitoExtension.class)
class LocalDateSerializerTest {

    private static final String DATE_STRING = "2015-06-26T08:31:35.058Z";

    @Mock
    private SerializerProvider serializerProvider;
    @Mock
    private JsonGenerator jsonGenerator;

    private LocalDateSerializer serializer;

    @BeforeEach
    void setUp() {
        serializer = new LocalDateSerializer();
    }

    @Test
    void testSerialiseValidValue() throws IOException {
        serializer.serialize(DateUtils.parse(DATE_STRING), jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeRawValue("ISODate(\"2015-06-26T00:00:00Z\")");
    }

    @Test
    void testSerialiseNullValue() throws IOException {
        serializer.serialize(null, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeNull();
    }

}