package org.storck.filelocator.model;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Builder
@Document(collection = "#{@collectionName}")
@NoArgsConstructor
@AllArgsConstructor
public final class FileEntry {

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

    @Builder.Default
    private String name = "unknown";

    @Builder.Default
    private String path = "unknown";

    @Builder.Default
    private long lastModifiedTime = 0;

    @Builder.Default
    private long lastAccessTime = 0;

    @Builder.Default
    private long creationTime = 0;

    @Builder.Default
    private boolean isRegularFile = false;

    @Builder.Default
    private boolean isDirectory = false;

    @Builder.Default
    private boolean isSymbolicLink = false;

    @Builder.Default
    private boolean isOther = false;

    @Builder.Default
    private long size = 0;
}
