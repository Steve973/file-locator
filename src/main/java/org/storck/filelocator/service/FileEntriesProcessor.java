package org.storck.filelocator.service;

import com.arangodb.ArangoDatabase;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.model.GraphCreateOptions;
import com.arangodb.springframework.core.ArangoOperations;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.storck.filelocator.model.FileEntry;
import org.storck.filelocator.model.Relation;
import org.storck.filelocator.repository.FileEntryRepository;
import org.storck.filelocator.repository.FileRelationRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static org.storck.filelocator.service.ReactiveFileSystemTraverser.ROOT_PARENT;

@Slf4j
@Service
public class FileEntriesProcessor {

    final FileEntryRepository fileEntryRepository;

    final FileRelationRepository fileRelationRepository;

    final ArangoDatabase arangoDatabase;

    final String databaseName;

    final String collectionName;

    final String edgeCollectionName;

    final String graphName;

    public FileEntriesProcessor(FileEntryRepository fileEntryRepository,
                                FileRelationRepository fileRelationRepository,
                                ArangoOperations arangoOperations,
                                @Value("${arangodb.spring.data.database}") String databaseName,
                                @Qualifier("collectionName") String collectionName,
                                @Qualifier("edgeCollectionName") String edgeCollectionName,
                                @Qualifier("graphName") String graphName) {
        this.fileEntryRepository = fileEntryRepository;
        this.fileRelationRepository = fileRelationRepository;
        this.databaseName = databaseName;
        this.collectionName = collectionName;
        this.edgeCollectionName = edgeCollectionName;
        this.graphName = graphName;
        this.arangoDatabase = arangoOperations.driver().db(databaseName);
    }

    private static boolean createGraph(ArangoDatabase arangoDatabase, String collectionName, String edgeCollectionName, String graphName) {
        if (arangoDatabase.collection(collectionName).exists() &&
                arangoDatabase.collection(edgeCollectionName).exists() &&
                !arangoDatabase.graph(graphName).exists()) {
            EdgeDefinition edgeDefinition = new EdgeDefinition()
                    .collection(edgeCollectionName)
                    .from(collectionName)
                    .to(collectionName);
            GraphCreateOptions graphOptions = new GraphCreateOptions()
                    .orphanCollections(collectionName)
                    .isSmart(true);
            arangoDatabase.createGraph(graphName, List.of(edgeDefinition), graphOptions);
        } else {
            throw new IllegalStateException("Vertices collection or edge collection does not exist");
        }
        return arangoDatabase.graph(graphName).exists();
    }

    private static final BiFunction<FileEntry, FileEntryRepository, Relation> createEdge = (fileEntry, repo) -> {
        Relation result = new Relation();
        String fileEntryPath = fileEntry.getPath();
        String parentPath;
        String parentName;
        // There is no need to process the root node, since it has no parent
        if (!fileEntryPath.equals(ROOT_PARENT)) {
            try {
                if (fileEntryPath.equals("/")) {
                    // When the parent directory of a node is the root directory "/", we need to adjust for
                    // this special case.  When the file system nodes were ingested, the root node was created
                    // with its nonexistent parent set to the value of the ROOT_PARENT constant, and its name
                    // value set to "/".
                    parentPath = ROOT_PARENT;
                    parentName = "/";
                } else {
                    int lastDelimAt = fileEntryPath.lastIndexOf('/');
                    parentPath = fileEntryPath.substring(0, Math.max(lastDelimAt, 1));
                    parentName = fileEntryPath.substring(lastDelimAt + 1);
                }
                result = Optional.ofNullable(repo.findOneByPathIsAndNameIs(parentPath, parentName))
                        .map(fe -> new Relation(fe, fileEntry))
                        .orElseGet(() -> {
                            log.info("Could not create a parent relationship to fileEntryPath: {}, fileEntryName: {}",
                                    fileEntryPath, fileEntry.getName());
                            return new Relation();
                        });
            } catch (Exception e) {
                log.warn("Error when trying to create file system relationship: {}", e.getLocalizedMessage());
            }
        }
        return result;
    };

    private static final BiConsumer<Collection<Relation>, FileRelationRepository> saveEdges = (edges, repo) -> {
        repo.saveAll(edges);
        log.info("Batch complete");
    };

    public void processForRelationships() {
        Flux.fromIterable(fileEntryRepository.findAll())
                .flatMap(fe -> Mono.fromCallable(() -> createEdge.apply(fe, fileEntryRepository))
                        .subscribeOn(Schedulers.boundedElastic())
                        .filter(r -> r.getFrom() != null && r.getTo() != null))
                .buffer(10000)
                .handle((relations, synchronousSink) -> saveEdges.accept(relations, fileRelationRepository))
                .blockLast();
        if (!createGraph(arangoDatabase, collectionName, edgeCollectionName, graphName)) {
            throw new IllegalStateException("Could not create graph database: " + graphName);
        }
    }
}
