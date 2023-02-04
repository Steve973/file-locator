package org.storck.filelocator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class FileLocatorApplication {

    public static void main(final String... args) {
        Class<?>[] runner = new Class<?>[]{FileVisitRunner.class};
        System.exit(SpringApplication.exit(SpringApplication.run(runner, args)));
    }
}
