Feature: Sending an email through email service

  Scenario: User receives a reset password email
    Given a reset password email request with token "test-token" and destination "test@example.com"
    When the email is sent
    Then the subject of the email should be "Reset Your Password"
    And the recipient of the email should be "test@example.com"
    And the email content should contain "Reset your password"
    And the email content should contain token "test-token"

  Scenario: User receives a set password email
    Given a set password email request with token "test-token" and destination "test@example.com"
    When the email is sent
    Then the subject of the email should be "Set Your Password"
    And the recipient of the email should be "test@example.com"
    And the email content should contain "Set your password"
    And the email content should contain token "test-token"

  Scenario: User receives an activate account email
    Given an activate account email request with token "test-token" and destination "test@example.com"
    When the email is sent
    Then the subject of the email should be "Activate Your Account"
    And the recipient of the email should be "test@example.com"
    And the email content should contain "Activate Your Account"
    And the email content should contain token "test-token"