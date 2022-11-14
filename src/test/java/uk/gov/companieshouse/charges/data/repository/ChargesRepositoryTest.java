package uk.gov.companieshouse.charges.data.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargeLink;
import uk.gov.companieshouse.api.charges.ParticularsApi;
import uk.gov.companieshouse.api.charges.PersonsEntitledApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;

@DataMongoTest
@ExtendWith(SpringExtension.class)
class ChargesRepositoryTest {

    @Autowired
    private ChargesRepository chargesRepository;

    @AfterEach
    void teardown() {
        chargesRepository.deleteAll();
    }

    @DisplayName("Repository returns unfiltered charges when no filter specified")
    @Test
    void findCharges() {
        // given
        ChargesDocument chargesFullySatisfied = getCharges("1", ChargeApi.StatusEnum.FULLY_SATISFIED);
        ChargesDocument chargesSatisfied = getCharges("2", ChargeApi.StatusEnum.SATISFIED);
        ChargesDocument chargesPartSatisfied = getCharges("3", ChargeApi.StatusEnum.PART_SATISFIED);
        ChargesDocument chargesOutstanding = getCharges("4", ChargeApi.StatusEnum.OUTSTANDING);
        chargesRepository.saveAll(
                Arrays.asList(chargesFullySatisfied, chargesSatisfied,
                        chargesPartSatisfied, chargesOutstanding));

        // when
        Page<ChargesDocument> chargesPage = chargesRepository.findCharges(
                "00006400",  Collections.emptyList(), PageRequest.ofSize(4));
        List<ChargesDocument> chargesList = chargesPage.toList();

        // then
        assertEquals(4, chargesList.size());
    }

    @DisplayName("Repository returns outstanding and part-satisfied charges when filter specified")
    @Test
    void findChargesFiltered() {
        // given
        ChargesDocument chargesFullySatisfied = getCharges("1", ChargeApi.StatusEnum.FULLY_SATISFIED);
        ChargesDocument chargesSatisfied = getCharges("2", ChargeApi.StatusEnum.SATISFIED);
        ChargesDocument chargesPartSatisfied = getCharges("3", ChargeApi.StatusEnum.PART_SATISFIED);
        ChargesDocument chargesOutstanding = getCharges("4", ChargeApi.StatusEnum.OUTSTANDING);
        chargesRepository.saveAll(
                Arrays.asList(chargesFullySatisfied, chargesSatisfied,
                        chargesPartSatisfied, chargesOutstanding));

        // when
        Page<ChargesDocument> chargesPage = chargesRepository.findCharges(
                "00006400",  Arrays.asList(ChargeApi.StatusEnum.SATISFIED, ChargeApi.StatusEnum.FULLY_SATISFIED), PageRequest.ofSize(4));
        List<ChargesDocument> chargesList = chargesPage.toList();

        // then
        assertEquals(2, chargesList.size());
        assertEquals(chargesPartSatisfied.getId(), chargesList.get(0).getId());
        assertEquals(chargesPartSatisfied.getData().getStatus(), chargesList.get(0).getData().getStatus());
        assertEquals(chargesOutstanding.getId(), chargesList.get(1).getId());
        assertEquals(chargesOutstanding.getData().getStatus(), chargesList.get(1).getData().getStatus());
    }

    private ChargesDocument getCharges(String id, ChargeApi.StatusEnum status) {
        ChargesDocument document = new ChargesDocument();
        document.setId(id);
        document.setUpdated(new ChargesDocument.Updated()
                    .setAt(LocalDateTime.of(2015,6,26,8,31))
                    .setBy("558d0dd3afa24c7af459c706")
                    .setType("mortgage_delta"));
        document.setData(new ChargeApi()
                    .etag("822f73eb83a4aeb97853572e9f238beb3b296920")
                    .deliveredOn(LocalDate.of(2017,6,26))
                    .chargeNumber(2)
                    .personsEntitled(Collections.singletonList(new PersonsEntitledApi().name("(Miss) E.R.Linley")))
                    .particulars(new ParticularsApi()
                            .type(ParticularsApi.TypeEnum.SHORT_PARTICULARS)
                            .description("Beechen cliff house, beechen cliff road, bath."))
                    .createdOn(LocalDate.of(2017,6,26))
                    .links(new ChargeLink().self("/company/00006400/charges/Wt1poFfN7wAkoSpHyv922w_ZpB0"))
                    .status(status));
        document.setCompanyNumber("00006400");
        return document;
    }
}
