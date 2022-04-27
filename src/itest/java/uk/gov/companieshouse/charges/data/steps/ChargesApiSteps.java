package uk.gov.companieshouse.charges.data.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static com.github.tomakehurst.wiremock.common.Metadata.metadata;
import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.FileCopyUtils;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;

import uk.gov.companieshouse.charges.data.config.CucumberContext;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;

public class ChargesApiSteps {

    private static String port = "8888";

    private String companyNumber;
    private String chargeId;

    @Autowired
    private ObjectMapper mongoCustomConversions;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ChargesRepository chargesRepository;

    private ResponseEntity<String> lastResponse;

    private static WireMockServer wireMockServer;


    @BeforeAll
    public static void before_all() {
        setupWiremock();
        stubChargeDataApi();
    }

    @AfterAll
    public static void after_all() {
        wireMockServer.stop();
    }

    @Given("Charges data api service is running")
    public void theApplicationRunning() {
        assertThat(restTemplate).isNotNull();
        stubChargeDataApi();
        stubCompanyMetricsApi();
        lastResponse = null;
    }

    @Given("the company charges with {string} and {string} exists with data {string}")
    public void the_company_charges_with_and_exists_with_data(String inCompanyNumber, String inChargeId, String dataFile) throws IOException {
        this.i_send_put_request_for_company_number_and_charge_id_with_payload(inCompanyNumber, inChargeId, dataFile);
    }

    @When("I send PUT request for company number {string} and chargeId {string} with payload {string}")
    public void i_send_put_request_for_company_number_and_charge_id_with_payload(String inCompanyNumber, String inChargeId, String fileName) throws IOException {
        File file = new FileSystemResource("src/itest/resources/payload/input/" + fileName + ".json").getFile();
        InternalChargeApi
                companyCharge = mongoCustomConversions.readValue(file, InternalChargeApi.class);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-request-id", "5234234234");

        HttpEntity request = new HttpEntity(companyCharge, headers);
        String uri = "/company/{company_number}/charge/{charge_id}/internal";
        this.companyNumber = inCompanyNumber;
        this.chargeId = inChargeId;
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber, chargeId);

        this.companyNumber = companyNumber;
        this.chargeId = chargeId;
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @When("I send GET request with company number {string} and charge Id {string}")
    public void i_send_get_request_with_parameters(String companyNumber, String chargeId) {
        String uri = "/company/{company_number}/charges/{charge_id}";
        ResponseEntity<ChargeApi> response = restTemplate.exchange(uri, HttpMethod.GET, null, ChargeApi.class, companyNumber, chargeId);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
        CucumberContext.CONTEXT.set("getChargeDetailsResponseBody", response.getBody());
    }

    @When("I send GET request with company number {string}")
    public void i_send_get_request_with_parameters(String companyNumber) {
        String uri = "/company/{company_number}/charges";
        ResponseEntity<ChargesApi> response = restTemplate.exchange(uri, HttpMethod.GET, null, ChargesApi.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
        CucumberContext.CONTEXT.set("getChargesResponseBody", response.getBody());
    }

    @Then("verify the data stored in db matches to {string} file")
    public void the_expected_result_should_match(String string) throws IOException {
        FileSystemResource file = new FileSystemResource("src/itest/resources/payload/output/" + string + ".json");

        List<ChargesDocument> chargesDocuments = chargesRepository.findAll();

        Assertions.assertThat(chargesDocuments).hasSize(1);

        ChargesDocument actual = chargesDocuments.get(0);

        assertThat(actual).isNotNull();

        Document document = readData(file);
        ChargesDocument expected =
                mongoCustomConversions.convertValue(document, ChargesDocument.class);

        expected.getData().setEtag(actual.getData().getEtag());

        assertThat(actual.getId()).isEqualTo(chargeId);
        assertThat(actual.getCompanyNumber()).isEqualTo(companyNumber);
        assertThat(actual.getData()).isEqualTo(expected.getData());
        verify(1, postRequestedFor(urlEqualTo("/resource-changed")));
    }

    @Then("I should receive {int} status code")
    public void i_should_receive_status_code(Integer statusCode) {
        int expectedStatusCode = CucumberContext.CONTEXT.get("statusCode");
        Assertions.assertThat(expectedStatusCode).isEqualTo(statusCode);
    }

    @Then("the Get call response body should match {string} file")
    public void the_get_call_response_body_should_match(String dataFile) throws IOException {
        FileSystemResource file = new FileSystemResource("src/itest/resources/payload/output/"+dataFile+".json");
        Document document = readData(file);
        ChargesDocument expectedDocument =
                mongoCustomConversions.convertValue(document, ChargesDocument.class);

        ChargeApi expected = expectedDocument.getData();
        ChargeApi actual = CucumberContext.CONTEXT.get("getChargeDetailsResponseBody");
        expected.setEtag(actual.getEtag());
        assertThat(actual).isEqualTo(expected);
    }

    @Then("the Get charges call response body should match {string} file")
    public void the_get_charges_call_response_body_should_match(String dataFile) throws IOException {
       /* FileSystemResource file = new FileSystemResource("src/itest/resources/payload/output/"+dataFile+".json");
        Document document = readData(file);
        ChargesDocument expectedDocument =
                mongoCustomConversions.convertValue(document, ChargesDocument.class);

        ChargesApi expected = expectedDocument.getData();
        ChargesApi actual = CucumberContext.CONTEXT.get("getChargesResponseBody");
        assertThat(actual).isEqualTo(expected);*/
    }

    private Document readData(Resource resource) throws IOException {
        var data= FileCopyUtils.copyToString(new InputStreamReader(Objects.requireNonNull(
                resource.getInputStream())));
        Document document = Document.parse(data);

        return document;
    }

    private static void setupWiremock() {
        wireMockServer = new WireMockServer(Integer.parseInt(port));
        wireMockServer.start();
        configureFor("localhost", Integer.parseInt(port));
    }

    private static void stubChargeDataApi() {
            stubFor(
                post(urlPathMatching("/resource-changed"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")));
    }

    private void stubCompanyMetricsApi() {
        stubFor(
                get(urlPathMatching("^/company/([A-Za-z0-9]{8})/metrics$"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                //.withHeader("Content-Type", "application/json")
                                //.withBody("{\"etag\":\"0dbf16c34be9d2d10ad374d206f598563bc20eb7\",\"counts\":{\"persons-with-significant-control\":null,\"appointments\":{\"active_directors_count\":null,\"active_secretaries_count\":null,\"active_count\":null,\"resigned_count\":null,\"total_count\":null,\"active_llp_members_count\":null}},\"mortgage\":{\"satisfied_count\":0,\"part_satisfied_count\":0,\"total_count\":14}}")

                        ) .withMetadata(metadata()
                                .list("tags", "getMetrics")));
    }

}
