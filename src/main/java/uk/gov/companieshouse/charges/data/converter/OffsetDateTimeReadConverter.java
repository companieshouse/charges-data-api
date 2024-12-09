package uk.gov.companieshouse.charges.data.converter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import org.springframework.core.convert.converter.Converter;

public class OffsetDateTimeReadConverter implements Converter<Date, OffsetDateTime> {

    @Override
    public OffsetDateTime convert(final Date date) {
        return date.toInstant()
                .atOffset(ZoneOffset.UTC);
    }

}
