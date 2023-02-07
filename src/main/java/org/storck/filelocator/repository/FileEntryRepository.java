package org.storck.filelocator.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import org.storck.filelocator.model.FileEntry;

import java.util.Collection;

public interface FileEntryRepository extends ArangoRepository<FileEntry, String> {
    Collection<FileEntry> findByNameContaining(String text);

    Collection<FileEntry> findByNameContainingIgnoreCase(String text);

    Collection<FileEntry> findByNameStartingWith(String text);

    Collection<FileEntry> findByNameStartingWithIgnoreCase(String text);

    Collection<FileEntry> findByNameEndingWith(String text);

    Collection<FileEntry> findByNameEndingWithIgnoreCase(String text);

    Collection<FileEntry> findByNameMatchesRegex(String text);

    Collection<FileEntry> findByPathIsAndNameIs(String path, String name);
}
