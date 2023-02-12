package org.storck.filelocator.model;

import com.arangodb.springframework.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;

import java.util.Collection;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "#{@collectionName}")
public abstract class FileEntry {

    /**
     * db document field: _key
     */
    @Id
    private String id;

    /**
     * db document field: _id
     */
    @ArangoId
    private String arangoId;

    @FulltextIndexed
    @PersistentIndexed
    private String name;

    @FulltextIndexed
    @PersistentIndexed
    private String path;

    private Long lastModifiedTime;

    private Long lastAccessTime;

    private Long creationTime;

    private Long size;

    @Relations(edges = Relation.class, direction = Relations.Direction.INBOUND, lazy = true)
    private Collection<FileEntry> parent;

    @Relations(edges = Relation.class, direction = Relations.Direction.OUTBOUND, lazy = true)
    private Collection<FileEntry> children;
}
