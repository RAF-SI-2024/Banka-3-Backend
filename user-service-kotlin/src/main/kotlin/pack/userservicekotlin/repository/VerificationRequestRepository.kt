package pack.userservicekotlin.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pack.userservicekotlin.domain.entities.VerificationRequest

@Repository
interface VerificationRequestRepository : JpaRepository<VerificationRequest, Long> {
    @Query(
        """
        SELECT v FROM VerificationRequest v 
        WHERE v.userId = :userId 
          AND v.status = 'PENDING' 
          AND v.expirationTime > CURRENT_TIMESTAMP 
        ORDER BY v.createdAt DESC
    """,
    )
    fun findActiveRequests(
        @Param("userId") userId: Long,
    ): List<VerificationRequest>

    @Query(
        """
        SELECT v FROM VerificationRequest v 
        WHERE v.userId = :userId 
          AND (v.status != 'PENDING' OR v.expirationTime < CURRENT_TIMESTAMP) 
        ORDER BY v.createdAt DESC
    """,
    )
    fun findInactiveRequests(
        @Param("userId") userId: Long,
    ): List<VerificationRequest>

    @Query(
        """
        SELECT v FROM VerificationRequest v 
        WHERE v.id = :id 
          AND v.userId = :userId 
          AND v.status = 'PENDING' 
          AND v.expirationTime > CURRENT_TIMESTAMP
    """,
    )
    fun findActiveRequest(
        @Param("id") id: Long,
        @Param("userId") userId: Long,
    ): VerificationRequest?
}
