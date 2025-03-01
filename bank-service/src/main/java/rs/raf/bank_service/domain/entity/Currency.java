package rs.raf.bank_service.domain.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;

@Getter
@Setter
@Entity
public class Currency {
    @Id
    private String code; // oznaka npr EUR
    private String name;
    private String symbol;
    private String countries;
    private String description;
    private boolean active;
}