package rs.raf.stock_service.service;

import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import rs.raf.stock_service.domain.entity.Country;
import rs.raf.stock_service.repository.CountryRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalTime;

@Service
@AllArgsConstructor
public class CountryService {

    private CountryRepository countryRepository;

    public void importCountries(){
        BufferedReader bufferedReader;
        String line;
        if (countryRepository.count()==0){
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new ClassPathResource("exchanges.csv").getInputStream()));
                line = bufferedReader.readLine();

                while ((line = bufferedReader.readLine()) != null) {
                    Country country = new Country();
                    String[] attributes = line.split(",");

                    if (attributes[3].equalsIgnoreCase("usa")){
                        country.setName("United States");
                    }else {
                        country.setName(attributes[3]);
                    }
                    country.setOpenTime(LocalTime.parse(attributes[6].trim()));
                    country.setCloseTime(LocalTime.parse(attributes[7].trim()));
                    if (countryRepository.countByName(country.getName())==0) {
                        countryRepository.save(country);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
