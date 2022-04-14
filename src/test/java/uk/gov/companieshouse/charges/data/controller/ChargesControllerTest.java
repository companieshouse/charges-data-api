package uk.gov.companieshouse.charges.data.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.util.NestedServletException;
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
    private final String companyNumber = "02588581";
    private final String chargeId = "18588520";
    private final int itemsPerPage = 1;
    private final int startIndex = 0;
    private final String CHARGES_PUT_URL = "/company/" + companyNumber + "/charge/" + chargeId + "/internal";
    private final String CHARGE_DETAILS_GET_URL = "/company/" + companyNumber + "/charges/" + chargeId;
    private final String CHARGES_GET_URL = "/company/" + companyNumber + "/charges/" + itemsPerPage + "/" + startIndex;

    private MockMvc mockMvc;

    @Mock
    private Logger logger;

    @Mock
    private ChargesService chargesService;

    @InjectMocks
    private ChargesController chargesController;

    @Mock
    private ChargesTransformer chargesTransformer;

    private final ObjectMapper mapper = new ObjectMapper();

    private final Gson gson = new Gson();

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
        mockMvc.perform(put(CHARGES_PUT_URL).contentType(APPLICATION_JSON)
                .header("x-request-id", companyNumber)
                .content(gson.toJson(request))).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Charges PUT request throws exception")
    public void callChargesPutRequestThrowException() throws Exception {
        InternalChargeApi request = createChargesDocument();
        String contextId = companyNumber;
        doThrow(new Exception("Test exception")).when(chargesService).upsertCharges(eq(contextId), eq(companyNumber), eq(chargeId), any(InternalChargeApi.class));
        NestedServletException exception = assertThrows( NestedServletException.class, () -> {
            mockMvc.perform(put(CHARGES_PUT_URL).contentType(APPLICATION_JSON)
                    .header("x-request-id", companyNumber)
                    .content(gson.toJson(request))).andExpect(status().isOk());
        });
        assertEquals("Test exception", exception.getRootCause().getMessage());
    }

    @Test()
    @DisplayName("When calling get charges - returns a 500 INTERNAL SERVER ERROR")
    void getChargeInternalServerError(){
        when(chargesService.getChargeDetails(any(), any())).thenThrow(RuntimeException.class);

        assertThatThrownBy(() ->
                mockMvc.perform(get(CHARGE_DETAILS_GET_URL))
                        .andExpect(status().isInternalServerError())
                        .andExpect(content().string(""))
        ).hasCause(new RuntimeException());
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

        mockMvc.perform(get(CHARGE_DETAILS_GET_URL)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Retrieve company charges for a given company number")
    void getCharges() throws Exception {
        var charges = new ChargesApi();
        when(chargesService.findCharges(any(), any())).thenReturn(Optional.of(charges));
        mockMvc.perform(get(CHARGES_GET_URL))
                .andExpect(status().isOk());
    }

    private InternalChargeApi createChargesDocument() throws IOException {
        String incomingData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                        resourceFile.getInputStream())));
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper.readValue(incomingData, InternalChargeApi.class);
    }

    private ChargesDocument transform(String companyNumber, String chargeId, InternalChargeApi requestBody) {
        OffsetDateTime at = requestBody.getInternalData().getDeltaAt();

        String by = requestBody.getInternalData().getUpdatedBy();
        final Updated updated = new Updated().setAt(at.toLocalDate()).setType("mortgage_delta").setBy(by);

        return new ChargesDocument().setId(chargeId)
                .setCompanyNumber(companyNumber).setData(requestBody.getExternalData())
                .setUpdated(updated);
    }

}
