package uk.gov.companieshouse.charges.data.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.charges.ChargeApi;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ChargeApiWriteConverterTest {

    @Mock
    private ObjectMapper objectMapper;

    private ChargeApiWriteConverter writeConverter;

    @BeforeEach
    void setUp() {
        writeConverter = new ChargeApiWriteConverter(objectMapper);
    }

    @Test
    void testReadException() throws Exception {
        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new IllegalArgumentException("test"));

        assertThrows(RuntimeException.class, () -> writeConverter.convert(new ChargeApi()));
    }
}