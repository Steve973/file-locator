package org.storck.filelocator.service;

import com.arangodb.springframework.core.ArangoOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.storck.filelocator.model.FileEntry;
import org.storck.filelocator.repository.FileEntryRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Slf4j
@Service
public class FileSystemTraverser implements FileVisitor<Path> {

    private int numVisited = 0;

    private final Collection<String> skipPaths;

    private final FileEntryRepository fileEntryRepository;

    protected FileSystemTraverser(final FileEntryRepository fileEntryRepository,
                                  final ArangoOperations arangoOperations,
                                  @Qualifier("skipPaths") final Collection<String> skipPaths,
                                  @Qualifier("collectionName") final String collectionName) {
        this.fileEntryRepository = fileEntryRepository;
        this.skipPaths = skipPaths;
        arangoOperations.collection(collectionName).drop();
    }

    public void updateFileDatabase() {
        CompletableFuture.runAsync(() -> {
            try {
                Files.walkFileTree(new File("/").toPath(), this);
            } catch (IOException e) {
                log.error("Error encountered when updating file database", e);
            }
        });
    }

    public int getNumVisited() {
        return numVisited;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        return (!dir.toFile().canRead() || skipPaths.stream().anyMatch(sp -> dir.toFile().getAbsolutePath().startsWith(sp))) ?
                SKIP_SUBTREE : CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
        numVisited++;
        if (file == null) {
            log.warn("Skipping null file");
        } else if (attrs == null) {
            visitFileFailed(file, new IOException("Could not read file attributes"));
        } else {
            if (file.toFile().canRead()) {
                FileEntry fileEntry = FileEntry.builder()
                        .name(file.getFileName().toString())
                        .path(file.getParent().toString())
                        .isRegularFile(attrs.isRegularFile())
                        .isDirectory(attrs.isDirectory())
                        .isOther(attrs.isOther())
                        .creationTime(attrs.creationTime().toMillis())
                        .lastAccessTime(attrs.lastAccessTime().toMillis())
                        .lastModifiedTime(attrs.lastModifiedTime().toMillis())
                        .size(attrs.size())
                        .build();
                fileEntryRepository.save(fileEntry);
            }
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        numVisited++;
        log.warn("File visit failed: {}", file.toFile().getAbsolutePath());
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if (exc != null) {
            log.warn("Exception when visiting directory: {}", exc.getLocalizedMessage());
        }
        return CONTINUE;
    }
}
