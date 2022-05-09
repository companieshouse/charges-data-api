package uk.gov.companieshouse.charges.data.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargeApi.StatusEnum;
import uk.gov.companieshouse.charges.data.config.ChargesApplicationConfig;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@Import(ChargesApplicationConfig.class)
class ChargeApiReadConverterTest {
    String chargesData;

    @Autowired
    private ObjectMapper mongoDbObjectMapper;

    private ChargeApiReadConverter chargeApiReadConverter;

    @Value("classpath:charges-test-DB-record-2.json")
    private Resource resource;

    @BeforeEach
    void setup() throws IOException {
        chargesData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(resource.getInputStream())));
        chargeApiReadConverter = new ChargeApiReadConverter(mongoDbObjectMapper);
    }

    /*@Test
    void shouldReadBsonDocument() throws IOException {
        Document chargesBson = Document.parse(chargesData);
        ChargeApi chargeApi =
                chargeApiReadConverter.convert(chargesBson);
       assertThat(chargeApi).isNotNull();
       assertThat(chargeApi.getStatus()).isNotNull();
       assertThat(chargeApi.getStatus()).isEqualTo(StatusEnum.OUTSTANDING);
    }*/

    /*@Test
    void shouldReadBsonDocumentWithoutISOSDate() throws IOException {
        Document chargesBson = Document.parse(chargesData);
        ChargeApi chargeApi =
                chargeApiReadConverter.convert(chargesBson);
        assertThat(chargeApi).isNotNull();
        assertThat(chargeApi.getStatus()).isNotNull();
        assertThat(chargeApi.getStatus()).isEqualTo(StatusEnum.OUTSTANDING);
    }

    @Test
    public void makeBsonObject() {
        System.out.println("BSON = " + BasicDBObject.parse(chargesData));
    }*/
}