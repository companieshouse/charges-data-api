package uk.gov.companieshouse.charges.data.config;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.companieshouse.charges.data.AbstractIntegrationTest;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext
@ActiveProfiles({"test"})
public class CucumberSpringConfiguration extends AbstractIntegrationTest {

}

