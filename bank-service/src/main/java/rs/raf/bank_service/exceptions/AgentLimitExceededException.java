package rs.raf.bank_service.exceptions;

public class AgentLimitExceededException extends RuntimeException {
    public AgentLimitExceededException() {
        super("Can not place order. Limit exceeded.");
    }
}
