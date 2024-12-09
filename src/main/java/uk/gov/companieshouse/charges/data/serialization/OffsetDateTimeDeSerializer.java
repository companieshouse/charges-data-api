package uk.gov.companieshouse.charges.data.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import uk.gov.companieshouse.charges.data.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;


public class OffsetDateTimeDeSerializer extends JsonDeserializer<OffsetDateTime> {

    public static final String APPLICATION_NAME_SPACE = "company-profile-api";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAME_SPACE);

    @Override
    public OffsetDateTime deserialize(JsonParser jsonParser, DeserializationContext
            deserializationContext) {
        try {
            JsonNode jsonNode = jsonParser.readValueAsTree();
            return OffsetDateTime.parse(jsonNode.get("$date")
                    .textValue(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        } catch (Exception exception) {
            LOGGER.error("OffsetDateTime Deserialization failed.", exception, DataMapHolder.getLogMap());
            throw new RuntimeException("Failed while deserializing "
                    + "date value for json node.", exception);
        }
    }
}
