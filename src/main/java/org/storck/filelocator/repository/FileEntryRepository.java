package org.storck.filelocator.repository;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import org.springframework.data.repository.query.Param;
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

    FileEntry findOneByName(String name);

    FileEntry findOneByPathIsAndNameIs(String path, String name);

    @Query("FOR v, e, p IN 1.." + Short.MAX_VALUE + " OUTBOUND @startVertex GRAPH 'fileSystemGraph' "
            + "FILTER REGEX_TEST(v.name, @nameRegex) "
            + "RETURN v")
    Collection<FileEntry> searchFilesByNameInGraph(@Param("startVertex") String startVertex, @Param("nameRegex") String nameRegex);
}
