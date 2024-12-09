package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.lang3.ObjectUtils;

public class NotNullFieldObjectSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object value, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        if (isEmpty(serializerProvider, value)) {
            jsonGenerator.writeNull();
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            jsonGenerator.writeRawValue(objectMapper.writeValueAsString(value));
        }
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Object value) {
        return !isAnyFieldValueNotNull(value);
    }

    private boolean isAnyFieldValueNotNull(Object value) {
        if (value == null) {
            return false;
        }

        return ObjectUtils.anyNotNull(Arrays.stream(value.getClass().getDeclaredFields())
                .filter(field -> !field.isSynthetic())
                .map(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(value);
                    } catch (IllegalAccessException ex) {
                        throw new RuntimeException(ex);
                    }
                }).toArray(Object[]::new));
    }

}
