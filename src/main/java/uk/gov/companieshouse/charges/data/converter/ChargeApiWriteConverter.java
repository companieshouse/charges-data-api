package uk.gov.companieshouse.charges.data.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import uk.gov.companieshouse.api.charges.ChargeApi;

@WritingConverter
public class ChargeApiWriteConverter implements Converter<ChargeApi, BasicDBObject> {

    private final ObjectMapper objectMapper;

    public ChargeApiWriteConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public BasicDBObject convert(ChargeApi source) {
        try {
            return BasicDBObject.parse(objectMapper.writeValueAsString(source));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
