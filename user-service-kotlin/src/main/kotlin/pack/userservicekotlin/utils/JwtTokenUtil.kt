package pack.userservicekotlin.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.security.Key
import java.time.Instant
import java.util.*

@Component
class JwtTokenUtil {
    private val expiration: Long = 86400000

    fun generateToken(
        email: String?,
        id: Long?,
        role: String?,
    ): String =
        Jwts
            .builder()
            .setSubject(email)
            .claim("role", role)
            .claim("userId", id)
            .setIssuedAt(Date())
            .setExpiration(Date(Instant.now().toEpochMilli() + expiration))
            .signWith(SignatureAlgorithm.HS512, secret)
            .compact()

    fun getSubjectFromToken(token: String?): String = getClaimsFromToken(token).subject

    fun getClaimsFromToken(token: String?): Claims =
        Jwts
            .parserBuilder()
            .setSigningKey(secret)
            .build()
            .parseClaimsJws(token)
            .body

    fun validateToken(token: String?): Boolean {
        try {
            getClaimsFromToken(token)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun getUserIdFromAuthHeader(authHeader: String?): Long {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw SecurityException("Invalid or missing Authorization header")
        }

        val token = authHeader.replace("Bearer ", "").trim { it <= ' ' }

        // Parsiramo token i vadimo userId
        return getClaimsFromToken(token).get("userId", Int::class.java).toLong()
    }

    companion object {
        private val secret: Key =
            Keys.hmacShaKeyFor(
                "si-2024-banka-3-tajni-kljuc-za-jwt-generisanje-tokena-mora-biti-512-bitova-valjda-je-dovoljno".toByteArray(),
            )
    }
}
