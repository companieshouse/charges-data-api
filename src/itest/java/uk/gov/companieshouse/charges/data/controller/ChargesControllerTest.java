package uk.gov.companieshouse.charges.data.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import uk.gov.companieshouse.charges.data.service.ChargesService;

@SpringBootTest
@ExtendWith(MockitoExtension.class)

public class ChargesControllerTest {

    @Mock
    ChargesService mockChargesService;

    @InjectMocks
    ChargesController chargesController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(chargesController)
                .build();
    }

    @Test
    @DisplayName("Successfully returns charges")
    public void returnChargesSuccessfully() throws Exception {
        String url = String.format("/company/%s/charges", "companyNumber");
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Successfully returns charge")
    public void returnChargeSuccessfully() throws Exception {
        String url = String.format("/company/%s/charge/%s", "companyNumber", "chargeId");
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }

}
