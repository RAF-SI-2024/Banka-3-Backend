package pack.userservicekotlin.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.access.hierarchicalroles.RoleHierarchy
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableAsync
@EnableMethodSecurity(prePostEnabled = true)
class SpringSecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
) {
    @Bean
    fun securityFilterChain(http: org.springframework.security.config.annotation.web.builders.HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { }
            sessionManagement {
                sessionCreationPolicy = SessionCreationPolicy.STATELESS
            }
            authorizeHttpRequests {
                authorize("/swagger-ui.html", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/api-docs/**", permitAll)
                authorize("/api/auth/**", permitAll)

                authorize("/api/admin/users/**", hasRole("ADMIN"))
                authorize("/api/admin/employees/me", hasRole("EMPLOYEE"))
                authorize("/api/admin/employees/**", hasRole("ADMIN"))
                authorize("/api/admin/clients/me", hasRole("CLIENT"))
                authorize("/api/admin/clients/**", hasRole("EMPLOYEE"))
                authorize("/api/company/**", hasAnyRole("ADMIN", "EMPLOYEE"))
                authorize("/api/admin/actuaries/**", hasRole("SUPERVISOR"))
                authorize("/api/verification/request", hasRole("ADMIN"))
                authorize("/api/verification/**", authenticated)
                authorize(anyRequest, authenticated)
            }
            headers {
                frameOptions { disable() }
            }
            addFilterBefore<UsernamePasswordAuthenticationFilter>(jwtAuthenticationFilter)
        }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost:4200", "https://banka-3.si.raf.edu.rs/")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun roleHierarchy(): RoleHierarchy {
        val hierarchy = RoleHierarchyImpl()
        hierarchy.setHierarchy(
            """
            ROLE_ADMIN > ROLE_SUPERVISOR
            ROLE_SUPERVISOR > ROLE_AGENT
            ROLE_AGENT > ROLE_EMPLOYEE
            """.trimIndent(),
        )
        return hierarchy
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
}
