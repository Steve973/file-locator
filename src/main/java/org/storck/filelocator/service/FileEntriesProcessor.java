package org.storck.filelocator.service;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.storck.filelocator.model.FileEntry;
import org.storck.filelocator.model.Relation;
import org.storck.filelocator.repository.FileEntryRepository;
import org.storck.filelocator.repository.FileRelationRepository;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static org.storck.filelocator.service.FileSystemTraverser.ROOT_PARENT;

@Slf4j
@Service
public class FileEntriesProcessor {

    private static final String ROOT_DIR = "<SLASH_ROOT>";

    final FileEntryRepository fileEntryRepository;

    final FileRelationRepository fileRelationRepository;

    public FileEntriesProcessor(FileEntryRepository fileEntryRepository, FileRelationRepository fileRelationRepository) {
        this.fileEntryRepository = fileEntryRepository;
        this.fileRelationRepository = fileRelationRepository;
    }

    private static final BiFunction<FileEntry, FileEntryRepository, Relation> createEdge = (fileEntry, repo) -> {
        Relation result = null;
        try {
            String fileEntryPath = fileEntry.getPath();
            if (fileEntryPath.equals(ROOT_PARENT)) {
                // since there is no parent for the root directory, skip this entry
                return null;
            }
            // Splitting a string when the delimiter is the first character causes a problem of
            // the first element of the array being empty, so we can solve this issue by prepending
            // a known string.
            String[] pathSegments = (ROOT_DIR + fileEntryPath).split("/");
            int numberOfSegments = pathSegments.length;
            String path = Arrays.stream(pathSegments, 0, numberOfSegments - 1)
                    .collect(Collectors.joining("/"));
            // We have to remove the prepended string before attempting to use it.
            path = path.replaceFirst(ROOT_DIR, "");
            // In cases where this is an element in the root directory, there is only one path element,
            // and we have to remove the prepended string .
            String name = pathSegments[numberOfSegments - 1].replaceFirst(ROOT_DIR, "");
            if (Strings.isNullOrEmpty(path) && Strings.isNullOrEmpty(name)) {
                // The parent directory of an item in the root directory has an empty parent path, and
                // a name of "/", so we need to adjust this to the special case of using the ROOT_PARENT
                // constant during ingest of the file system as a placeholder for the parent of the root
                // directory.  This special case also has the value set to "/" for the name of the root
                // directory.
                path = ROOT_PARENT;
                name = "/";
            } else if (!path.startsWith("/")) {
                // Ensure that, for all other cases, the path starts with a forward slash.
                path = "/" + path;
            }
            result = Optional.ofNullable(repo.findOneByPathIsAndNameIs(path, name))
                    .map(fe -> new Relation(fe, fileEntry))
                    .orElseGet(() -> {
                        log.info("Could not create a parent relationship to fileEntryPath: {}, fileEntryName: {}",
                                fileEntryPath, fileEntry.getName());
                        return null;
                    });
        } catch (Exception e) {
            log.warn("Error when trying to create file system relationship: {}", e.getLocalizedMessage());
        }
        return result;
    };

    public void processForRelationships() {
        Flux.fromIterable(fileEntryRepository.findAll())
                .subscribeOn(Schedulers.boundedElastic())
                .mapNotNull(fe -> createEdge.apply(fe, fileEntryRepository))
                .buffer(1000)
                .map(fileRelationRepository::saveAll)
                .doOnNext(it -> log.info("Batch complete"))
                .then()
                .subscribe();
    }
}
