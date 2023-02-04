package org.storck.filelocator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.ComponentScan;
import org.storck.filelocator.service.FileSystemTraverser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@ComponentScan(basePackages = "org.storck.filelocator")
public class FileVisitRunner implements CommandLineRunner {

    private final FileSystemTraverser fileSystemTraverser;

    public FileVisitRunner(FileSystemTraverser fileSystemTraverser) {
        this.fileSystemTraverser = fileSystemTraverser;
    }

    @Override
    public void run(String... args) throws IOException {
        Files.walkFileTree(new File("/").toPath(), fileSystemTraverser);
        log.warn("Files visited: {}", fileSystemTraverser.getNumVisited());
    }
}
