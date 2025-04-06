package rs.raf.stock_service;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class StockServiceApplication {

    public static void main(String[] args) {
        if (System.getenv("ALPHAVANTAGE_API_KEY") == null) {
            Dotenv dotenv = Dotenv.load();
            System.setProperty("ALPHAVANTAGE_API_KEY", dotenv.get("ALPHAVANTAGE_API_KEY"));
            System.out.println("Loaded AlphaVantage API Key from ..env: " + dotenv.get("ALPHAVANTAGE_API_KEY"));

        } else {
            System.out.println("Loaded AlphaVantage API Key from ..env (docker): " + System.getenv("ALPHAVANTAGE_API_KEY"));
        }
        SpringApplication.run(StockServiceApplication.class, args);
    }

}
