Feature: Process company charges error scenarios

  Scenario: Process company charges should return correct response code on repository call failure

    Given Charges Data API component is successfully running
    And   Stubbed CHS Kafka API endpoint will return 200 http response code
    But   MongoDB is not reachable
    When  PUT Rest endpoint is invoked with a valid json payload
    Then  Rest endpoint returns http response code 503 to the client
    And   CHS Kafka API is never invoked


  Scenario: Process company charges should return correct response code for invalid payload which can not be de-serialised

    Given Charges Data API component is successfully running
    And   Stubbed CHS Kafka API endpoint will return 200 http response code
    When  PUT Rest endpoint is invoked with a random invalid payload that fails to de-serialised into Request object
    Then  Rest endpoint returns http response code 400 to the client
    And   CHS Kafka API is never invoked

  Scenario: Process company charges should return correct response code for payload which causes NPE

    Given Charges Data API component is successfully running
    And   Stubbed CHS Kafka API endpoint will return 200 http response code
    When  PUT Rest endpoint is invoked with a valid json payload that causes a NPE
    Then  Rest endpoint returns http response code 500 to the client
    And   CHS Kafka API is never invoked
    And   Data is not updated into Mongo DB

  Scenario: Process company charges with CHS Kafka API endpoint returning 503 response code

    Given Charges Data API component is successfully running
    And   Stubbed CHS Kafka API endpoint will return 503 http response code
    When  PUT Rest endpoint is invoked with a valid json payload
    Then  Rest endpoint returns http response code 503 to the client
    And   Data is updated in Mongo DB

  Scenario Outline: Process company charges upsert returns conflict error for outdated delta at

    Given Charges data api service is running
    And i create a company charges record in DB from file "<input>" for company number "<company_number>" and charge id "<charge_id>"
    And the company charges payload created from file "<input>"
    When I send PUT request for company number "<company_number>" and chargeId "<charge_id>" with outdated delta at
    Then I should receive 409 status code
    And verify the data stored in db matches to "<output>" file

    Examples:
      | input                             | output                             | company_number | charge_id                                                |
      | Insolvency_cases_Happy_Path_input | Insolvency_cases_Happy_Path_output | 08124207       | AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng== |
