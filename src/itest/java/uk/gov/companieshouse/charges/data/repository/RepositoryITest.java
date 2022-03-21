package uk.gov.companieshouse.charges.data.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.company.Data;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.Updated;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class RepositoryITest {

  static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
      DockerImageName.parse("mongo:4.0.10"));

  @Autowired
  ChargesRepository chargesRepository;

  @Value("classpath:company-api-request-data.json")
  Resource resourceFile;

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    mongoDBContainer.start();

  }

  @BeforeAll
  static void setup(){
    mongoDBContainer.start();
  }

  @Test
  void should_return_mongodb_as_running() {
    Assertions.assertTrue(mongoDBContainer.isRunning());
  }

  @Test
  void should_save_and_retrieve_charges_data() throws IOException {

    ChargesDocument chargesDocument = createChargesDocument("CH253434", "jbvgV-Zu-i8bRkypE0AEJx1N_Sk");

    chargesRepository.save(chargesDocument);

    List<ChargesDocument> documents = chargesRepository.findAll();
    assertThat(documents).hasSize(1);
    assertThat(documents.get(0).getData().getId()).isEqualTo(chargesDocument.getData().getId());
    assertThat(documents.get(0).getCompanyNumber()).isEqualTo(chargesDocument.getCompanyNumber());
    assertThat(documents.get(0).getUpdated().getAt()).isEqualTo(chargesDocument.getUpdated().getAt());
  }

  private ChargesDocument createChargesDocument(String companyNumber, String chargeId) throws IOException {
    String incomingData =
            FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                    resourceFile.getInputStream())));
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    InternalChargeApi chargesDocument = mapper.readValue(incomingData, InternalChargeApi.class);
    ChargesDocument chargesDocument1 =
            transform("02327864", "2m1l9ofMOYNsHxDiSC_FkHw9lOw", chargesDocument);

    return chargesDocument1;
  }

  @AfterAll
  static void tear(){
    mongoDBContainer.stop();
  }

  public ChargesDocument transform(String companyNumber, String chargeId,
          InternalChargeApi requestBody) {

    OffsetDateTime at = requestBody.getInternalData().getDeltaAt();

    String by = requestBody.getInternalData().getUpdatedBy();
    final Updated updated = new Updated().setAt(at.toLocalDate()).setType("mortgage_delta").setBy(by);
    var chargesDocument = new ChargesDocument().setId(chargeId)
            .setCompanyNumber(companyNumber).setData(requestBody.getExternalData())
            .setUpdated(updated);

    return chargesDocument;
  }
}
