package rs.raf.stock_service.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

@Entity
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Exchange {
    @Id
    private String mic;
    private String name;
    private String acronym;
    @ManyToOne
    @JoinColumn(name = "country_id", nullable = false)
    private Country polity;
    private String currencyCode;
    private Long timeZone;

    private boolean testMode = false;
}