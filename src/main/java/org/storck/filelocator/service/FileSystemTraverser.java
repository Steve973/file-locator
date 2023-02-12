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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.SKIP_SUBTREE;

@Slf4j
@Service
public class FileSystemTraverser implements FileVisitor<Path> {

    public static final String ROOT_PARENT = "<N/A>";

    int count = 0;

    private final List<String> skipPaths = new ArrayList<>();

    private final String collectionName;

    private final SkipPathListGenerator skipPathListGenerator;

    private final FileEntryRepository fileEntryRepository;

    private final ArangoOperations arangoOperations;

    protected FileSystemTraverser(final FileEntryRepository fileEntryRepository,
                                  final ArangoOperations arangoOperations,
                                  @Qualifier("collectionName") final String collectionName,
                                  SkipPathListGenerator skipPathListGenerator) {
        this.fileEntryRepository = fileEntryRepository;
        this.arangoOperations = arangoOperations;
        this.collectionName = collectionName;
        this.skipPathListGenerator = skipPathListGenerator;
    }

    public String updateFileDatabase() {
        long start = System.currentTimeMillis();
        arangoOperations.collection(collectionName).drop();
        skipPaths.clear();
        skipPaths.addAll(skipPathListGenerator.generateSkipPathList());
        try {
            Files.walkFileTree(new File("/").toPath(), this);
        } catch (IOException e) {
            log.error("Error encountered when updating file database", e);
        }
        long duration = (System.currentTimeMillis() - start) / 1000;
        return String.format("Count: %d, time: %s seconds", count, duration);
    }

    @Override
    public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) {
        try {
            return skipPaths.contains(path.toString()) ? SKIP_SUBTREE : visitFile(path, attrs);
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
    public FileVisitResult visitFileFailed(Path path, IOException exc) {
        log.warn("File visit failed: {}", path.toString());
        return path.toFile().isDirectory() ? SKIP_SUBTREE : CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path path, IOException exc) {
        return CONTINUE;
    }
}
