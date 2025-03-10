//package rs.raf.user_service.bankClient;
//
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PutMapping;
//
//import org.springframework.cloud.openfeign.FeignClient;
//
//@FeignClient(name = "bank-service", url = "${bank.service.url:http://localhost:8082}")
//public interface BankClient {
//
//    @PutMapping("/api/accounts/{id}/change-limit")
//    void changeAccountLimit(@PathVariable("id") Long id);
//
//}
