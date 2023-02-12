package org.storck.filelocator.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Example;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.storck.filelocator.model.FileEntry;
import org.storck.filelocator.repository.FileEntryRepository;
import org.storck.filelocator.service.FileEntriesProcessor;
import org.storck.filelocator.service.FileSystemTraverser;
import org.storck.filelocator.service.ReactiveFileSystemTraverser;
import org.storck.filelocator.service.SkipPathListGenerator;

import java.util.Collection;

@Slf4j
@Tag(name = "file-locator")
@RestController("/files")
public class FileLocatorController {

    private final FileSystemTraverser fileSystemTraverser;

    private final ReactiveFileSystemTraverser reactiveFileSystemTraverser;

    private final SkipPathListGenerator skipPathListGenerator;

    private final FileEntriesProcessor fileEntriesProcessor;

    private final FileEntryRepository fileEntryRepository;

    public FileLocatorController(FileSystemTraverser fileSystemTraverser,
                                 ReactiveFileSystemTraverser reactiveFileSystemTraverser,
                                 SkipPathListGenerator skipPathListGenerator,
                                 FileEntriesProcessor fileEntriesProcessor,
                                 FileEntryRepository fileEntryRepository) {
        this.fileSystemTraverser = fileSystemTraverser;
        this.reactiveFileSystemTraverser = reactiveFileSystemTraverser;
        this.skipPathListGenerator = skipPathListGenerator;
        this.fileEntriesProcessor = fileEntriesProcessor;
        this.fileEntryRepository = fileEntryRepository;
    }

    @Operation
    @PutMapping(path = "/updatedb")
    ResponseEntity<String> updateFileDb() {
        String result = fileSystemTraverser.updateFileDatabase();
        return new ResponseEntity<>("Database updated: " + result, HttpStatus.OK);
    }

    @Operation
    @PutMapping(path = "/reacctive/updatedb")
    ResponseEntity<String> updateFileDbReactive() {
        String result = reactiveFileSystemTraverser.updateFileDatabase();
        return new ResponseEntity<>("Database updated: " + result, HttpStatus.OK);
    }

    @Operation
    @PutMapping(path = "/updateSkipPaths")
    ResponseEntity<Collection<String>> updateSkipPaths() {
        Collection<String> response = skipPathListGenerator.generateSkipPathList();
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation
    @PutMapping(path = "/graph")
    ResponseEntity<String> processEntriesForRelations() {
        fileEntriesProcessor.processForRelationships();
        return new ResponseEntity<>("Processing entries for file system relationships", HttpStatus.ACCEPTED);
    }

    @Operation
    @PostMapping(path = "/example", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<FileEntry> findByExampleNameAndPath(@RequestBody FileEntry example) {
        FileEntry result = fileEntryRepository.findOne(Example.of(example)).orElse(example);
        return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
    }

    @Operation
    @GetMapping(path = "/example")
    ResponseEntity<FileEntry> findByPathAndName(@RequestParam String path, @RequestParam String name) {
        log.info("path: {}, name: {}", path, name);
        FileEntry result = fileEntryRepository.findOneByPathIsAndNameIs(path, name);
        return new ResponseEntity<>(result, HttpStatus.ACCEPTED);
    }

    @Operation
    @GetMapping(path = "/regex/{exp}")
    ResponseEntity<Collection<FileEntry>> findByRegex(@PathVariable String exp) {
        Collection<FileEntry> results = fileEntryRepository.findByNameMatchesRegex(exp);
        return new ResponseEntity<>(results, HttpStatus.ACCEPTED);
    }
}
