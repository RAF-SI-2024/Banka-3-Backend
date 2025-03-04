package rs.raf.user_service.exceptions;

public class CompanyNotFoundException extends RuntimeException{
    public CompanyNotFoundException(Long id) {
        super("Cannot find company with id: "+id);
    }
}
