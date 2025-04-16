package pack.userservicekotlin

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.cloud.openfeign.FeignAutoConfiguration

// @OpenAPIDefinition
// @EnableScheduling
@EnableFeignClients
@SpringBootApplication
@ImportAutoConfiguration(FeignAutoConfiguration::class)
class UserServiceKotlinApplication

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        runApplication<UserServiceKotlinApplication>(*args)
    }
}
