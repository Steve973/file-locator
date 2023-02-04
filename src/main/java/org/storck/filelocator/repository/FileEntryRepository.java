package org.storck.filelocator.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import org.storck.filelocator.model.FileEntry;

public interface FileEntryRepository extends ArangoRepository<FileEntry, String> {
}
