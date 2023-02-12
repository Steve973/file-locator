package org.storck.filelocator.service;

import com.arangodb.springframework.core.ArangoOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.storck.filelocator.model.*;
import org.storck.filelocator.repository.FileEntryRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Slf4j
@Service
public class FileSystemTraverser implements FileVisitor<Path> {

    public static final String ROOT_PARENT = "<N/A>";

    int count = 0;

    private final Collection<String> skipPaths;

    private final String collectionName;

    private final FileEntryRepository fileEntryRepository;

    private final ArangoOperations arangoOperations;

    protected FileSystemTraverser(final FileEntryRepository fileEntryRepository,
                                  final ArangoOperations arangoOperations,
                                  @Qualifier("skipPaths") final Collection<String> skipPaths,
                                  @Qualifier("collectionName") final String collectionName) {
        this.fileEntryRepository = fileEntryRepository;
        this.arangoOperations = arangoOperations;
        this.skipPaths = skipPaths;
        this.collectionName = collectionName;
    }

    public String updateFileDatabase() {
        long start = System.currentTimeMillis();
        arangoOperations.collection(collectionName).drop();
        try {
            Files.walkFileTree(new File("/").toPath(), this);
        } catch (IOException e) {
            log.error("Error encountered when updating file database", e);
        }
        long duration = (System.currentTimeMillis() - start) / 1000;
        return String.format("Count: %d, time: %s seconds", count, duration);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
        try {
            if (skipPaths.parallelStream().anyMatch(dir::startsWith) || !dir.toFile().canRead()) {
                return SKIP_SUBTREE;
            } else {
                return visitFile(dir, attrs);
            }
        } catch (Exception e) {
            return SKIP_SUBTREE;
        }
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        if (path == null || attrs == null) {
            return CONTINUE;
        }
        try {
            File file = path.toFile();
            if (file.canRead()) {
                FileEntry fileEntry = null;
                FileEntry.FileEntryBuilder<?, ?> fileEntryBuilder = null;
                if (attrs.isDirectory()) {
                    fileEntryBuilder = DirectoryNode.builder();
                } else if (attrs.isSymbolicLink()) {
                    fileEntryBuilder = LinkNode.builder();
                } else if (attrs.isRegularFile()) {
                    fileEntryBuilder = FileNode.builder();
                } else if (attrs.isOther()) {
                    fileEntryBuilder = OtherNode.builder();
                }
                if (fileEntryBuilder != null) {
                    File parent = file.getParentFile();
                    String parentPath = parent != null ? parent.getAbsolutePath() : ROOT_PARENT;
                    String name = file.getName();
                    fileEntry = fileEntryBuilder.name(parent == null && !StringUtils.hasText(name) ? "/": name)
                            .path(parentPath)
                            .creationTime(attrs.creationTime().toMillis())
                            .lastAccessTime(attrs.lastAccessTime().toMillis())
                            .lastModifiedTime(attrs.lastModifiedTime().toMillis())
                            .size(attrs.size())
                            .build();
                }
                if (fileEntry != null) {
                    fileEntryRepository.save(fileEntry);
                    count++;
                }
            }
        } catch (Exception e) {
            visitFileFailed(path, new IOException(e));
        }
        return CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) {
        Throwable cause = exc.getCause();
        log.warn("File visit failed: {} {}", exc.getLocalizedMessage(), cause != null ? cause.getLocalizedMessage() : "");
        return CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
        if (exc != null) {
            visitFileFailed(dir, exc);
        }
        return CONTINUE;
    }
}
