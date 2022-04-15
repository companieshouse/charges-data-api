package uk.gov.companieshouse.charges.data.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import uk.gov.companieshouse.api.charges.ChargeApi;

@ReadingConverter
public class ChargeApiReadConverter implements Converter<Document, ChargeApi> {

    private final ObjectMapper objectMapper;

    public ChargeApiReadConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Read convertor.
     * @param source source Document.
     * @return charge object.
     */
    @Override
    public ChargeApi convert(Document source) {
        try {
            return objectMapper.readValue(source.toJson(), ChargeApi.class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
