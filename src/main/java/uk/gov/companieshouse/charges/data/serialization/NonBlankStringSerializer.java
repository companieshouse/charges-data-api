package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import org.apache.commons.lang.StringUtils;

public class NonBlankStringSerializer extends JsonSerializer<String> {

    @Override
    public void serialize(String value, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        if (isEmpty(serializerProvider, value)) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeString(value);
        }
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, String value) {
        return StringUtils.isBlank(value);
    }
}
