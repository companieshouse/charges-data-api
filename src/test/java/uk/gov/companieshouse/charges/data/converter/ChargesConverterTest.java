package uk.gov.companieshouse.charges.data.converter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.charges.ChargeApi.StatusEnum;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;

@SpringBootTest
class ChargesConverterTest {
    String chargesData;
    String chargesData1;

    @Autowired
    private ObjectMapper mongoCustomConversions;

    @Value("classpath:charges-test-DB-record.json")
    private Resource resource;

    @BeforeEach
    void setup() throws IOException {
        chargesData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(resource.getInputStream())));
    }

    @Test
    void shouldReadEnumValue() throws IOException {
        Document chargesBson = Document.parse(chargesData);
        ChargesDocument chargesDocument =
                mongoCustomConversions.convertValue(chargesBson, ChargesDocument.class);
       assertThat(chargesDocument).isNotNull();
       assertThat(chargesDocument.getData().getStatus()).isNotNull();
       assertThat(chargesDocument.getData().getStatus()).isEqualTo(StatusEnum.FULLY_SATISFIED);
    }

}