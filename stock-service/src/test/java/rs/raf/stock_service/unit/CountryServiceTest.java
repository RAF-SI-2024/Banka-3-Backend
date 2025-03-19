package rs.raf.stock_service.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import rs.raf.stock_service.domain.entity.Country;
import rs.raf.stock_service.repository.CountryRepository;
import rs.raf.stock_service.service.CountryService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CountryServiceTest {

    private CountryService countryService;
    private CountryRepository countryRepository;

    @BeforeEach
    void setUp() {
        countryRepository = mock(CountryRepository.class);
        countryService = new CountryService(countryRepository);
    }

    @Test
    void testImportCountries_noCountriesInRepo() throws IOException {
        when(countryRepository.count()).thenReturn(0L);

        when(countryRepository.countByName(any(String.class))).thenReturn(0L);

        BufferedReader mockReader = mock(BufferedReader.class);
        when(mockReader.readLine()).thenReturn("Exchange Name,Exchange Acronym,Exchange Mic Code,Country,Currency,Time Zone,Open Time,Close Time") // header line
                .thenReturn("Nasdaq,NASDAQ,XNAS,USA,USD,America/New_York, 09:30, 16:00") // USA entry
                .thenReturn("Jakarta Futures Exchange (bursa Berjangka Jakarta),BBJ,XBBJ,Indonesia,Indonesian Rupiah,Asia/Jakarta, 09:00, 17:30") // Indonesia entry
                .thenReturn(null);

        Resource resource = mock(ClassPathResource.class);
        InputStream mockInputStream = mock(InputStream.class);
        when(resource.getInputStream()).thenReturn(mockInputStream);

        countryService.importCountries();

        verify(countryRepository, times(93)).save(any(Country.class));
    }

    @Test
    void testImportCountries_noNewCountries() throws IOException {
        when(countryRepository.count()).thenReturn(1L);

        countryService.importCountries();

        verify(countryRepository, never()).save(any(Country.class));
    }

    @Test
    void testImportCountries_shouldHandleUSA() throws IOException {

        when(countryRepository.count()).thenReturn(0L);


        when(countryRepository.countByName("United States")).thenReturn(0L);


        BufferedReader mockReader = mock(BufferedReader.class);
        when(mockReader.readLine()).thenReturn("Exchange Name,Exchange Acronym,Exchange Mic Code,Country,Currency,Time Zone,Open Time,Close Time") // header line
                .thenReturn("Nasdaq,NASDAQ,XNAS,USA,USD,America/New_York, 09:30, 16:00") // USA entry
                .thenReturn(null);


        Resource resource = mock(ClassPathResource.class);
        InputStream mockInputStream = mock(InputStream.class);
        when(resource.getInputStream()).thenReturn(mockInputStream);

        countryService.importCountries();

        verify(countryRepository, times(93)).save(any(Country.class));
    }

    @Test
    void testImportCountries_shouldHandleIndonesia() throws IOException {

        when(countryRepository.count()).thenReturn(0L);

        when(countryRepository.countByName("Indonesia")).thenReturn(0L);

        BufferedReader mockReader = mock(BufferedReader.class);
        when(mockReader.readLine()).thenReturn("Exchange Name,Exchange Acronym,Exchange Mic Code,Country,Currency,Time Zone,Open Time,Close Time") // header line
                .thenReturn("Jakarta Futures Exchange (bursa Berjangka Jakarta),BBJ,XBBJ,Indonesia,Indonesian Rupiah,Asia/Jakarta, 09:00, 17:30") // Indonesia entry
                .thenReturn(null);

        Resource resource = mock(ClassPathResource.class);
        InputStream mockInputStream = mock(InputStream.class);
        when(resource.getInputStream()).thenReturn(mockInputStream);

        countryService.importCountries();

        verify(countryRepository, times(93)).save(any(Country.class));
    }
}
