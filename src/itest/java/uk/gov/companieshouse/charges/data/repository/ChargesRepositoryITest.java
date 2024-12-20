package uk.gov.companieshouse.charges.data.repository;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.AbstractIntegrationTest;
import uk.gov.companieshouse.charges.data.model.ChargesAggregate;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.ChargesDocument.Updated;

class ChargesRepositoryITest extends AbstractIntegrationTest {

    @Autowired
    ChargesRepository chargesRepository;

    @BeforeEach
    public void setup() {
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
        assertThat(documents.getFirst().getData().getId()).isEqualTo(chargesDocument.getData().getId());
        assertThat(documents.getFirst().getCompanyNumber()).isEqualTo(chargesDocument.getCompanyNumber());
        assertThat(documents.getFirst().getDeltaAt()).isEqualTo(chargesDocument.getDeltaAt());
        assertThat(documents.getFirst().getUpdated().getAt()).isEqualTo(chargesDocument.getUpdated().getAt());
        assertThat(documents.getFirst().getData().getAcquiredOn()).isEqualTo(chargesDocument.getData().getAcquiredOn());
    }

    @Test
    void should_save_and_retrieve_charges_data_deltaAt_Null() throws IOException {

        ChargesDocument chargesDocument = createChargesDocument(UUID.randomUUID().toString(), UUID.randomUUID().toString(),
                "charge-api-request-data-2.json");

        chargesRepository.save(chargesDocument);

        List<ChargesDocument> documents = chargesRepository.findAll();
        assertThat(documents).hasSize(1);
        assertThat(documents.getFirst().getData().getId()).isEqualTo(chargesDocument.getData().getId());
        assertThat(documents.getFirst().getCompanyNumber()).isEqualTo(chargesDocument.getCompanyNumber());
        assertThat(documents.getFirst().getDeltaAt()).isEqualTo(chargesDocument.getDeltaAt());
        assertThat(documents.getFirst().getUpdated().getAt()).isEqualTo(chargesDocument.getUpdated().getAt());
        assertThat(documents.getFirst().getData().getAcquiredOn()).isEqualTo(chargesDocument.getData().getAcquiredOn());
    }

    @DisplayName("Repository returns unfiltered charges when no filter specified")
    @Test
    void findChargesUnfiltered() throws IOException {
        // given
        ChargesDocument chargesFullySatisfied = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargesFullySatisfied.getData().setStatus(ChargeApi.StatusEnum.FULLY_SATISFIED);
        ChargesDocument chargesSatisfied = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargesSatisfied.getData().setStatus(ChargeApi.StatusEnum.SATISFIED);
        ChargesDocument chargesPartSatisfied = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargesPartSatisfied.getData().setStatus(ChargeApi.StatusEnum.PART_SATISFIED);
        ChargesDocument chargesOutstanding = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargesOutstanding.getData().setStatus(ChargeApi.StatusEnum.OUTSTANDING);

        chargesRepository.saveAll(
                Arrays.asList(chargesFullySatisfied, chargesSatisfied,
                        chargesPartSatisfied, chargesOutstanding));

        // when
        ChargesAggregate chargesAggregate = chargesRepository.findCharges(
                "00006400", emptyList(), 0, 4);

        // then
        assertEquals(4, chargesAggregate.getChargesDocuments().size());
    }

    @DisplayName("Repository returns outstanding and part-satisfied charges when filter specified")
    @Test
    void findChargesFiltered() throws IOException {
        // given
        ChargesDocument chargesFullySatisfied = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargesFullySatisfied.getData().setStatus(ChargeApi.StatusEnum.FULLY_SATISFIED);
        ChargesDocument chargesSatisfied = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargesSatisfied.getData().setStatus(ChargeApi.StatusEnum.SATISFIED);
        ChargesDocument chargesPartSatisfied = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargesPartSatisfied.getData().setStatus(ChargeApi.StatusEnum.PART_SATISFIED);
        ChargesDocument chargesOutstanding = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargesOutstanding.getData().setStatus(ChargeApi.StatusEnum.OUTSTANDING);
        chargesRepository.saveAll(
                Arrays.asList(chargesFullySatisfied, chargesSatisfied,
                        chargesPartSatisfied, chargesOutstanding));

        // when
        ChargesAggregate chargesAggregate = chargesRepository.findCharges(
                "00006400", Arrays.asList(ChargeApi.StatusEnum.SATISFIED.toString(), ChargeApi.StatusEnum.FULLY_SATISFIED.toString()), 0, 4);

        // then
        assertEquals(2, chargesAggregate.getChargesDocuments().size());
        assertEquals(chargesPartSatisfied.getId(), chargesAggregate.getChargesDocuments().getFirst().getId());
        assertEquals(chargesPartSatisfied.getData().getStatus(), chargesAggregate.getChargesDocuments().getFirst().getData().getStatus());
        assertEquals(chargesOutstanding.getId(), chargesAggregate.getChargesDocuments().get(1).getId());
        assertEquals(chargesOutstanding.getData().getStatus(), chargesAggregate.getChargesDocuments().get(1).getData().getStatus());
    }

    @DisplayName("Repository returns charges in order with correct page size")
    @Test
    void findChargesWithPaging() throws IOException {
        // given
        ChargesDocument chargeOne = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeOne.getData().chargeNumber(1).createdOn(LocalDate.of(2017, 7, 10));
        ChargesDocument chargeTwo = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeTwo.getData().chargeNumber(2).createdOn(LocalDate.of(2017, 7, 10));
        ChargesDocument chargeThree = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeThree.getData().chargeNumber(1).createdOn(LocalDate.of(2018, 7, 10));
        ChargesDocument chargeFour = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeFour.getData().chargeNumber(1).createdOn(LocalDate.of(2017, 10, 10));
        chargesRepository.saveAll(
                Arrays.asList(chargeOne, chargeTwo,
                        chargeThree, chargeFour));

        // when
        ChargesAggregate chargesAggregate = chargesRepository.findCharges("00006400",
                emptyList(), 0, 3);

        // then
        assertEquals(4L, chargesAggregate.getTotalCharges().getFirst().getCount());
        assertEquals(3, chargesAggregate.getChargesDocuments().size());
        assertEquals(chargeThree.getId(), chargesAggregate.getChargesDocuments().getFirst().getId());
        assertEquals(chargeFour.getId(), chargesAggregate.getChargesDocuments().get(1).getId());
        assertEquals(chargeTwo.getId(), chargesAggregate.getChargesDocuments().get(2).getId());
    }

    @DisplayName("Repository returns charges in order with start index")
    @Test
    void findChargesWithSpecifiedStartIndex() throws IOException {
        // given
        ChargesDocument chargeOne = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeOne.getData().chargeNumber(1).createdOn(LocalDate.of(2017, 7, 10));
        ChargesDocument chargeTwo = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeTwo.getData().chargeNumber(2).createdOn(LocalDate.of(2017, 7, 10));
        ChargesDocument chargeThree = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeThree.getData().chargeNumber(1).createdOn(LocalDate.of(2018, 7, 10));
        ChargesDocument chargeFour = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeFour.getData().chargeNumber(1).createdOn(LocalDate.of(2017, 10, 10));
        chargesRepository.saveAll(
                Arrays.asList(chargeOne, chargeTwo,
                        chargeThree, chargeFour));

        // when
        ChargesAggregate chargesAggregate = chargesRepository.findCharges("00006400",
                emptyList(), 2, 3);

        // then
        assertEquals(4L, chargesAggregate.getTotalCharges().getFirst().getCount());
        assertEquals(2, chargesAggregate.getChargesDocuments().size());
        assertEquals(chargeTwo.getId(), chargesAggregate.getChargesDocuments().getFirst().getId());
        assertEquals(chargeOne.getId(), chargesAggregate.getChargesDocuments().get(1).getId());
    }

    @DisplayName("Repository returns charges first sorted by created_on and second sorted by charge_number")
    @Test
    void findChargesSorted() throws IOException {
        // given
        ChargesDocument chargeOne = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeOne.getData().chargeNumber(1).createdOn(LocalDate.of(2017, 7, 10));
        ChargesDocument chargeTwo = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeTwo.getData().chargeNumber(2).createdOn(LocalDate.of(2017, 7, 10));
        ChargesDocument chargeThree = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeThree.getData().chargeNumber(1).createdOn(LocalDate.of(2018, 7, 10));
        ChargesDocument chargeFour = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeFour.getData().chargeNumber(1).createdOn(LocalDate.of(2017, 10, 10));
        chargesRepository.saveAll(
                Arrays.asList(chargeOne, chargeTwo,
                        chargeThree, chargeFour));

        // when
        ChargesAggregate chargesAggregate = chargesRepository.findCharges("00006400",
                emptyList(), 0, 4);

        // then
        assertEquals(4, chargesAggregate.getChargesDocuments().size());
        assertEquals(chargeThree.getId(), chargesAggregate.getChargesDocuments().getFirst().getId());
        assertEquals(chargeFour.getId(), chargesAggregate.getChargesDocuments().get(1).getId());
        assertEquals(chargeTwo.getId(), chargesAggregate.getChargesDocuments().get(2).getId());
        assertEquals(chargeOne.getId(), chargesAggregate.getChargesDocuments().get(3).getId());
    }

    @DisplayName("Repository returns filtered charges first sorted by created_on or delivered_on if null and second sorted by charge_number")
    @Test
    void findChargesSortedDeliveredOnWithFilter() throws IOException {
        // given
        ChargesDocument chargeOne = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeOne.getData().chargeNumber(1).createdOn(LocalDate.of(2017, 7, 10));
        chargeOne.getData().setStatus(ChargeApi.StatusEnum.FULLY_SATISFIED);

        ChargesDocument chargeTwo = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeTwo.getData().chargeNumber(2).createdOn(LocalDate.of(2017, 7, 10));
        chargeTwo.getData().setStatus(ChargeApi.StatusEnum.OUTSTANDING);

        ChargesDocument chargeThree = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeThree.getData().chargeNumber(1).createdOn(LocalDate.of(2018, 7, 10));
        chargeThree.getData().setStatus(ChargeApi.StatusEnum.OUTSTANDING);

        ChargesDocument chargeFour = createChargesDocument("00006400", UUID.randomUUID().toString(),
                "charge-api-request-data-1.json");
        chargeFour.getData().chargeNumber(1).createdOn(null);
        chargeFour.getData().chargeNumber(1).deliveredOn(LocalDate.of(2017, 10, 10));
        chargeFour.getData().setStatus(ChargeApi.StatusEnum.OUTSTANDING);

        chargesRepository.saveAll(
                Arrays.asList(chargeOne, chargeTwo,
                        chargeThree, chargeFour));

        List<String> filter = Arrays.asList(ChargeApi.StatusEnum.SATISFIED.toString(), ChargeApi.StatusEnum.FULLY_SATISFIED.toString());

        // when

        ChargesAggregate chargesAggregate = chargesRepository.findCharges("00006400", filter, 0, 4);

        // then
        assertEquals(3, chargesAggregate.getChargesDocuments().size());
        assertEquals(chargeThree.getId(), chargesAggregate.getChargesDocuments().getFirst().getId());
        assertEquals(chargeFour.getId(), chargesAggregate.getChargesDocuments().get(1).getId());
        assertEquals(chargeTwo.getId(), chargesAggregate.getChargesDocuments().get(2).getId());
    }

    @DisplayName("Repository returns no charges when filtered and there are no matches")
    @Test
    void findChargesNoResultsWithFilter() {
        // given
        List<String> filter = Arrays.asList(ChargeApi.StatusEnum.SATISFIED.toString(), ChargeApi.StatusEnum.FULLY_SATISFIED.toString());

        // when

        ChargesAggregate chargesAggregate = chargesRepository.findCharges("00006400", filter, 0, 4);

        // then
        assertEquals(0, chargesAggregate.getChargesDocuments().size());
        assertEquals(0, chargesAggregate.getTotalCharges().size());
    }

    @DisplayName("Repository returns no charges when there are no matches")
    @Test
    void findChargesNoResults() {
        // given
        // when

        ChargesAggregate chargesAggregate = chargesRepository.findCharges("00006400", emptyList(), 0, 4);

        // then
        assertEquals(0, chargesAggregate.getChargesDocuments().size());
        assertEquals(0, chargesAggregate.getTotalCharges().size());
    }

    private ChargesDocument createChargesDocument(String companyNumber, String chargeId, String filename) throws IOException {
        String incomingData = loadInputFile(filename);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        InternalChargeApi chargesDocument = mapper.readValue(incomingData, InternalChargeApi.class);
        return transform(companyNumber, chargeId, chargesDocument);
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
        return new ChargesDocument().setId(chargeId)
                .setCompanyNumber(companyNumber).setData(requestBody.getExternalData())
                .setDeltaAt(deltaAt)
                .setUpdated(updated);
    }

    private String loadFile(String dir, String fileName) {
        try {
            return FileUtils.readFileToString(ResourceUtils.getFile("file:src/itest/resources/payload/" + dir
                    + "/" + fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to locate file %s", fileName));
        }
    }

    private String loadInputFile(String fileName) {
        return loadFile("input", fileName);
    }

}
