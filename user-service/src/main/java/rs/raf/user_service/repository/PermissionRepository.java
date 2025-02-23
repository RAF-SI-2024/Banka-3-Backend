package rs.raf.user_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rs.raf.user_service.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
}
