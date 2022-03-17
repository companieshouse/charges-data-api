package uk.gov.companieshouse.charges.data.repository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.Updated;
import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@DataMongoTest(excludeAutoConfiguration = EmbeddedMongoAutoConfiguration.class)
public class RepositoryITest {

  static final MongoDBContainer mongoDBContainer = new MongoDBContainer(
      DockerImageName.parse("mongo:4.0.10"));

  @Autowired
  ChargesRepository chargesRepository;

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
  void should_save_and_retrieve_charges_data() {

    ChargesDocument chargesDocument = createChargesDocument("CH253434", "jbvgV-Zu-i8bRkypE0AEJx1N_Sk");

    chargesRepository.save(chargesDocument);

    List<ChargesDocument> documents = chargesRepository.findAll();
    assertThat(documents).hasSize(1);
    assertThat(documents.get(0).getData().getId()).isEqualTo(chargesDocument.getData().getId());
    assertThat(documents.get(0).getCompanyNumber()).isEqualTo(chargesDocument.getCompanyNumber());
    assertThat(documents.get(0).getUpdated().getAt()).isEqualTo(chargesDocument.getUpdated().getAt());
  }

  private ChargesDocument createChargesDocument(String companyNumber, String chargeId) {

    ChargeApi externalData = new ChargeApi();
    externalData.setId(chargeId);
    externalData.setChargeNumber(9);
    externalData.setCreatedOn(LocalDate.now());
    Updated updated = new Updated().setAt(OffsetDateTime.now().toLocalDate()).setBy("Charges Consumer").setType("mortgage_delta");
    return new ChargesDocument().setCompanyNumber(companyNumber).setData(externalData).setUpdated(updated);
  }

  @AfterAll
  static void tear(){
    mongoDBContainer.stop();
  }

}
