package org.storck.filelocator.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import org.storck.filelocator.model.Relation;

public interface FileRelationRepository extends ArangoRepository<Relation, String> {
}
