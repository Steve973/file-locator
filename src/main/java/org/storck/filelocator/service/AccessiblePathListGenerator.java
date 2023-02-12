package org.storck.filelocator.service;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Slf4j
@Service
@NoArgsConstructor
public class AccessiblePathListGenerator implements FileVisitor<Path> {

    List<Path> accessiblePaths = new ArrayList<>();

    private final Collection<String> skipPaths = new ArrayList<>();

    public List<Path> generateAccessiblePathsList(final Collection<String> skipPaths) {
        this.skipPaths.clear();
        this.skipPaths.addAll(skipPaths);
        try {
            log.info("Generating list of accessible paths");
            accessiblePaths.clear();
            Files.walkFileTree(Path.of("/"), this);
        } catch (IOException e) {
            log.error("Error encountered when updating path skip list", e);
        }
        accessiblePaths = accessiblePaths.stream()
                .filter(p -> p.toFile().isDirectory())
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        log.info("Accessible paths contains {} items", accessiblePaths.size());
        return accessiblePaths;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
        try {
            if (skipPaths.contains(path.toString())) {
                return SKIP_SUBTREE;
            } else {
                File dir = path.toFile();
                if (dir.canRead()) {
                    accessiblePaths.add(path);
                }
                return CONTINUE;
            }
        } catch (Exception e) {
            return SKIP_SUBTREE;
        }
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException exc) {
        if (path.toFile().isDirectory()) {
            return SKIP_SUBTREE;
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException exc) {
        return CONTINUE;
    }
}
