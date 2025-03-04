package rs.raf.bank_service.exceptions;

public class ClientNotAccountOwnerException extends RuntimeException{
    public ClientNotAccountOwnerException() {
        super("Client sending request is not the account owner.");
    }
}
