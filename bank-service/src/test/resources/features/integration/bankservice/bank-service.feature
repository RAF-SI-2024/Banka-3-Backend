Feature: Testing the bank service


  Scenario: Full integration flow for account creation, login, card request, and transaction approval
    Given an employee with email "petar.p@example.com" and password "petarpetar" logs in
    When an employee creates a client with email "something@example.com"
    And an employee creates an account for the client with email "something@example.com"
    Then when searching for client with email "something@example.com" it comes up
    And when all accounts are listed for client with email "something@example.com" list is not empty
    When the client sets "dvadesettrecimaj" as password
    And the client with email "something@example.com" and password "dvadesettrecimaj" logs in
    And the client requests a new card
    And the employee confirms card creation request and the card is created
    Then when all cards are listed for account, the list is not empty
    When the client initiates a payment to another bootstrap account
    Then the money has not been sent yet
    When the admin approves the payment
    Then the money is successfully transferred