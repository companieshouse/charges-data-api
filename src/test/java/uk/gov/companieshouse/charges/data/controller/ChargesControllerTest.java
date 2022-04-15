package uk.gov.companieshouse.charges.data.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.Updated;
import uk.gov.companieshouse.charges.data.service.ChargesService;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class ChargesControllerTest {

    private final String CHARGES_PUT_URL = "/company/%s/charge/%s/internal";
    private final String CHARGE_DETAILS_GET_URL = "/company/%s/charges/%s";
    private final String CHARGES_GET_URL_WITH_PAGINATION = "/company/%s/charges/%d/%d";
    private final String CHARGES_GET_URL = "/company/%s/charges";
    private final String companyNumber = "02588581";
    private final String chargeId = "02588581";
    private final String requestId = "02588581";

    private MockMvc mockMvc;

    @Mock
    private Logger logger;

    @Mock
    private ChargesService chargesService;

    @InjectMocks
    private ChargesController chargesController;

    @Mock
    private ChargesTransformer chargesTransformer;

    private ObjectMapper mapper = new ObjectMapper();

    private Gson gson = new Gson();

    @Value("file:src/test/resources/charges-api-request-data.json")
    Resource resourceFile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chargesController)
                .build();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    @DisplayName("Charges PUT request")
    public void callChargesPutRequest() throws Exception {
        InternalChargeApi request = createChargesDocument();
        String url = String.format(CHARGES_PUT_URL, companyNumber, chargeId);
        mockMvc.perform(put(url).contentType(APPLICATION_JSON)
                .header("x-request-id", requestId)
                .content(mapper.writeValueAsString(request))).andExpect(status().isOk());
        verify(chargesService, times(1)).upsertCharges(requestId, companyNumber, chargeId, request);
    }

    @Test()
    @DisplayName("When calling get charges - returns a 500 INTERNAL SERVER ERROR")
    void getChargeInternalServerError() throws Exception {
        when(chargesService.getChargeDetails(any(), any())).thenThrow(RuntimeException.class);

        assertThatThrownBy(() ->
                mockMvc.perform(get(String.format(CHARGE_DETAILS_GET_URL, companyNumber, chargeId)))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().string(""))
        ).hasCause(new RuntimeException());
        verify(chargesService, times(1)).getChargeDetails(companyNumber, chargeId);
    }

    @Test
    @DisplayName("Retrieve company charge details for a given company number and chargeId")
    void getCharge() throws Exception {

        InternalChargeApi request = createChargesDocument();
        ChargesDocument document = transform(companyNumber, chargeId, request);
        when(chargesService.getChargeDetails(companyNumber, chargeId)).thenReturn(Optional.of(document.getData()));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        mockMvc.perform(get(String.format(CHARGE_DETAILS_GET_URL, companyNumber, chargeId)))
                .andExpect(status().isOk());
        verify(chargesService, times(1)).getChargeDetails(companyNumber, chargeId);
    }

    @Test
    @DisplayName("Retrieve company charges for a given company number")
    void getChargesWithPagination() throws Exception {
        int itemsPerPage = 1;
        int startingIndex = 0;
        var charges = new ChargesApi();
        when(chargesService.findCharges(any(), any())).thenReturn(Optional.of(charges));
        var requestUri = String.format(CHARGES_GET_URL_WITH_PAGINATION, companyNumber, itemsPerPage, startingIndex);
        mockMvc.perform(get(requestUri))
                .andExpect(status().isOk());
        verify(chargesService, times(1)).findCharges(companyNumber, PageRequest.of(startingIndex, itemsPerPage));
    }

    @Test
    @DisplayName("Retrieve company charges for a given company number")
    void getCharges() throws Exception {

        var charges = new ChargesApi();
        when(chargesService.findCharges(any(), any())).thenReturn(Optional.of(charges));
        var requestUri = String.format(CHARGES_GET_URL, companyNumber);
        mockMvc.perform(get(requestUri))
                .andExpect(status().isOk());
        verify(chargesService, times(1)).findCharges(companyNumber, Pageable.unpaged());
    }


    private InternalChargeApi createChargesDocument() throws IOException {
        String incomingData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                        resourceFile.getInputStream())));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        InternalChargeApi internalChargeApi =
                mapper.readValue(incomingData, InternalChargeApi.class);
        return internalChargeApi;
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
