package uk.gov.companieshouse.charges.data.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileCopyUtils;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.Updated;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class, includeFilters = @ComponentScan.Filter(Component.class))
@TestInstance(Lifecycle.PER_CLASS)
public class RepositoryITest {

  static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
      DockerImageName.parse("mongo:4.0.10"));

  @Autowired
  ChargesRepository chargesRepository;

  @Value("file:src/itest/resources/payload/input/charge-api-request-data.json")
  Resource resourceFile;

  @DynamicPropertySource
  static void setProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl );
    mongoDBContainer.start();
  }

  @BeforeAll
  public void setup(){
    this.chargesRepository.deleteAll();
  }

  @Test
  void should_return_mongodb_as_running() {
    Assertions.assertTrue(mongoDBContainer.isRunning());
  }

  @Test
  void should_save_and_retrieve_charges_data() throws IOException {

    ChargesDocument chargesDocument = createChargesDocument(UUID.randomUUID().toString(), UUID.randomUUID().toString());

    chargesRepository.save(chargesDocument);

    List<ChargesDocument> documents = chargesRepository.findAll();
    assertThat(documents).hasSize(1);
    assertThat(documents.get(0).getData().getId()).isEqualTo(chargesDocument.getData().getId());
    assertThat(documents.get(0).getCompanyNumber()).isEqualTo(chargesDocument.getCompanyNumber());
    assertThat(documents.get(0).getDeltaAt()).isEqualTo(chargesDocument.getDeltaAt());
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
    ChargesDocument transformedChargesDocument =
            transform(companyNumber, chargeId, chargesDocument);

    return transformedChargesDocument;
  }

  @AfterAll
  public void tear() {
    this.chargesRepository.deleteAll();;
    mongoDBContainer.stop();
  }

  public ChargesDocument transform(String companyNumber, String chargeId,
          InternalChargeApi requestBody) {
    OffsetDateTime deltaAt = requestBody.getInternalData().getDeltaAt();
    String type = "mortgage_delta";

    OffsetDateTime at = requestBody.getInternalData().getDeltaAt();

    String by = requestBody.getInternalData().getUpdatedBy();
    final Updated updated =
            new Updated().setAt(at.toLocalDateTime()).setType(type).setBy(by);
    var chargesDocument = new ChargesDocument().setId(chargeId)
            .setCompanyNumber(companyNumber).setData(requestBody.getExternalData())
            .setDeltaAt(deltaAt.toLocalDateTime())
            .setUpdated(updated);

    return chargesDocument;
  }
}
