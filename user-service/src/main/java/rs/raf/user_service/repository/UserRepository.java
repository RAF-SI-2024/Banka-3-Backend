package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.entity.BaseUser;

public interface UserRepository extends JpaRepository<BaseUser, Long> {
}
