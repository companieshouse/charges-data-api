package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;

import uk.gov.companieshouse.charges.data.util.DateUtils;

public class LocalDateSerializer extends JsonSerializer<LocalDate> {

    @Override
    public void serialize(LocalDate localDate, JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        if (localDate == null) {
            jsonGenerator.writeNull();
        } else {
            String format = DateUtils.format(localDate);
            jsonGenerator.writeRawValue("ISODate(\"" + format + "\")");
        }
    }
}
