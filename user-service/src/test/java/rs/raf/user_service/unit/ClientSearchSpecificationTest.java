package rs.raf.user_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rs.raf.user_service.domain.entity.Client;
import rs.raf.user_service.specification.ClientSearchSpecification;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;


class ClientSearchSpecificationTest {

    @Mock
    private Root<Client> root;

    @Mock
    private CriteriaBuilder criteriaBuilder;

    @Mock
    private Predicate predicate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testFirstNameContains_withNonEmptyValue() {
        when(criteriaBuilder.lower(any())).thenReturn(mock(javax.persistence.criteria.Expression.class));
        when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);

        Predicate result = ClientSearchSpecification.firstNameContains("Marko").toPredicate(root, null, criteriaBuilder);
        assertNotNull(result);
    }

    @Test
    void testFirstNameContains_withNullValue() {
        Predicate result = ClientSearchSpecification.firstNameContains(null).toPredicate(root, null, criteriaBuilder);
        assertNull(result);
    }

    @Test
    void testLastNameContains_withNonEmptyValue() {
        when(criteriaBuilder.lower(any())).thenReturn(mock(javax.persistence.criteria.Expression.class));
        when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);

        Predicate result = ClientSearchSpecification.lastNameContains("Petrovic").toPredicate(root, null, criteriaBuilder);
        assertNotNull(result);
    }

    @Test
    void testLastNameContains_withEmptyValue() {
        Predicate result = ClientSearchSpecification.lastNameContains("").toPredicate(root, null, criteriaBuilder);
        assertNull(result);
    }

    @Test
    void testEmailContains_withNonEmptyValue() {
        when(criteriaBuilder.lower(any())).thenReturn(mock(javax.persistence.criteria.Expression.class));
        when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);

        Predicate result = ClientSearchSpecification.emailContains("test@mail.com").toPredicate(root, null, criteriaBuilder);
        assertNotNull(result);
    }

    @Test
    void testEmailContains_withNullValue() {
        Predicate result = ClientSearchSpecification.emailContains(null).toPredicate(root, null, criteriaBuilder);
        assertNull(result);
    }
}
