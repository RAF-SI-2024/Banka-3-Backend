package rs.raf.stock_service.exceptions;

public class OtcOptionAlreadyExercisedException extends RuntimeException{
    public OtcOptionAlreadyExercisedException() {
        super("Option already exercised.");
    }
}
