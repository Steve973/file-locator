package org.storck.filelocator.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.List;

@Slf4j
@Configuration
@PropertySource(value = {"classpath:application.yaml"}, factory = YamlPropertySourceFactory.class)
public class AppConfig {

    @Bean(name = "collectionName")
    String collectionName() {
        return System.getProperty("user.name");
    }

    @Bean(name = "skipPaths")
    List<String> skipPaths(@Value("${file-locator.skip-paths}") List<String> skipPaths) {
        return skipPaths;
    }
}