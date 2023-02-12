package org.storck.filelocator.model;

import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Edge;
import com.arangodb.springframework.annotation.From;
import com.arangodb.springframework.annotation.To;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Data
@Edge(collection = "#{@edgeCollectionName}")
@NoArgsConstructor
@AllArgsConstructor
public class Relation {

    public Relation(FileEntry from, FileEntry to) {
        this.from = from;
        this.to = to;
    }

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

    @From
    FileEntry from;

    @To
    FileEntry to;
}
