package pack.userservicekotlin

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UserServiceKotlinApplication

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        runApplication<UserServiceKotlinApplication>(*args)
    }
}
