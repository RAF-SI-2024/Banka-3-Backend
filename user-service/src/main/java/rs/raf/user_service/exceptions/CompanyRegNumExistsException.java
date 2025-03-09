package rs.raf.user_service.exceptions;


public class CompanyRegNumExistsException extends RuntimeException {
    public CompanyRegNumExistsException(String id) {
        super("Company with registration number  " + id + " already exists.");

    }

}
