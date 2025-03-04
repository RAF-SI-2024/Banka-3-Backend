package rs.raf.bank_service.exceptions;

public class PaymentNotFoundException extends RuntimeException {

  public PaymentNotFoundException(Long id) {
    super("Payment not found with id: " + id);
  }
}
