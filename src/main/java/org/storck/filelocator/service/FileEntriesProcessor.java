package org.storck.filelocator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.storck.filelocator.model.Relation;
import org.storck.filelocator.repository.FileEntryRepository;
import org.storck.filelocator.repository.FileRelationRepository;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileEntriesProcessor {

    final FileEntryRepository fileEntryRepository;

    final FileRelationRepository fileRelationRepository;

    public FileEntriesProcessor(FileEntryRepository fileEntryRepository, FileRelationRepository fileRelationRepository) {
        this.fileEntryRepository = fileEntryRepository;
        this.fileRelationRepository = fileRelationRepository;
    }

    public void processForRelationships() {
        CompletableFuture.runAsync(() ->
            fileEntryRepository.findAll().forEach(fileEntry -> {
                try {
                    String fileEntryPath = fileEntry.getPath();
                    String[] pathSegments = fileEntryPath.split("/");
                    int numberOfSegments = pathSegments.length;
                    String path;
                    String name;
                    if (numberOfSegments == 0) {
                        // This is the case when the item is the root directory
                        path = "";
                        name = "/";
                    } else if (numberOfSegments == 1) {
                        // This is the case for files or directories in the root directory
                        path = "/";
                        name = pathSegments[0];
                    } else {
                        // This is for all other cases
                        path = Arrays.stream(pathSegments, 0, numberOfSegments - 1)
                                .collect(Collectors.joining("/"));
                        name = pathSegments[numberOfSegments - 1];
                    }
                    fileEntryRepository.findByPathIsAndNameIs(path, name).stream().findFirst().ifPresentOrElse(fe ->
                                    fileRelationRepository.save(new Relation(fe, fileEntry)),
                            () -> log.info("Could not create a relationship from {}/{} to {}/{}/{}",
                                    path, name, path, name, fileEntry.getName()));
                } catch (Exception e) {
                    log.warn("Error when trying to create file system relationship: {}", e.getLocalizedMessage());
                }
            })
        );
    }
}
