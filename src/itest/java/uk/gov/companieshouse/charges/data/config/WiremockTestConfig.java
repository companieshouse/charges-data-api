package uk.gov.companieshouse.charges.data.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import java.util.ArrayList;
import java.util.List;

public class WiremockTestConfig {

    private static final int PORT = 8888;

    private static WireMockServer wireMockServer = null;

    public static void setupWiremock() {
        if (wireMockServer == null) {
            wireMockServer = new WireMockServer(PORT);
            wireMockServer.start();
            configureFor("localhost", PORT);
        } else {
            wireMockServer.resetAll();
        }
    }


    public static void stubKafkaApi(Integer responseCode) {
        stubFor(
                post(urlPathMatching("/private/resource-changed"))
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
        stubFor(
            get(urlPathMatching("/company/70242180/metrics"))
                .willReturn(aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                ));
    }

    public static List<ServeEvent> getServeEvents() {
        return wireMockServer != null ? wireMockServer.getAllServeEvents() :
            new ArrayList<>();
    }
}
