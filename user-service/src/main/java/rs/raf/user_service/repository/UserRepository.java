package rs.raf.user_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.entity.BaseUser;
import rs.raf.user_service.entity.Employee;

import java.util.Optional;

public interface UserRepository extends JpaRepository<BaseUser, Long> {
    Optional<BaseUser> findByEmail(String email);

    Page<BaseUser> findAll(Pageable pageable);  // Za paginaciju

    // Check if an employee exists with the given username
    boolean existsByUsername(String username);

    // Check if an employee exists with the given email
    boolean existsByEmail(String email);

    Optional<BaseUser> findByJmbg(String jmbg);
}
