package solutions.thonbecker.personal.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class WebClientConfig {
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        // Add message converters
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();

        // JSON converter for application/json responses
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setSupportedMediaTypes(Arrays.asList(MediaType.APPLICATION_JSON));
        messageConverters.add(jsonConverter);

        // String converter for text/plain responses
        org.springframework.http.converter.StringHttpMessageConverter stringConverter =
                new org.springframework.http.converter.StringHttpMessageConverter();
        stringConverter.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_PLAIN));
        messageConverters.add(stringConverter);

        restTemplate.setMessageConverters(messageConverters);

        return restTemplate;
    }
}
