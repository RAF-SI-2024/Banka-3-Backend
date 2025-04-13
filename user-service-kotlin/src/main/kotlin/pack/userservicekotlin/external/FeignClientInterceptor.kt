package pack.userservicekotlin.external

import feign.RequestInterceptor
import feign.RequestTemplate
import org.springframework.stereotype.Component

// Ova komponenta sluzi da u sve Feign pozive ubaci token koji nam treba da bi mogli da radimo autorizaciju za komunikaciju izmedju servisa trenutno radi testiranja
@Component
class FeignClientInterceptor : RequestInterceptor {
    override fun apply(template: RequestTemplate) {
        template.header("Authorization", "Bearer $STATIC_TOKEN")
    }

    companion object {
        // Dugotrajni token ovde
        private const val STATIC_TOKEN =
            "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJwZXRhci5wQGV4YW1wbGUuY29tIiwicm9sZSI6IkFETUlOIiwidXNlcklkIjozLCJpYXQiOjE3NDE1MjEwMTEsImV4cCI6MjA1NzA1MzgxMX0.3425U9QrOg04G_bZv8leJNYEOKy7C851P5pWv0k9R3rWpA0ePoeBGpLDd-vKK2qNVgi-Eu2PkfFz41WdUTdFeQ"
    }
}
