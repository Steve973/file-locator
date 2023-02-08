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
import java.util.concurrent.CompletableFuture;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Slf4j
@Service
public class FileSystemTraverser implements FileVisitor<Path> {

    public static final String ROOT_PARENT = "<N/A>";

    private int numVisited = 0;

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

    public void updateFileDatabase() {
        arangoOperations.collection(collectionName).drop();
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
        if (!dir.toFile().canRead() || skipPaths.stream().anyMatch(sp -> dir.toFile().getAbsolutePath().startsWith(sp))) {
            return SKIP_SUBTREE;
        } else {
            return visitFile(dir, attrs);
        }
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) {
        try {
            numVisited++;
            if (path == null) {
                log.warn("Skipping null file");
            } else if (attrs == null) {
                visitFileFailed(path, new IOException("Could not read file attributes"));
            } else {
                File file = path.toFile();
                if (file.canRead()) {
                    FileEntry fileEntry = null;
                    File parent = file.getParentFile();
                    String parentPath = parent != null ? parent.getAbsolutePath() : ROOT_PARENT;
                    String name = file.getName();
                    if (attrs.isDirectory()) {
                        fileEntry = DirectoryNode.builder()
                                .name(parent == null && !StringUtils.hasText(name) ? "/": name)
                                .path(parentPath)
                                .creationTime(attrs.creationTime().toMillis())
                                .lastAccessTime(attrs.lastAccessTime().toMillis())
                                .lastModifiedTime(attrs.lastModifiedTime().toMillis())
                                .size(attrs.size())
                                .build();
                    } else if (attrs.isSymbolicLink()) {
                        fileEntry = LinkNode.builder()
                                .name(name)
                                .path(parentPath)
                                .creationTime(attrs.creationTime().toMillis())
                                .lastAccessTime(attrs.lastAccessTime().toMillis())
                                .lastModifiedTime(attrs.lastModifiedTime().toMillis())
                                .size(attrs.size())
                                .build();
                    } else if (attrs.isRegularFile()) {
                        fileEntry = FileNode.builder()
                                .name(name)
                                .path(parentPath)
                                .creationTime(attrs.creationTime().toMillis())
                                .lastAccessTime(attrs.lastAccessTime().toMillis())
                                .lastModifiedTime(attrs.lastModifiedTime().toMillis())
                                .size(attrs.size())
                                .build();
                    } else if (attrs.isOther()) {
                        fileEntry = OtherNode.builder()
                                .name(name)
                                .path(parentPath)
                                .creationTime(attrs.creationTime().toMillis())
                                .lastAccessTime(attrs.lastAccessTime().toMillis())
                                .lastModifiedTime(attrs.lastModifiedTime().toMillis())
                                .size(attrs.size())
                                .build();
                    }
                    if (fileEntry != null) {
                        fileEntryRepository.save(fileEntry);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Unexpected error when processing file", e);
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
