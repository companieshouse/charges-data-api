package uk.gov.companieshouse.charges.data.steps;

import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.lessThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.bson.Document;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import uk.gov.companieshouse.api.charges.ChargeApi;
import uk.gov.companieshouse.api.charges.ChargesApi;
import uk.gov.companieshouse.api.charges.InternalChargeApi;
import uk.gov.companieshouse.api.chskafka.ChangedResource;
import uk.gov.companieshouse.charges.data.CucumberFeaturesRunnerITest;
import uk.gov.companieshouse.charges.data.config.CucumberContext;
import uk.gov.companieshouse.charges.data.config.WiremockTestConfig;
import uk.gov.companieshouse.charges.data.model.ChargesDocument;
import uk.gov.companieshouse.charges.data.repository.ChargesRepository;

public class ChargesApiSteps {

    @Autowired
    private ObjectMapper mongoCustomConversions;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ChargesRepository chargesRepository;

    String companyNumber = "08124207";
    String chargeId = "AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng==";
    String insolvency_cases_happy_path_input = "Insolvency_cases_Happy_Path_input";
    String invalid_payload = "Invalid_payload";
    String x_request_value = "5234234234";
    String x_request_id = "x-request-id";

    static InternalChargeApi companyCharge;

    @Before
    public static void before_each() {
        WiremockTestConfig.setupWiremock();
        //CucumberFeaturesRunnerITest.start();
        companyCharge = null;
    }

    @After
    public static void after_each() {
        CucumberFeaturesRunnerITest.stop();
    }

    @Given("Charges data api service is running")
    public void theApplicationRunning() {
        assertThat(restTemplate).isNotNull();
        WiremockTestConfig.stubKafkaApi(HttpStatus.OK.value());
        WiremockTestConfig.stubCompanyMetricsApi();
    }

    private void readCompanyChargeFile(String fileName) {
        File file = new FileSystemResource("src/itest/resources/payload/input/" + fileName + ".json").getFile();
        try {
            companyCharge = mongoCustomConversions.readValue(file, InternalChargeApi.class);
        } catch (IOException e) {
            fail("Failed to read Company Charge File '" + fileName +"'");
        }
    }

    @Given("the company charges payload created from file {string}")
    public void the_company_charges_with_and_exists_with_data(String fileName) {
        readCompanyChargeFile(fileName);
    }

    @Given("i create a company charges record in DB from file {string} for company number {string} and charge id {string}")
    public void addChargesToDBFromFile(String fileName, String companyNumber, String chargeId) {
        InternalChargeApi companyCharge = null;
        File file = new FileSystemResource(
            "src/itest/resources/payload/input/" + fileName + ".json").getFile();
        try {
            companyCharge = mongoCustomConversions.readValue(file, InternalChargeApi.class);
        } catch (IOException e) {
            fail("Failed to read Company Charge File '" + fileName +"'");
        }
        String uri = "/company/{company_number}/charge/{charge_id}/internal";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, getSecurityForRequest(companyCharge),
            Void.class, companyNumber, chargeId);
    }

    @When("I send PUT request for company number {string} and chargeId {string} with payload")
    public void i_send_put_request_for_company_number_and_charge_id_with_payload(String inCompanyNumber, String inChargeId) {
        HttpEntity request = getSecurityForRequest(companyCharge);
        String uri = "/company/{company_number}/charge/{charge_id}/internal";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, inCompanyNumber, inChargeId);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    private HttpEntity getSecurityForRequest(Object body){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(x_request_id, x_request_value);

        headers.set("ERIC-Identity" , "SOME_IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Privileges", "internal-app");
        return new HttpEntity(body, headers);
    }

    private HttpEntity getNullRequest(){
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(x_request_id, x_request_value);

        return new HttpEntity(null, headers);
    }

    @When("I send GET request with company number {string} and charge Id {string}")
    public void i_send_get_request_with_parameters(String companyNumber, String chargeId) {
        String uri = "/company/{company_number}/charges/{charge_id}";
        ResponseEntity<ChargeApi> response = restTemplate.exchange(uri, HttpMethod.GET, getSecurityForRequest(null), ChargeApi.class, companyNumber, chargeId);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
        CucumberContext.CONTEXT.set("getChargeDetailsResponseBody", response.getBody());
    }

    @When("I send GET request with company number {string} and charge Id {string} without security headers")
    public void i_send_get_request_with_parameters_without_security_headers(String companyNumber, String chargeId) {
        String uri = "/company/{company_number}/charges/{charge_id}";

        RestClientException exception = assertThrows(RestClientException.class,
            () -> restTemplate.exchange(uri, HttpMethod.GET, getNullRequest(), ChargeApi.class,
                companyNumber, chargeId));
        String exceptionMessage = exception.getMessage();
        CucumberContext.CONTEXT.set("exceptionMessageText", exceptionMessage);
    }

    @When("I send GET request with company number {string} without security headers")
    public void i_send_get_request_with_company_number_without_security_headers(String companyNumber) {
        String uri = "/company/{company_number}/charges";

        RestClientException exception = assertThrows(RestClientException.class,
            () -> restTemplate.exchange(uri, HttpMethod.GET, getNullRequest(), ChargeApi.class,
                companyNumber, chargeId));
        String exceptionMessage = exception.getMessage();
        CucumberContext.CONTEXT.set("exceptionMessageText", exceptionMessage);
    }

    @When("I send GET request with company number {string}")
    public void i_send_get_request_with_parameters(String companyNumber) {
        String uri = "/company/{company_number}/charges";
        ResponseEntity<ChargesApi> response = restTemplate.exchange(uri, HttpMethod.GET, getSecurityForRequest(null), ChargesApi.class, companyNumber);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
        CucumberContext.CONTEXT.set("getChargesResponseBody", response.getBody());
    }

    @Then("verify the data stored in db matches to {string} file")
    public void the_expected_result_should_match(String fileName) throws IOException {
        FileSystemResource file = new FileSystemResource("src/itest/resources/payload/output/" + fileName + ".json");

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
        verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/private/resource-changed")));
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

    @Then("the Get charges call response body should match {string} file for {string}")
    public void the_get_charges_call_response_body_should_match(String dataFile, String companyNumber) throws IOException {
        FileSystemResource file = new FileSystemResource("src/itest/resources/payload/output/"+dataFile+".json");
        ChargesApi expectedDocument =
                mongoCustomConversions.readValue(file.getFile(), ChargesApi.class);
        ChargesApi actual = CucumberContext.CONTEXT.get("getChargesResponseBody");
        assertThat(actual.getTotalCount()).isEqualTo(expectedDocument.getTotalCount());
        assertThat(actual.getUnfilteredCount()).isEqualTo(expectedDocument.getUnfilteredCount());
        assertThat(actual.getPartSatisfiedCount()).isEqualTo(expectedDocument.getPartSatisfiedCount());
        assertThat(actual.getEtag()).isEqualTo(expectedDocument.getEtag());
        verify(moreThanOrExactly(1), getRequestedFor(urlEqualTo(String.format("/company/%s/metrics", companyNumber))));
    }

    @Given("Charges Data API component is successfully running")
    public void charges_data_api_component_is_successfully_running() {
        assertThat(restTemplate).isNotNull();
    }

    @Given("Stubbed CHS Kafka API endpoint will return {int} http response code")
    public void stubbed_chs_kafka_api_endpoint_will_return_http_response_code(Integer responseCode) {
        WiremockTestConfig.stubKafkaApi(responseCode);
    }
    @Given("MongoDB is not reachable")
    public void mongo_db_is_not_reachable() {
        CucumberFeaturesRunnerITest.stop();
    }

    @When("PUT Rest endpoint is invoked with a valid json payload but Repository throws an error")
    public void put_rest_endpoint_is_invoked_with_a_valid_json_payload_but_repository_throws_an_error() {
        readCompanyChargeFile(insolvency_cases_happy_path_input);
        i_send_put_request_for_company_number_and_charge_id_with_payload(companyNumber,
                chargeId);
    }

    @Then("Rest endpoint returns http response code {int} to the client")
    public void rest_endpoint_returns_http_response_code_to_the_client(Integer expectedResponseCode) {
        var actualStatusCode = CucumberContext.CONTEXT.get("statusCode");
        assertThat(actualStatusCode).isEqualTo(expectedResponseCode);
    }

    @Then("CHS Kafka API is never invoked")
    public void chs_kafka_api_is_never_invoked() {
        verify(lessThanOrExactly(0), postRequestedFor(urlEqualTo("/private/resource-changed")));
    }

    @When("PUT Rest endpoint is invoked with a random invalid payload that fails to de-serialised into Request object")
    public void put_rest_endpoint_is_invoked_with_a_random_invalid_payload_that_fails_to_de_serialised_into_request_object() {
        HttpEntity request = getSecurityForRequest(chargeId);
        String uri = "/company/{company_number}/charge/{charge_id}/internal";
        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, request, Void.class, companyNumber, chargeId);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @When("PUT Rest endpoint is invoked with a valid json payload that causes a NPE")
    public void put_rest_endpoint_is_invoked_with_a_valid_json_payload_that_causes_a_npe() {
        readCompanyChargeFile(invalid_payload);
        i_send_put_request_for_company_number_and_charge_id_with_payload(companyNumber,
                chargeId);
    }

    @When("PUT Rest endpoint is invoked with a valid json payload")
    public void put_rest_endpoint_is_invoked_with_a_valid_json_payload() {
        readCompanyChargeFile(insolvency_cases_happy_path_input);
        i_send_put_request_for_company_number_and_charge_id_with_payload(companyNumber,
                chargeId);
    }

    @Then("MongoDB is successfully updated")
    public void mongo_db_is_successfully_updated() throws IOException {
       the_expected_result_should_match("Insolvency_cases_Happy_Path_output");
    }

    @Then("Data is not updated into Mongo DB")
    public void data_is_not_updated_into_mongo_db() {
        List<ChargesDocument> chargesDocuments = chargesRepository.findAll();
        Assertions.assertThat(chargesDocuments).hasSize(0);
    }

    @Then("Data is updated in Mongo DB")
    public void data_is_updated_in_mongo_db() {
        List<ChargesDocument> chargesDocuments = chargesRepository.findAll();
        Assertions.assertThat(chargesDocuments).hasSize(1);
    }


    private Document readData(Resource resource) throws IOException {
        return Document.parse(IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8));
    }

    @Then("an exception is thrown with message text containing {string}")
    public void anExceptionIsThrownDueToStatusCode(String exceptionCode) {
        String exceptionMessage = CucumberContext.CONTEXT.get("exceptionMessageText");
        assertTrue(exceptionMessage.contains(exceptionCode));
    }

    @When("I send PUT request with company number {string} and charge Id {string} without security headers")
    public void iSendPUTRequestWithCompanyNumberAndChargeIdWithoutSecurityHeaders(String companyNumber,
        String chargeId) {
        String uri = "/company/{company_number}/charge/{charge_id}/internal";

        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.PUT, getNullRequest(),
            Void.class, companyNumber, chargeId);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }


    @When("I send DELETE request with company number {string} and charge Id {string} without security headers")
    public void iSendDELETERequestWithCompanyNumberAndChargeIdWithoutSecurityHeaders(String companyNumber,
        String chargeId) {
        String uri = "/company/{company_number}/charges/{charge_id}";

        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.DELETE, getNullRequest(),
            Void.class, companyNumber, chargeId);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @Given("the company charge exists for charge id {string}")
    public void the_company_charge_exists_for_charge_id(String id) throws IOException {

        FileSystemResource file = new FileSystemResource("src/itest/resources/payload/input/" + id + ".json");
        Document document = readData(file);
        ChargesDocument chargesDocument = mongoCustomConversions.convertValue(document, ChargesDocument.class);
        chargesDocument.setId(id); chargesDocument.setCompanyNumber(companyNumber);
        chargesRepository.save(chargesDocument);
        assertNotNull(chargesRepository.findById(id));
    }

    @When("I send DELETE request with company number {string} and charge id {string}")
    public void i_send_delete_request_with_company_number(String companyNumber, String chargeId) {
        String uri = "/company/{company_number}/charges/{charge_id}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set(x_request_id, x_request_value);
        headers.set("ERIC-Identity" , "SOME_IDENTITY");
        headers.set("ERIC-Identity-Type", "key");
        headers.set("ERIC-Authorised-Key-Privileges", "internal-app");
        var request = new HttpEntity<>(null, headers);

        ResponseEntity<Void> response = restTemplate.exchange(uri, HttpMethod.DELETE, request, Void.class, companyNumber, chargeId);

        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
    }

    @Then("the CHS Kafka API is invoked successfully with delete event payload {string}")
    public void chs_kafka_api_invoked(String payload) throws IOException {

        File file = new FileSystemResource("src/itest/resources/payload/input/" + payload + ".json").getFile();
        List<ServeEvent> serverEvents = WiremockTestConfig.getServeEvents();
        assertThat(serverEvents.size()).isEqualTo(1);
        assertThat(serverEvents.isEmpty()).isFalse();
        String requestReceived = serverEvents.get(0).getRequest().getBodyAsString();

        ChangedResource expectedChangedResource = mongoCustomConversions.readValue(file, ChangedResource.class);
        ChangedResource actualChangedResource = mongoCustomConversions.convertValue(Document.parse(requestReceived), ChangedResource.class);

        assertEquals( expectedChangedResource.getEvent().getType(), actualChangedResource.getEvent().getType());
        assertEquals( expectedChangedResource.getResourceUri(), actualChangedResource.getResourceUri());
        assertEquals(expectedChangedResource.getResourceKind(), actualChangedResource.getResourceKind());

    }

    @When("I send GET request with company number {string} and charge id {string}")
    public void i_send_get_request_with_company_number_and_charge_id(String companyNumber, String chargeId) {
        String uri = "/company/{company_number}/charges/{charge_id}";
        ResponseEntity<ChargesApi> response = restTemplate.exchange(uri, HttpMethod.GET, getSecurityForRequest(null), ChargesApi.class, companyNumber, chargeId);
        CucumberContext.CONTEXT.set("statusCode", response.getStatusCodeValue());
        CucumberContext.CONTEXT.set("getChargesResponseBody", response.getBody());
    }

    @Then("charge id {string} does not exist in mongo db")
    public void charge_id_does_not_exist_in_mongo_db(String chargeId) throws IOException {
        assertFalse(chargesRepository.findById(chargeId).isPresent());
    }

    @Given("the company mortgages database is down")
    public void the_company_mortgages_db_is_down() {
        CucumberFeaturesRunnerITest.mongoDBContainer.stop();
    }

    @Then("charge id {string} exist in mongo db")
    public void charge_id_exist_in_mongo_db(String chargeId) throws IOException {
        Assert.assertTrue(chargesRepository.findById(chargeId).isPresent());
    }

    @Given("populate invalid company number for charge id {string}")
    public void populate_invalid_company_number_for_charge_id(String chargeId) throws IOException {

        FileSystemResource file = new FileSystemResource("src/itest/resources/payload/input/" + chargeId + ".json");
        Document document = readData(file);
        ChargesDocument chargesDocument = mongoCustomConversions.convertValue(document, ChargesDocument.class);
        chargesDocument.setId(chargeId); chargesDocument.setCompanyNumber(null);
        chargesRepository.save(chargesDocument);
    }

}
