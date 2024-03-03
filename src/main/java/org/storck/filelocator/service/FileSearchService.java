package org.storck.filelocator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.storck.filelocator.model.FileEntry;
import org.storck.filelocator.repository.FileEntryRepository;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class FileSearchService {

    final FileEntryRepository fileEntryRepository;

    public FileSearchService(final FileEntryRepository fileEntryRepository) {
        this.fileEntryRepository = fileEntryRepository;
    }

    public String resolveIdByName(String name) {
        return Optional.ofNullable(fileEntryRepository.findOneByName(name))
                .map(FileEntry::getArangoId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Cannot find ArangoDB identifier for file entry with name: " + name));
    }

    public List<String> searchByRegex(String start, String regex) {
        String nodePath = start == null || start.isEmpty() ? "/" : start;
        String startNodeId = resolveIdByName(nodePath);
        log.info("Starting at '{}', and searching with expression '{}'", nodePath, regex);
        return fileEntryRepository.searchFilesByNameInGraph(startNodeId, regex).stream()
                .map(fe -> String.format("%s%s%s", fe.getPath(), File.separator, fe.getName()))
                .sorted()
                .toList();
    }
}
