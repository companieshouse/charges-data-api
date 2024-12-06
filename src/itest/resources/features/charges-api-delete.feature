Feature: Delete company charges information

  Scenario Outline: Delete company charges information successfully 200

    Given Charges data api service is running
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    And the CHS Kafka API is invoked successfully with delete event payload "<payload>"
    And charge id "<charge_id>" does not exist in mongo db
    Then I should receive 200 status code

    Examples:
      | company_number | charge_id             | payload                            |
      | 08124207       | 12345678910123456789  | chs-kafka-api-12345678910123456789 |


  Scenario Outline: Delete company charges URL return 200 for non existent document

    Given Charges data api service is running
    And  charge id "<charge_id>" does not exist in mongo db
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    And the CHS Kafka API is invoked successfully with delete event payload "<payload>"
    Then I should receive 200 status code

    Examples:
      | company_number | charge_id             | payload                            |
      | 08124207       | 12345678910123456789  | chs-kafka-api-12345678910123456789 |

  Scenario Outline: Delete company charges URL returns 503 when DB is down

    Given Charges data api service is running
    And Stubbed CHS Kafka API endpoint will return 200 http response code
    And  the company charge exists for charge id "<charge_id>"
    And  the company mortgages database is down
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    Then I should receive 503 status code

    Examples:
      | company_number | charge_id            |
      | 12345678       | 12345678910123456789 |


  Scenario Outline: Delete company charges URL returns 503 when chs kafka api is down

    Given Charges data api service is running
    And Stubbed CHS Kafka API endpoint will return 503 http response code
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    Then I should receive 503 status code
    And charge id "<charge_id>" does not exist in mongo db

    Examples:
      | company_number | charge_id            |
      | 12345678       | 12345678910123456789 |


  Scenario Outline: Delete company charges URL returns 400 when delta at is missing

    Given Charges data api service is running
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>", charge id "<charge_id>" and delta_at "<delta_at>"
    Then I should receive 400 status code
    And charge id "<charge_id>" exist in mongo db

    Examples:
      | company_number | charge_id            | delta_at             |
      | 12345678       | 12345678910123456789 |                      |


  Scenario Outline: Delete company charges URL returns 409 when delta is stale

    Given Charges data api service is running
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>", charge id "<charge_id>" and delta_at "<delta_at>"
    Then I should receive 409 status code
    And charge id "<charge_id>" exist in mongo db

    Examples:
      | company_number | charge_id            | delta_at             |
      | 12345678       | 12345678910123456789 | 20231205123045999999 |


