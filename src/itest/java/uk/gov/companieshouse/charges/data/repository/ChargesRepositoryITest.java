package uk.gov.companieshouse.charges.data.repository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.AbstractIntegrationTest;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.ChargesDocument.Updated;

import static org.assertj.core.api.Assertions.assertThat;

public class ChargesRepositoryITest extends AbstractIntegrationTest {

  @Autowired
  ChargesRepository chargesRepository;

  @BeforeEach
  public void setup(){
    this.chargesRepository.deleteAll();
  }

  @Test
  void should_return_mongodb_as_running() {
    Assertions.assertTrue(mongoDBContainer.isRunning());
  }

  @Test
  void should_save_and_retrieve_charges_data() throws IOException {

    ChargesDocument chargesDocument = createChargesDocument(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            "charge-api-request-data-1.json");

    chargesRepository.save(chargesDocument);

    List<ChargesDocument> documents = chargesRepository.findAll();
    assertThat(documents).hasSize(1);
    assertThat(documents.get(0).getData().getId()).isEqualTo(chargesDocument.getData().getId());
    assertThat(documents.get(0).getCompanyNumber()).isEqualTo(chargesDocument.getCompanyNumber());
    assertThat(documents.get(0).getDeltaAt()).isEqualTo(chargesDocument.getDeltaAt());
    assertThat(documents.get(0).getUpdated().getAt()).isEqualTo(chargesDocument.getUpdated().getAt());
    assertThat(documents.get(0).getData().getAcquiredOn()).isEqualTo(chargesDocument.getData().getAcquiredOn());
  }

  @Test
  void should_save_and_retrieve_charges_data_deltaAt_Null() throws IOException {

    ChargesDocument chargesDocument = createChargesDocument(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
            "charge-api-request-data-2.json");

    chargesRepository.save(chargesDocument);

    List<ChargesDocument> documents = chargesRepository.findAll();
    assertThat(documents).hasSize(1);
    assertThat(documents.get(0).getData().getId()).isEqualTo(chargesDocument.getData().getId());
    assertThat(documents.get(0).getCompanyNumber()).isEqualTo(chargesDocument.getCompanyNumber());
    assertThat(documents.get(0).getDeltaAt()).isEqualTo(chargesDocument.getDeltaAt());
    assertThat(documents.get(0).getUpdated().getAt()).isEqualTo(chargesDocument.getUpdated().getAt());
    assertThat(documents.get(0).getData().getAcquiredOn()).isEqualTo(chargesDocument.getData().getAcquiredOn());
  }

  private ChargesDocument createChargesDocument(String companyNumber, String chargeId, String filename) throws IOException {
    String incomingData = loadInputFile(filename);
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    InternalChargeApi chargesDocument = mapper.readValue(incomingData, InternalChargeApi.class);
    ChargesDocument transformedChargesDocument =
            transform(companyNumber, chargeId, chargesDocument);

    return transformedChargesDocument;
  }

  public ChargesDocument transform(String companyNumber, String chargeId,
          InternalChargeApi requestBody) {
    OffsetDateTime deltaAt = requestBody.getInternalData().getDeltaAt();
    String type = "mortgage_delta";

    OffsetDateTime at = deltaAt != null ? deltaAt
            : OffsetDateTime.parse("2015-06-26T08:31:40.028Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);

    String by = requestBody.getInternalData().getUpdatedBy();
    final Updated updated =
            new Updated().setAt(at.toLocalDateTime()).setType(type).setBy(by);
    var chargesDocument = new ChargesDocument().setId(chargeId)
            .setCompanyNumber(companyNumber).setData(requestBody.getExternalData())
            .setDeltaAt(deltaAt)
            .setUpdated(updated);

    return chargesDocument;
  }

  private String loadFile(String dir, String fileName) {
    try {
      return FileUtils.readFileToString(ResourceUtils.getFile("file:src/itest/resources/payload/"+ dir
              + "/" + fileName), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(String.format("Unable to locate file %s", fileName));
    }
  }

  private String loadInputFile(String fileName) {
    return loadFile("input", fileName);
  }

}
