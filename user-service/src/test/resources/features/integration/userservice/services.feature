Feature: Testing the services in user-service

  #ClientService
  Scenario: Adding a new client
    When created a new client with first name "Luka", second name "Basrak", email "test@example.com", adress "Sample Adress 3", phone number "1234567", gender "M", and birthday on "14.07.2001." and jmbg "1111111111111"
    And list all clients
    Then recieve client with email "test@example.com"

  Scenario: Updating a client
    Given created a new client with first name "Luka", second name "Basrak", email "test1@example.com", adress "Sample Adress 3", phone number "1234567", gender "M", and birthday on "14.07.2001." and jmbg "1111111111112"
    When updating the clients phone number with "7654321"
    And searching for that client
    Then recieve that client with email "test1@example.com" and phone number "7654321"

  Scenario: Deleting a client
    Given created a new client with first name "Luka", second name "Basrak", email "test2@example.com", adress "Sample Adress 3", phone number "1234567", gender "M", and birthday on "14.07.2001." and jmbg "1111111111113"
    When deleting that client
    Then the client with the "test2@example.com" email does not exist in the list of all clients

  #commented tests cause a side effect that makes rabbitmq fire a message to emailservice by creating an employee
  #and that causes an infinite loop, will be fixed

  #EmployeeService
  #Scenario: Adding a new employee
  #  When created a new employee with first name "Luka", second name "Basrak", email "employee@example.com", adress "Sample Adress 3", phone number "1234567", gender "M", birthday on "14.07.2001.", username "Username", position "Position" and department "Department 1"
  #  And list all employees
  #  Then recieve employee with email "employee@example.com"

  #Scenario: Activating and changing the phone number of an employee
  #  Given created a new employee with first name "Luka", second name "Basrak", email "employee1@example.com", adress "Sample Adress 3", phone number "1234567", gender "M", birthday on "14.07.2001.", username "Username1", position "Position" and department "Department 1"
  #  When activated employee account
  #  And updated employees phone number to "7654321"
  #  And searching for that employee
  #  Then recieve that employee with email "employee1@example.com", now activated and with phone number "7654321"

  #Scenario: Deleting an employee
  #  Given created a new employee with first name "Luka", second name "Basrak", email "employee2@example.com", adress "Sample Adress 3", phone number "1234567", gender "M", birthday on "14.07.2001.", username "Username2", position "Position" and department "Department 1"
  #  When deleting that employee
  #  Then the employee with the "employee2@example.com" email does not exist in the list of all employees


  #UserService
  #Scenario: Adding a permission
  #  Given created a new employee with first name "Luka", second name "Basrak", email "employee3@example.com", adress "Sample Adress 3", phone number "1234567", gender "M", birthday on "14.07.2001.", username "Username3", position "Position" and department "Department 1"
  #  When added a new permission with id "1"
  #  Then recieve permission with id "1" when listing all permisions of that user

  #Scenario: Removing a permission
  #  Given created a new employee with first name "Luka", second name "Basrak", email "employee4@example.com", adress "Sample Adress 3", phone number "1234567", gender "M", birthday on "14.07.2001.", username "Username4", position "Position" and department "Department 1"
  #  When added a new permission with id "1"
  #  And removed permission with id "1"
  #  Then not recieve permission with id "1" when listing all permisions of that user


  #AuthService
  Scenario: Authenticate a client
    Given a client exists with email "marko.m@example.com"
    When client logs in with email "marko.m@example.com" and password "markomarko"
    Then the client with email "marko.m@example.com" receives a valid JWT token

  #once services are added to docker, more integration tests can be written