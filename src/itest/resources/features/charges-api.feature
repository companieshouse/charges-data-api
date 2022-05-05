Feature: Process company charges

  Scenario Outline: Process company charges successfully

    Given Charges data api service is running
    When I send PUT request for company number "08124207" and chargeId "AbRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng==" with payload "<input>"
    Then I should receive 200 status code
    And verify the data stored in db matches to "<output>" file

    Examples:
      | input                                                             | output                                                      |
      | Insolvency_cases_Happy_Path_input                                 | Insolvency_cases_Happy_Path_output                          |
      | Additional_notices_Happy_Path_input                               | Additional_notices_Happy_Path_output                        |
      | Scott_alterations_input                                           | Scott_alterations_output                                    |
      | alterations_to_order_input                                        | alterations_to_order_output                                 |
      | assets_ceased_released_Input                                      | assets_ceased_released_output                               |
      | created_on_input                                                  | created_on_output                                           |
      | floating_charge_input                                             | floating_charge_output                                      |
      | more_than_4_persons_input                                         | more_than_4_persons_output                                  |
      | obligation_secured_&_nature_of_charge_Happy_Path_input            | obligation_secured_&_nature_of_charge_Happy_Path_output     |
      | satisfied_on_Happy_Path_input                                     | satisfied_on_Happy_Path_output                              |


  Scenario Outline: Retrieve charge details successfully

    Given Charges data api service is running
    And the company charges with "<company_number>" and "<charge_id>" exists with data "<data>"
    When I send GET request with company number "<company_number>" and charge Id "<charge_id>"
    Then I should receive 200 status code
    And the Get call response body should match "<result>" file

    Examples:
      | data                              | company_number  |   charge_id                                                       |  result                                    |
      | Insolvency_cases_Happy_Path_input | 08124207    |   OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng          |  Insolvency_cases_Happy_Path_output        |



  Scenario Outline: Retrieve charges successfully

    Given Charges data api service is running
    And the company charges with "<company_number>" and "<charge_id>" exists with data "<data>"
    When I send GET request with company number "<company_number>"
    Then I should receive 200 status code
    And the Get charges call response body should match "<result>" file

    Examples:
      | data                              | company_number  |   charge_id                                                       |  result                                    |
      | Insolvency_cases_Happy_Path_input | 08124207        |   OzRiNTU3NjNjZWI1Y2YxMzkzYWY3MzQ0YzVlOTg4ZGVhZTBkYWI4Ng          |  Insolvency_Company_Metrics_output         |


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
