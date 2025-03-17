package rs.raf.bank_service.domain.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@RequiredArgsConstructor
@SuperBuilder
@AllArgsConstructor
public class Currency {
    @Id
    private String code; // oznaka npr EUR
    private String name;
    private String symbol;
    private String countries;
    private String description;
    private boolean active;

    public Currency(String code) {
        this.code = code;
    }
}
