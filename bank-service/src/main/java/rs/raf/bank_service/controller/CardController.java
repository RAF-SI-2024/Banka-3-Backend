package rs.raf.bank_service.controller;


import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/card")
@Tag(name = "Card Management", description = "API for managing bank cards")
public class CardController {
}
