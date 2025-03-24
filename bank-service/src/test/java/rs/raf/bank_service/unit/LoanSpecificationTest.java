package rs.raf.bank_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.AdditionalAnswers;
import org.mockito.MockitoAnnotations;
import rs.raf.bank_service.domain.entity.Loan;
import rs.raf.bank_service.domain.enums.LoanStatus;
import rs.raf.bank_service.domain.enums.LoanType;
import rs.raf.bank_service.specification.LoanSpecification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LoanSpecificationTest {

    @Mock
    private Root<Loan> root;

    @Mock
    private CriteriaBuilder cb;

    @Mock
    private Predicate predicate1;

    @Mock
    private Predicate predicate2;

    @Mock
    private Predicate predicate3;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFilterBy_AllFieldsProvided() {

        var typePath = mock(javax.persistence.criteria.Path.class);
        var accountPath = mock(javax.persistence.criteria.Path.class);
        var accountNumberPath = mock(javax.persistence.criteria.Path.class);
        var statusPath = mock(javax.persistence.criteria.Path.class);


        when(root.get("type")).thenReturn(typePath);
        when(root.get("account")).thenReturn(accountPath);
        when(accountPath.get("accountNumber")).thenReturn(accountNumberPath);
        when(root.get("status")).thenReturn(statusPath);


        when(cb.equal(typePath, LoanType.STUDENT)).thenReturn(predicate1);
        when(cb.equal(accountNumberPath, "ACC123")).thenReturn(predicate2);
        when(cb.equal(statusPath, LoanStatus.APPROVED)).thenReturn(predicate3);


        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        var spec = LoanSpecification.filterBy(LoanType.STUDENT, "ACC123", LoanStatus.APPROVED);
        Predicate result = spec.toPredicate(root, null, cb);

        assertNotNull(result);
        verify(cb).equal(typePath, LoanType.STUDENT);
        verify(cb).equal(accountNumberPath, "ACC123");
        verify(cb).equal(statusPath, LoanStatus.APPROVED);
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void testFilterBy_OnlyTypeProvided() {
        when(cb.equal(root.get("type"), LoanType.CASH)).thenReturn(predicate1);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        var spec = LoanSpecification.filterBy(LoanType.CASH, null, null);
        Predicate result = spec.toPredicate(root, null, cb);

        assertNotNull(result);
        verify(cb).equal(root.get("type"), LoanType.CASH);
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void testFilterBy_OnlyStatusProvided() {
        when(cb.equal(root.get("status"), LoanStatus.REJECTED)).thenReturn(predicate1);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        var spec = LoanSpecification.filterBy(null, null, LoanStatus.REJECTED);
        Predicate result = spec.toPredicate(root, null, cb);

        assertNotNull(result);
        verify(cb).equal(root.get("status"), LoanStatus.REJECTED);
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void testFilterBy_OnlyAccountNumberProvided() {
        String accountNumber = "ACC456";

        var accountPath = mock(javax.persistence.criteria.Path.class);
        var accountNumberPath = mock(javax.persistence.criteria.Path.class);

        when(root.get("account")).thenReturn(accountPath);
        when(accountPath.get("accountNumber")).thenReturn(accountNumberPath);
        when(cb.equal(accountNumberPath, accountNumber)).thenReturn(predicate1);
        when(cb.and(predicate1)).thenReturn(mock(Predicate.class));

        var spec = LoanSpecification.filterBy(null, accountNumber, null);
        Predicate result = spec.toPredicate(root, null, cb);

        assertNotNull(result);
        verify(cb).equal(accountNumberPath, accountNumber);
        verify(cb).and(predicate1);
    }


    @Test
    void testFilterBy_EmptyAccountNumberIgnored() {
        when(cb.equal(root.get("type"), LoanType.AUTO)).thenReturn(predicate1);
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        var spec = LoanSpecification.filterBy(LoanType.AUTO, "", null);
        Predicate result = spec.toPredicate(root, null, cb);

        assertNotNull(result);
        verify(cb).equal(root.get("type"), LoanType.AUTO);
        verify(cb).and(any(Predicate[].class));
    }

    @Test
    void testFilterBy_NullInputs() {
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));

        var spec = LoanSpecification.filterBy(null, null, null);
        Predicate result = spec.toPredicate(root, null, cb);

        assertNotNull(result);
        verify(cb).and(any(Predicate[].class));
    }
}
