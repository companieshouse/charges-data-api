package uk.gov.companieshouse.charges.data.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;

public class WiremockTestConfig {

    private static String port = "8888";

    private static WireMockServer wireMockServer;

    public static void setupWiremock() {
        wireMockServer = new WireMockServer(Integer.parseInt(port));
        start();
        configureFor("localhost", Integer.parseInt(port));
    }

    public static void start() {
        wireMockServer.start();
    }

    public static void stop() {
        wireMockServer.stop();
    }

    public static void restart() {
        stop();
        start();
    }


    public static void stubKafkaApi(Integer responseCode) {
        stubFor(
                post(urlPathMatching("/resource-changed"))
                        .willReturn(aResponse()
                                .withStatus(responseCode)
                                .withHeader("Content-Type", "application/json"))
        );
    }

    public static void stubCompanyMetricsApi() {
        stubFor(
                get(urlPathMatching("/company/08124207/metrics"))
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"etag\":\"0dbf16c34be9d2d10ad374d206f598563bc20eb7\",\"counts\":{\"persons-with-significant-control\":null,\"appointments\":{\"active_directors_count\":null,\"active_secretaries_count\":null,\"active_count\":null,\"resigned_count\":null,\"total_count\":null,\"active_llp_members_count\":null}},\"mortgage\":{\"satisfied_count\":0,\"part_satisfied_count\":0,\"total_count\":14}}")

                        ));
    }


}
