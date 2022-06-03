Feature: Process company charges error scenarios

  Scenario: Process company charges should return correct response code on repository call failure

    Given Charges Data API component is successfully running
    And   Stubbed CHS Kafka API endpoint will return 200 http response code
    But   MongoDB is not reachable
    When  PUT Rest endpoint is invoked with a valid json payload but Repository throws an error
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
    And   Data is not updated into Mongo DB
    And   CHS Kafka API is never invoked

  Scenario: Process company charges should return correct response code for payload which causes NPE

    Given Charges Data API component is successfully running
    And   Stubbed CHS Kafka API endpoint will return 503 http response code
    When  PUT Rest endpoint is invoked with a valid json payload
    Then  Rest endpoint returns http response code 503 to the client
    And   MongoDB is successfully updated
