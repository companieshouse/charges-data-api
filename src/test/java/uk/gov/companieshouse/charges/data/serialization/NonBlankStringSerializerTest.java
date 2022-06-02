package uk.gov.companieshouse.charges.data.serialization;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NonBlankStringSerializerTest {

    @Mock
    private SerializerProvider serializerProvider;
    @Mock
    private JsonGenerator jsonGenerator;

    private NonBlankStringSerializer stringSerializer;

    @BeforeEach
    void setUp() {
        stringSerializer = new NonBlankStringSerializer();
    }

    @Test
    void testSerialiseNonBlankValue() throws IOException {
        stringSerializer.serialize("test", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeString("test");
    }

    @Test
    void testSerialiseBlankValue() throws IOException {
        stringSerializer.serialize("   ", jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeNull();
    }

    @Test
    void testSerialiseNullValue() throws IOException {
        stringSerializer.serialize(null, jsonGenerator, serializerProvider);

        verify(jsonGenerator).writeNull();
    }
}