package pack.userservicekotlin.configuration

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import pack.userservicekotlin.utils.JwtTokenUtil
import java.io.IOException
import java.util.*
import java.util.List
import javax.servlet.ServletException

/*
       Class is called on every Http request/response to check validity of token.
       It is set in the security config as before filter for every call.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenUtil: JwtTokenUtil,
) : OncePerRequestFilter() {
    @Throws(ServletException::class, IOException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = getJwtFromRequest(request)

        if (token != null && jwtTokenUtil.validateToken(token)) {
            val claims = jwtTokenUtil.getClaimsFromToken(token)
            val email = claims.subject

            val role = claims.get("role", String::class.java)
            val authority: GrantedAuthority = SimpleGrantedAuthority("ROLE_" + role.uppercase(Locale.getDefault()))
            val authorities = List.of(authority)

            val userDetails: UserDetails = User(email, "", authorities)

            val authentication =
                UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    authorities,
                )
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun getJwtFromRequest(request: HttpServletRequest): String? {
        val bearerToken = request.getHeader("Authorization")
        println(bearerToken)
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7)
        }
        return null
    }
}
