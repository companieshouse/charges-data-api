package uk.gov.companieshouse.charges.data.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.ChargesDocument.Updated;
import uk.gov.companieshouse.charges.data.service.ChargesService;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@SpringBootTest
public class ChargesControllerTest {
    private final String companyNumber = "02588581";
    private final String chargeId = "18588520";
    private final int itemsPerPage = 1;
    private final int startIndex = 0;
    private final String CHARGES_PUT_URL = "/company/" + companyNumber + "/charge/" + chargeId + "/internal";
    private final String CHARGE_DETAILS_GET_URL = "/company/" + companyNumber + "/charges/" + chargeId;
    private final String CHARGES_GET_URL = "/company/" + companyNumber + "/charges/" + itemsPerPage + "/" + startIndex;
    private final String CHARGES_DELETE_URL = String.format("/company/%s/charges/%s", companyNumber, chargeId);


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
        mockMvc.perform(get(CHARGE_DETAILS_GET_URL)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Retrieve company charges for a given company number")
    void getCharges() throws Exception {
        var charges = new ChargesApi();
        when(chargesService.findCharges(any(), any(), any())).thenReturn(Optional.of(charges));
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

    public ChargesDocument transform(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {

        String type = "mortgage_delta";

        OffsetDateTime at = requestBody.getInternalData().getDeltaAt();

        String by = requestBody.getInternalData().getUpdatedBy();
        OffsetDateTime deltaAt = requestBody.getInternalData().getDeltaAt();
        final Updated updated =
                new Updated().setAt(at.toLocalDateTime()).setType(type).setBy(by);
        var chargesDocument = new ChargesDocument().setId(chargeId)
                .setCompanyNumber(companyNumber).setData(requestBody.getExternalData())
                .setDeltaAt(deltaAt)
                .setUpdated(updated);
        return chargesDocument;
    }

    @Test
    @DisplayName("Company Charges DELETE request")
    void callChargeDeleteRequest() throws Exception {
        doNothing().when(chargesService).deleteCharge(anyString(), anyString());

        mockMvc.perform(delete(CHARGES_DELETE_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456789"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Company Charges DELETE request - NotFound status code 404 ")
    void callChargeDeleteRequestIllegalArgument() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .when(chargesService).deleteCharge(anyString(), anyString());

        mockMvc.perform(delete(CHARGES_DELETE_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456789"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Company Charges DELETE request - BadRequest status code 400")
    void callChargeDeleteRequestBadRequest() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
                .when(chargesService).deleteCharge(anyString(), anyString());

        mockMvc.perform(delete(CHARGES_DELETE_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456789"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Company Charges DELETE request - MethodNotAllowed status code 405")
    void callChargeDeleteRequestMethodNotAllowed() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED))
                .when(chargesService).deleteCharge(anyString(), anyString());

        mockMvc.perform(put(CHARGES_DELETE_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456789"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Company Charges DELETE request - InternalServerError status code 500")
    void callChargeDeleteRequestInternalServerError() throws Exception {

        doThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(chargesService).deleteCharge(anyString(), anyString());

        mockMvc.perform(delete(CHARGES_DELETE_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456789"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Company Charges DELETE request - ServiceUnavailable status code 503")
    void callChargeDeleteRequestServiceUnavailable() throws Exception {

        doThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE))
                .when(chargesService).deleteCharge(anyString(), anyString());

        mockMvc.perform(delete(CHARGES_DELETE_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456789"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Company Charges DELETE request - BadGateway status code 502")
    void callChargeDeleteRequestBadGatewayError() throws Exception {

        doThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY))
                .when(chargesService).deleteCharge(anyString(), anyString());

        mockMvc.perform(delete(CHARGES_DELETE_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", "123456789"))
                .andExpect(status().isBadGateway());
    }
}
