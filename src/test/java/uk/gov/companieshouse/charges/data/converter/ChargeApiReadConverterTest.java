package uk.gov.companieshouse.charges.data.converter;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.charges.ChargeApi;

@ExtendWith(MockitoExtension.class)
class ChargeApiReadConverterTest {

    @Mock
    private ObjectMapper objectMapper;

    private ChargeApiReadConverter readConverter;

    @BeforeEach
    void setUp() {
        readConverter = new ChargeApiReadConverter(objectMapper);
    }

    @Test
    void testReadException() throws Exception {
        when(objectMapper.readValue("", ChargeApi.class))
                .thenThrow(new IllegalArgumentException("test"));

        assertThrows(RuntimeException.class, () -> readConverter.convert(new Document()));
    }
}