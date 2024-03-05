package org.storck.filelocator.repository;

import com.arangodb.springframework.annotation.Query;
import com.arangodb.springframework.repository.ArangoRepository;
import org.springframework.data.repository.query.Param;
import org.storck.filelocator.model.FileEntry;

import java.util.Collection;
import java.util.List;

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

    @Query("""
            LET startNode = (
                FOR node IN nodes
                FILTER node.name == @firstString
                LIMIT 1
                RETURN node
            )
            LET traversalResult = (
                FOR i IN 1..LENGTH(@list)
                    LET currentNode = (
                        FOR vertex, edge, path IN 1..1 OUTBOUND startNode[0]._id edges
                        FILTER vertex.name == @list[i]
                        RETURN vertex
                    )
                    FILTER LENGTH(currentNode) > 0  // Check if nodes are found
                    LET startNode = (
                        FOR v IN currentNode
                        FILTER v.someField != @stopValue  // Filter out nodes with stop value
                        LIMIT 1
                        RETURN v
                    )
                    FILTER LENGTH(startNode) == 0  // Check if no nodes found after filter
                    RETURN startNode
                    LET stopValueFound = (
                        FOR v IN currentNode
                        FILTER v.someField == @stopValue  // Check if stop value found
                        LIMIT 1
                        RETURN 1
                    )
                    FILTER LENGTH(stopValueFound) > 0  // Check if stop value found
                    LIMIT 1  // Stop traversal if stop value found
                    RETURN []
                )
                RETURN traversalResult
            """)
    List<FileEntry> customTraversalQuery(@Param("firstString") String firstString,
                                         @Param("list") List<String> list,
                                         @Param("stopValue") String stopValue);

    @Query("""
            LET startNode = (
                FOR node IN nodes
                FILTER node.name == @firstString
                LIMIT 1
                RETURN node
            )
            LET traversalResult = (
                FOR i IN 1..LENGTH(@list)
                    LET searchValue = @list[i]
                    LET searchPattern = SPLIT(searchValue, '*')
                    LET currentNode = (
                        FOR vertex, edge, path IN 1..1 OUTBOUND startNode[0]._id edges
                        FILTER
                            (searchValue == '*' OR
                            (searchPattern[0] == '' OR
                            VERTEX.name LIKE CONCAT(searchPattern[0], '%')) AND
                            (searchPattern[1] == '' OR
                            VERTEX.name LIKE CONCAT('%', searchPattern[1])))
                            AND VERTEX.someField != @stopValue
                        RETURN vertex
                    )
                    FILTER LENGTH(currentNode) > 0
                    LET startNode = (
                        FOR v IN currentNode
                        FILTER v.someField != @stopValue
                        LIMIT 1
                        RETURN v
                    )
                    FILTER LENGTH(startNode) == 0
                    RETURN startNode
                    LET stopValueFound = (
                        FOR v IN currentNode
                        FILTER v.someField == @stopValue
                        LIMIT 1
                        RETURN 1
                    )
                    FILTER LENGTH(stopValueFound) > 0
                    LIMIT 1
                    RETURN []
                )
                RETURN traversalResult
            """)
    List<FileEntry> wildcardTraversalQuery(@Param("firstString") String firstString,
                                          @Param("list") List<String> list,
                                          @Param("stopValue") String stopValue);
}
