package uk.gov.companieshouse.charges.data.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.charges.data.service.ChargesService;
import uk.gov.companieshouse.charges.data.tranform.ChargesTransformer;
import uk.gov.companieshouse.logging.Logger;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
public class ChargesControllerTest {

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

        InternalChargeApi request = new InternalChargeApi();
        doNothing().when(chargesService).upsertCharges(eq("02588581"), eq("02588581"), eq("02588581"), isA(InternalChargeApi.class));
        String url = String.format("/company/%s/charge/%s/internal", "02588581", "02588581");
        mockMvc.perform(put(url).contentType(APPLICATION_JSON)
                .header("x-request-id", "02588581")
                .content(gson.toJson(request))).andExpect(status().isOk());
    }

}
