package org.storck.filelocator.model;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import com.arangodb.springframework.annotation.Relations;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;

import java.util.Collection;

@Data
@SuperBuilder
@Document(collection = "#{@collectionName}")
@NoArgsConstructor
@AllArgsConstructor
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

    private String name;

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
