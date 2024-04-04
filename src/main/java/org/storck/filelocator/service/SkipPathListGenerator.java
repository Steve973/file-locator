package org.storck.filelocator.service;

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
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Slf4j
@Service
public class SkipPathListGenerator implements FileVisitor<Path> {

    private final Collection<String> skipPaths;
    private Queue<String> pathsToSkip;

    public SkipPathListGenerator(@Qualifier("skipPaths") final Collection<String> skipPaths) {
        this.skipPaths = skipPaths;
    }

    public List<String> generateSkipPathList() {
        pathsToSkip = new ConcurrentLinkedQueue<>(skipPaths);
        try {
            log.info("Generating list of inaccessible paths to skip");
            Files.walkFileTree(Path.of("/"), this);
        } catch (IOException e) {
            log.error("Error encountered when updating path skip list", e);
        }
        List<String> pts = pathsToSkip.stream()
                .distinct()
                .sorted()
                .toList();
        log.info("Skip paths contains {} items", pts.size());
        pathsToSkip = null;
        return pts;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
        try {
            return skipPaths.contains(path.toString()) ? SKIP_SUBTREE : CONTINUE;
        } catch (Exception e) {
            log.debug("Error pre-visiting '{}' -- adding to skip paths", path.toAbsolutePath());
        }
        if (path.toFile().isDirectory()) {
            pathsToSkip.add(path.toString());
        }
        return SKIP_SUBTREE;
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path path, IOException exc) {
        if (path.toFile().isDirectory()) {
            pathsToSkip.add(path.toString());
            return SKIP_SUBTREE;
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException exc) {
        return CONTINUE;
    }
}
