package pack.userservicekotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class UserServiceKotlinApplication

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        runApplication<UserServiceKotlinApplication>(*args)
    }
}
