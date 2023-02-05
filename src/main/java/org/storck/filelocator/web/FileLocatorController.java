package org.storck.filelocator.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.storck.filelocator.model.FileEntry;
import org.storck.filelocator.repository.FileEntryRepository;
import org.storck.filelocator.service.FileSystemTraverser;

import java.util.Collection;

@Tag(name = "file-locator")
@RestController("/files")
public class FileLocatorController {

    private final FileSystemTraverser fileSystemTraverser;

    private final FileEntryRepository fileEntryRepository;

    public FileLocatorController(FileSystemTraverser fileSystemTraverser, FileEntryRepository fileEntryRepository) {
        this.fileSystemTraverser = fileSystemTraverser;
        this.fileEntryRepository = fileEntryRepository;
    }

    @Operation
    @PutMapping(path = "/updatedb")
    ResponseEntity<String> updateFileDb() {
        fileSystemTraverser.updateFileDatabase();
        return new ResponseEntity<>("Updating file database for user", HttpStatus.ACCEPTED);
    }

    @Operation
    @GetMapping(path = "/regex/{exp}")
    ResponseEntity<Collection<FileEntry>> findByRegex(@PathVariable String exp) {
        Collection<FileEntry> results = fileEntryRepository.findByNameMatchesRegex(exp);
        return new ResponseEntity<>(results, HttpStatus.ACCEPTED);
    }
}
