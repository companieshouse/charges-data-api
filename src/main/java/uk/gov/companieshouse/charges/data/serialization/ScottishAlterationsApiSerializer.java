package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.lang.reflect.Field;
import uk.gov.companieshouse.api.charges.ScottishAlterationsApi;

public class ScottishAlterationsApiSerializer extends JsonSerializer<ScottishAlterationsApi> {

    @Override
    public void serialize(ScottishAlterationsApi value, JsonGenerator jsonGenerator,
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
    public boolean isEmpty(SerializerProvider provider, ScottishAlterationsApi value) {
        return value == null || allFieldsNull(value);
    }

    private boolean allFieldsNull(ScottishAlterationsApi value) {
        try {
            for (Field field : ScottishAlterationsApi.class.getDeclaredFields()) {
                field.setAccessible(true);
                if (!field.isSynthetic() && field.get(value) != null) {
                    return false;
                }
            }
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

        return true;
    }

}
