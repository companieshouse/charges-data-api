package uk.gov.companieshouse.charges.data.serialization;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class OffsetDateTimeDeSerializerTest {

    private static final String DATE_STRING = "2015-06-26T08:31:35.058Z";

    @Mock
    private DeserializationContext deserializationContext;
    @Mock
    private JsonParser jsonParser;
    @Mock
    private JsonNode jsonNode;

    private OffsetDateTimeDeSerializer deserializer;

    @BeforeEach
    void setUp() throws IOException {
        deserializer = new OffsetDateTimeDeSerializer();
        when(jsonParser.readValueAsTree()).thenReturn(jsonNode);
    }

    @Test
    void testDeserializeTextValue() {
        when(jsonNode.get(any())).thenReturn(jsonNode);
        when(jsonNode.textValue()).thenReturn(DATE_STRING);
        OffsetDateTime offsetDateTime = deserializer.deserialize(jsonParser, deserializationContext);

        assertEquals(OffsetDateTime.parse(DATE_STRING, DateTimeFormatter.ISO_OFFSET_DATE_TIME), offsetDateTime);
    }

    @Test
    void testDeserializeWithException() {
        when(jsonNode.get(any())).thenReturn(jsonNode);
        when(jsonNode.textValue()).thenThrow(new IllegalStateException("exception"));

        assertThrows(RuntimeException.class, () -> deserializer.deserialize(jsonParser, deserializationContext));
    }
}