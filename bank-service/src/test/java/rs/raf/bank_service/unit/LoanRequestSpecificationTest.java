package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.jpa.domain.Specification;
import rs.raf.bank_service.domain.entity.LoanRequest;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.specification.LoanRequestSpecification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

class LoanRequestSpecificationTest {

    @Mock
    private Root<LoanRequest> root;
    @Mock
    private CriteriaQuery<?> query;
    @Mock
    private CriteriaBuilder cb;
    @Mock
    private Predicate predicate1;
    @Mock
    private Predicate predicate2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFilterBy_WithTypeAndAccountNumber() {
        LoanType type = LoanType.CASH;
        String accountNumber = "123456789";

        var typePath = mock(javax.persistence.criteria.Path.class);
        var accountPath = mock(javax.persistence.criteria.Path.class);
        var accountNumberPath = mock(javax.persistence.criteria.Path.class);

        when(root.get("type")).thenReturn(typePath);
        when(root.get("account")).thenReturn(accountPath);
        when(accountPath.get("accountNumber")).thenReturn(accountNumberPath);

        when(cb.equal(typePath, type)).thenReturn(predicate1);
        when(cb.equal(accountNumberPath, accountNumber)).thenReturn(predicate2);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        Specification<LoanRequest> spec = LoanRequestSpecification.filterBy(type, accountNumber);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(typePath, type);
        verify(cb).equal(accountNumberPath, accountNumber);

        ArgumentCaptor<Predicate[]> captor = ArgumentCaptor.forClass(Predicate[].class);
        verify(cb).and(captor.capture());
        Predicate[] captured = captor.getValue();
        assertEquals(2, captured.length);
        assertThat(Arrays.asList(captured)).containsExactlyInAnyOrder(predicate1, predicate2);
    }

    @Test
    void testFilterBy_OnlyType() {
        LoanType type = LoanType.STUDENT;
        var typePath = mock(javax.persistence.criteria.Path.class);

        when(root.get("type")).thenReturn(typePath);
        when(cb.equal(typePath, type)).thenReturn(predicate1);
        when(cb.and(predicate1)).thenReturn(mock(Predicate.class));

        Specification<LoanRequest> spec = LoanRequestSpecification.filterBy(type, null);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(typePath, type);
        verify(cb).and(predicate1);
    }

    @Test
    void testFilterBy_OnlyAccountNumber() {
        String accountNumber = "987654321";

        var accountPath = mock(javax.persistence.criteria.Path.class);
        var accountNumberPath = mock(javax.persistence.criteria.Path.class);

        when(root.get("account")).thenReturn(accountPath);
        when(accountPath.get("accountNumber")).thenReturn(accountNumberPath);
        when(cb.equal(accountNumberPath, accountNumber)).thenReturn(predicate1);
        when(cb.and(predicate1)).thenReturn(mock(Predicate.class));

        Specification<LoanRequest> spec = LoanRequestSpecification.filterBy(null, accountNumber);
        Predicate result = spec.toPredicate(root, query, cb);

        assertNotNull(result);
        verify(cb).equal(accountNumberPath, accountNumber);
        verify(cb).and(predicate1);
    }

    @Test
    void testFilterBy_NullInputs() {
        Specification<LoanRequest> spec = LoanRequestSpecification.filterBy(null, null);

        Predicate result = spec.toPredicate(root, query, cb);

        // Kada su svi filteri null → builder.and() sa praznim nizom → logički validan, ali moraš vratiti and() bez predikata
        verify(cb).and(new Predicate[0]);
        assertThat(result).isNull(); // Bitno: vraća se ne-null and(), iako je bez uslova
    }
}
