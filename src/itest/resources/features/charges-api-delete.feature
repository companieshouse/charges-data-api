Feature: Delete company charges information

  Scenario Outline: Delete company charges information successfully 200

    Given Charges data api service is running
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    And the CHS Kafka API is invoked successfully with delete event payload "<payload>"
    And charge id "<charge_id>" does not exist in mongo db
    Then I should receive 200 status code

    Examples:
      | company_number |   charge_id                                        |  payload |
      | 0       |   12345678910123456789  | chs-kafka-api-12345678910123456789   |


  Scenario Outline: Delete company charges URL returns 404

    Given Charges data api service is running
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id_to_send>"
    Then I should receive 404 status code

    Examples:
      | company_number |   charge_id      | charge_id_to_send |
      | 0       |   12345678910123456789  | 12345             |

  Scenario Outline: Delete company charges URL returns 503 when DB is down

    Given Charges data api service is running
    And Stubbed CHS Kafka API endpoint will return 200 http response code
    And  the company charge exists for charge id "<charge_id>"
    And  the company mortgages database is down
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    Then I should receive 503 status code

    Examples:
      | company_number |   charge_id      |
      | 0       |   12345678910123456789  |


  Scenario Outline: Delete company charges URL returns 503 when chs kafka api is down

    Given Charges data api service is running
    And Stubbed CHS Kafka API endpoint will return 503 http response code
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    Then charge id "<charge_id>" does not exist in mongo db

    Examples:
      | company_number |   charge_id      |
      | 0       |   12345678910123456789  |

  Scenario Outline:  Delete company charges URL returns 404 when company number not found

    Given Charges data api service is running
    And populate invalid company number for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    Then I should receive 404 status code

    Examples:
      | company_number |   charge_id      |
      | null       |   123456789101236666 |

  Scenario Outline:  Delete company charges URL returns 500

    Given Charges data api service is running
    And  the company charge exists for charge id "<charge_id>"
    And Stubbed CHS Kafka API endpoint will return 500 http response code
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    Then I should receive 500 status code

    Examples:
      | company_number |   charge_id    |
      | 0       |   123456789101236666  |

  Scenario Outline: Delete company charges URL returns 301 when chs kafka api is down

    Given Charges data api service is running
    And Stubbed CHS Kafka API endpoint will return 301 http response code
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    Then I should receive 301 status code
    And charge id "<charge_id>" does not exist in mongo db

    Examples:
      | company_number |   charge_id      |
      | 0       |   12345678910123456789  |

  Scenario Outline: Delete company charges URL returns 201 when chs kafka api is down

    Given Charges data api service is running
    And Stubbed CHS Kafka API endpoint will return 201 http response code
    And  the company charge exists for charge id "<charge_id>"
    When I send DELETE request with company number "<company_number>" and charge id "<charge_id>"
    Then I should receive 200 status code
    And charge id "<charge_id>" does not exist in mongo db

    Examples:
      | company_number |   charge_id      |
      | 0       |   12345678910123456789  |