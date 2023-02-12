package org.storck.filelocator.service;

import com.arangodb.springframework.core.ArangoOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.storck.filelocator.model.*;
import org.storck.filelocator.repository.FileEntryRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.storck.filelocator.service.FileSystemTraverser.createFileEntry;

@Slf4j
@Service
public class ReactiveFileSystemTraverser {

    int count = 0;

    private final Collection<String> skipPaths = new ArrayList<>();

    private final String collectionName;

    private final SkipPathListGenerator skipPathListGenerator;

    private final AccessiblePathListGenerator accessiblePathListGenerator;

    private final FileEntryRepository fileEntryRepository;

    private final ArangoOperations arangoOperations;

    protected ReactiveFileSystemTraverser(final FileEntryRepository fileEntryRepository,
                                          final ArangoOperations arangoOperations,
                                          @Qualifier("collectionName") final String collectionName,
                                          SkipPathListGenerator skipPathListGenerator,
                                          AccessiblePathListGenerator accessiblePathListGenerator) {
        this.fileEntryRepository = fileEntryRepository;
        this.arangoOperations = arangoOperations;
        this.collectionName = collectionName;
        this.skipPathListGenerator = skipPathListGenerator;
        this.accessiblePathListGenerator = accessiblePathListGenerator;
    }

    private final BiConsumer<Collection<FileEntry>, FileEntryRepository> saveFile = (entries, repo) -> {
        repo.saveAll(entries);
        count += entries.size();
        log.info("Batch complete");
    };

    public String updateFileDatabase() {
        long start = System.currentTimeMillis();
        arangoOperations.collection(collectionName).drop();
        skipPaths.clear();
        skipPaths.addAll(skipPathListGenerator.generateSkipPathList());
        List<Path> accessiblePaths = accessiblePathListGenerator.generateAccessiblePathsList(skipPaths);
        saveFile.accept(List.of(visitFile(new File("/"))), fileEntryRepository);
        try (Stream<Path> pathStream = accessiblePaths.stream()) {
            pathStream.forEach(accessiblePath -> {
                try (Stream<Path> accessiblePathStream = Files.list(accessiblePath)) {
                    Flux.fromStream(accessiblePathStream)
                            .map(Path::toFile)
                            .filter(File::canRead)
                            .flatMap(f -> Mono.fromCallable(() -> this.visitFile(f))
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .filter(Objects::nonNull))
                            .buffer(10000)
                            .handle((relations, synchronousSink) -> saveFile.accept(relations, fileEntryRepository))
                            .blockLast();
                } catch (Exception e) {
                    log.error("Error processing file system", e);
                }
            });
        } catch (Exception e) {
            log.error("Error processing file system", e);
        }
        long duration = (System.currentTimeMillis() - start) / 1000;
        return String.format("Count: %d, time: %s seconds", count, duration);
    }

    public FileEntry visitFile(File file) {
        FileEntry result = null;
        try {
            if (file == null) {
                log.warn("Skipping null file");
            } else {
                BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                result = createFileEntry(file, attrs);
            }
        } catch (Exception e) {
            log.warn("Unexpected error when processing file", e);
        }
        return result;
    }
}
