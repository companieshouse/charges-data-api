package uk.gov.companieshouse.charges.data.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeDeSerializerTest {

    private static final String DATE_STRING = "2015-06-26T08:31:35.058Z";

    @Mock
    private DeserializationContext deserializationContext;
    @Mock
    private JsonParser jsonParser;
    @Mock
    private JsonNode jsonNode;

    private LocalDateTimeDeSerializer deserializer;

    @BeforeEach
    void setUp() throws IOException {
        deserializer = new LocalDateTimeDeSerializer();
        when(jsonParser.readValueAsTree()).thenReturn(jsonNode);
    }

    @Test
    void testDeserializeTextValue() {
        when(jsonNode.get(any())).thenReturn(jsonNode);
        when(jsonNode.textValue()).thenReturn(DATE_STRING);
        LocalDateTime localDateTime = deserializer.deserialize(jsonParser, deserializationContext);

        assertEquals(LocalDateTime.parse(DATE_STRING, DateTimeFormatter.ISO_DATE_TIME), localDateTime);
    }

    @Test
    void testDeserializeWithException() {
        when(jsonNode.get(any())).thenReturn(jsonNode);
        when(jsonNode.textValue()).thenThrow(new IllegalStateException("exception"));

        assertThrows(RuntimeException.class, () -> deserializer.deserialize(jsonParser, deserializationContext));
    }
}