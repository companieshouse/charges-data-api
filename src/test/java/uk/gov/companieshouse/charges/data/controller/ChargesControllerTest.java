package uk.gov.companieshouse.charges.data.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.Objects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.config.WebSecurityConfig;
import uk.gov.companieshouse.charges.data.exception.BadRequestException;
import uk.gov.companieshouse.charges.data.exception.ConflictException;
import uk.gov.companieshouse.charges.data.exception.NotFoundException;
import uk.gov.companieshouse.charges.data.exception.ServiceUnavailableException;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.model.ChargesDocument.Updated;
import uk.gov.companieshouse.charges.data.service.ChargesService;
import uk.gov.companieshouse.charges.data.transform.ChargesTransformer;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = ChargesController.class)
@ContextConfiguration(classes = {ChargesController.class, ControllerExceptionHandler.class})
@Import({WebSecurityConfig.class})
class ChargesControllerTest {

    private static final String COMPANY_NUMBER = "02588581";
    private static final String CHARGE_ID = "18588520";
    private static final String CHARGES_PUT_URL = String.format("/company/%s/charge/%s/internal",
            COMPANY_NUMBER, CHARGE_ID);
    private static final String CHARGE_DETAILS_GET_URL = String.format("/company/%s/charges/%s",
            COMPANY_NUMBER, CHARGE_ID);
    private static final String CHARGES_GET_URL = String.format("/company/%s/charges", COMPANY_NUMBER);
    private static final String CHARGES_DELETE_URL = String.format("/company/%s/charge/%s/internal",
            COMPANY_NUMBER, CHARGE_ID);
    private static final String X_REQUEST_ID = "123";
    private static final String DELTA_AT = "20241205123045999999";

    private static final String ERIC_ALLOWED_ORIGIN="ERIC-Allowed-Origin";
    private static final String ERIC_IDENTITY="ERIC-Identity";
    private static final String ERIC_IDENTITY_TYPE="ERIC-Identity-Type";
    private static final String ORIGIN="Origin";
    private static final String ERIC_ALLOWED_ORIGIN_VALUE="some-origin";
    private static final String ERIC_IDENTITY_VALUE="123";
    private static final String ERIC_IDENTITY_TYPE_VALUE="key";
    private static final String ORIGIN_VALUE="http://www.test.com";


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChargesService chargesService;

    @InjectMocks
    private ChargesController chargesController;

    @MockBean
    private ChargesTransformer chargesTransformer;

    private final ObjectMapper mapper = new ObjectMapper();

    private final Gson gson = new GsonBuilder().setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Value("file:src/test/resources/charges-api-request-data.json")
    Resource resourceFile;

    @BeforeEach
    void setUp() {
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    @DisplayName("Charges PUT request")
    void callChargesPutRequest() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", X_REQUEST_ID)
                .header("ERIC-Identity" , "SOME_IDENTITY")
                .header("ERIC-Identity-Type", "KEY")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(request)))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Charges PUT request fails when Oauth2 has privileges")
    void callChargesPutRequestOauth2WithPrivileges() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "OAUTH2")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Charges PUT request fails when missing privileges for KEY identity type")
    void callChargesPutRequestMissingAuthorisationForKeyType() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "KEY")
                        .content(gson.toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Charges PUT request fails when missing privileges for OAUTH2 identity type")
    void callChargesPutRequestMissingAuthorisationForOauth2Type() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "OAUTH2")
                        .content(gson.toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Charges PUT request fails when no ERIC-Identity")
    void chargesPutRequestFailsWhenUnauthenticatedWithNoIdentity() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .content(gson.toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Charges PUT request fails when no ERIC-Identity-Type")
    void chargesPutRequestFailsWhenUnauthenticatedWithNoIdentityType() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .content(gson.toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Charges PUT request returns 503 Service Unavailable when Mongo errors")
    void chargesPutRequestFailsWith503Error() throws Exception {
        // given
        InternalChargeApi request = createChargesDocument();
        doThrow(ServiceUnavailableException.class).when(chargesService).upsertCharges(any(), any(), any(), any());

        // when
        ResultActions actual = mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "KEY")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(request)));

        // then
        actual.andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Charges PUT request fails when no ERIC-Identity is present but has a valid ERIC-Identity-Type of Key")
    void chargesPutRequestFailsWhenUnauthenticatedWithNoIdentityButValidIdentityTypeOfKey() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity-Type", "KEY")
                        .content(gson.toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Charges PUT request fails when no ERIC-Identity is present but has a valid ERIC-Identity-Type of Oauth2")
    void chargesPutRequestFailsWhenUnauthenticatedWithNoIdentityButValidIdentityTypeOfOauth2() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity-Type", "OAUTH2")
                        .content(gson.toJson(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Charges GET request")
    void callChargesGetRequest() throws Exception {
        ChargesApi charge = new ChargesApi();
        doReturn(charge).when(chargesService).findCharges(anyString(), any());
        mockMvc.perform(get(CHARGES_GET_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "KEY")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app")
                        .content(gson.toJson(null)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET company charges request returns 503 Service Unavailable when Mongo errors")
    void getChargesRequest503Error() throws Exception {
        // given
        ChargesApi charge = new ChargesApi();
        doThrow(ServiceUnavailableException.class).when(chargesService).findCharges(anyString(), any());

        // when
        ResultActions actual = mockMvc.perform(get(CHARGES_GET_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", X_REQUEST_ID)
                .header("ERIC-Identity" , "SOME_IDENTITY")
                .header("ERIC-Identity-Type", "KEY")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .content(gson.toJson(null)));

        // then
        actual.andExpect(status().isServiceUnavailable());
    }


    @Test
    @DisplayName("Charges GET request success with no privileges for Key ERIC-Identity-Type")
    void callChargesGetRequestWithNoPrivilegesForKeyType() throws Exception {
        ChargesApi charge = new ChargesApi();
        doReturn(charge).when(chargesService).findCharges(anyString(), any());
        mockMvc.perform(get(CHARGES_GET_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "KEY")
                        .content(gson.toJson(null)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Charges GET request success with no privileges for Oauth2 ERIC-Identity-Type")
    void callChargesGetRequestWithNoPrivilegesForOauth2Type() throws Exception {
        ChargesApi charge = new ChargesApi();
        doReturn(charge).when(chargesService).findCharges(anyString(), any());

        mockMvc.perform(get(CHARGES_GET_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "OAUTH2")
                        .content(gson.toJson(null)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Charges GET request fails when no ERIC-Identity")
    void chargesGetRequestFailsWhenUnauthenticatedWithNoIdentity() throws Exception {
        mockMvc.perform(get(CHARGES_GET_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .content(gson.toJson(null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Charges GET request fails when no ERIC-Identity-Type")
    void chargesGetRequestFailsWhenUnauthenticatedWithNoIdentityType() throws Exception {
        mockMvc.perform(get(CHARGES_GET_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .content(gson.toJson(null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Charges GET request fails when no ERIC-Identity is present but has a valid ERIC-Identity-Type of Key")
    void chargesGetRequestFailsWhenUnauthenticatedWithNoIdentityButValidIdentityTypeOfKey() throws Exception {
        mockMvc.perform(get(CHARGES_GET_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity-Type", "KEY")
                        .content(gson.toJson(null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Charges GET request fails when no ERIC-Identity is present but has a valid ERIC-Identity-Type of Oauth2")
    void chargesGetRequestFailsWhenUnauthenticatedWithNoIdentityButValidIdentityTypeOfOauth2() throws Exception {
        mockMvc.perform(get(CHARGES_GET_URL)
                        .contentType(APPLICATION_JSON)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity-Type", "OAUTH2")
                        .content(gson.toJson(null)))
                .andExpect(status().isUnauthorized());
    }

    @Test()
    @DisplayName("When calling get charges - returns a 500 INTERNAL SERVER ERROR")
    void getChargeInternalServerError() throws Exception {
        when(chargesService.getChargeDetails(any(), any()))
                .thenThrow(new RuntimeException());

        mockMvc.perform(get(CHARGE_DETAILS_GET_URL)
                    .contentType(APPLICATION_JSON)
                    .header("x-request-id", X_REQUEST_ID)
                    .header("ERIC-Identity" , "SOME_IDENTITY")
                    .header("ERIC-Identity-Type", "KEY")
                    .header("ERIC-Authorised-Key-Privileges", "internal-app")
                    .content(gson.toJson(null)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Retrieve company charge details for a given company number and chargeId")
    void getCharge() throws Exception {
        InternalChargeApi request = createChargesDocument();
        ChargesDocument document = transform(COMPANY_NUMBER, CHARGE_ID, request);
        when(chargesService.getChargeDetails(COMPANY_NUMBER, CHARGE_ID)).thenReturn(document.getData());
        mockMvc.perform(get(CHARGE_DETAILS_GET_URL)
                    .header("x-request-id", X_REQUEST_ID)
                    .header("ERIC-Identity" , "SOME_IDENTITY")
                    .header("ERIC-Identity-Type", "KEY")
                    .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET single charge returns 503 Service Unavailable when Mongo errors")
    void getSingleCharge503Error() throws Exception {
        // given
        InternalChargeApi request = createChargesDocument();
        ChargesDocument document = transform(COMPANY_NUMBER, CHARGE_ID, request);
        when(chargesService.getChargeDetails(COMPANY_NUMBER, CHARGE_ID)).thenThrow(ServiceUnavailableException.class);

        // when
        ResultActions actual = mockMvc.perform(get(CHARGE_DETAILS_GET_URL)
                        .header("x-request-id", X_REQUEST_ID)

                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "KEY")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"));

       // then
       actual.andExpect(status().isServiceUnavailable());
    }


    @Test
    @DisplayName("Retrieve company charge details for non existent charge")
    void getChargNotFound() throws Exception {
        when(chargesService.getChargeDetails(COMPANY_NUMBER, CHARGE_ID)).thenThrow(NotFoundException.class);
        mockMvc.perform(get(CHARGE_DETAILS_GET_URL)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "KEY")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Retrieve company charges for a given company number")
    void getCharges() throws Exception {
        var charges = new ChargesApi();
        when(chargesService.findCharges(any(), any())).thenReturn(charges);
        mockMvc.perform(get(CHARGES_GET_URL)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "KEY")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET company charges for where charges does not exist")
    void getChargesNotFound() throws Exception {
        var charges = new ChargesApi();
        when(chargesService.findCharges(any(), any())).thenThrow(NotFoundException.class);
        mockMvc.perform(get(CHARGES_GET_URL)
                        .header("x-request-id", X_REQUEST_ID)
                        .header("ERIC-Identity" , "SOME_IDENTITY")
                        .header("ERIC-Identity-Type", "KEY")
                        .header("ERIC-Authorised-Key-Privileges", "internal-app"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Company Charges DELETE request")
    void callChargeDeleteRequest() throws Exception {
        // given

        // when
        ResultActions result = mockMvc.perform(delete(CHARGES_DELETE_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", X_REQUEST_ID)
                .header("ERIC-Identity" , "SOME_IDENTITY")
                .header("ERIC-Identity-Type", "KEY")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isOk());
    }

    @Test
    @DisplayName("Company Charges DELETE request when service returns Service Unavailable exception")
    void deleteCompanyChargesServerError() throws Exception {
        // given
        doThrow(ServiceUnavailableException.class).when(chargesService).deleteCharge(
                anyString(), anyString(), anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(CHARGES_DELETE_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("Company Charges DELETE request when service returns Bad Request exception")
    void deleteCompanyChargesBadRequest() throws Exception {
        // given
        doThrow(BadRequestException.class).when(chargesService).deleteCharge(
                anyString(), anyString(), anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(CHARGES_DELETE_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Company Charges DELETE request when service returns generic exception")
    void deleteCompanyChargesGenericRuntimeException() throws Exception {
        // given
        doThrow(RuntimeException.class).when(chargesService).deleteCharge(
                anyString(), anyString(), anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(CHARGES_DELETE_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Company Charges DELETE request when service returns conflict exception")
    void deleteCompanyChargesConflict() throws Exception {
        // given
        doThrow(ConflictException.class).when(chargesService).deleteCharge(
                anyString(), anyString(), anyString(), anyString());

        // when
        ResultActions result = mockMvc.perform(delete(CHARGES_DELETE_URL)
                .contentType(APPLICATION_JSON)
                .header("x-request-id", "5342342")
                .header("ERIC-Identity", "Test-Identity")
                .header("ERIC-Identity-Type", "Key")
                .header("ERIC-Authorised-Key-Privileges", "internal-app")
                .header("X-DELTA-AT", DELTA_AT));

        // then
        result.andExpect(status().isConflict());
    }

    private InternalChargeApi createChargesDocument() throws IOException {
        String incomingData =
                FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                        resourceFile.getInputStream())));
        ObjectMapper chargesDocMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return chargesDocMapper.readValue(incomingData, InternalChargeApi.class);
    }

    public ChargesDocument transform(String companyNumber, String chargeId,
            InternalChargeApi requestBody) {

        String type = "mortgage_delta";

        OffsetDateTime at = requestBody.getInternalData().getDeltaAt();

        String by = requestBody.getInternalData().getUpdatedBy();
        OffsetDateTime deltaAt = requestBody.getInternalData().getDeltaAt();
        final Updated updated =
                new Updated().setAt(at.toLocalDateTime()).setType(type).setBy(by);
        return new ChargesDocument().setId(chargeId)
                .setCompanyNumber(companyNumber).setData(requestBody.getExternalData())
                .setDeltaAt(deltaAt)
                .setUpdated(updated);
    }



    @Test
    void optionsChargesRequestCORS() throws Exception {

        mockMvc.perform(options(CHARGES_GET_URL)
                        .contentType(APPLICATION_JSON)
                        .header("Origin", "")
                )
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .andExpect(header().exists(HttpHeaders.ACCESS_CONTROL_MAX_AGE))
                .andReturn();
    }

    @Test
    void whenCorsRequestWithValidMethod_thenProceed() throws Exception {
        ChargesApi charge = new ChargesApi();
        doReturn(charge).when(chargesService).findCharges(anyString(), any());

        mockMvc.perform(get(CHARGES_GET_URL)
                        .header(ERIC_ALLOWED_ORIGIN, ERIC_ALLOWED_ORIGIN_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, ERIC_IDENTITY_TYPE_VALUE)
                        .header(ORIGIN,ORIGIN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void whenCorsRequestWithInvalidMethod_thenForbidden() throws Exception {
        InternalChargeApi request = createChargesDocument();
        mockMvc.perform(put(CHARGES_PUT_URL)
                        .contentType(APPLICATION_JSON)
                        .header(ERIC_ALLOWED_ORIGIN, ERIC_ALLOWED_ORIGIN_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, ERIC_IDENTITY_TYPE_VALUE)
                        .header(ORIGIN,ORIGIN_VALUE)
                        .content(gson.toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void whenCorsRequestWithMissingAllowedOrigin_thenForbidden() throws Exception {

        ChargesApi charge = new ChargesApi();
        doReturn(charge).when(chargesService).findCharges(anyString(), any());

        mockMvc.perform(get(CHARGES_GET_URL)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, ERIC_IDENTITY_TYPE_VALUE)
                        .header(ORIGIN,ORIGIN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

}
