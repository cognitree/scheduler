package com.cognitree.kronos.scheduler.store.mongo;

import com.cognitree.kronos.scheduler.model.Namespace;
import com.cognitree.kronos.scheduler.model.NamespaceId;
import com.cognitree.kronos.scheduler.store.NamespaceStore;
import com.cognitree.kronos.scheduler.store.StoreException;
import com.mongodb.client.MongoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

/**
 * A standard MongoDB based implementation of {@link NamespaceStore}.
 */
public class MongoNamespaceStore extends MongoStore<Namespace> implements NamespaceStore {

    private static final Logger logger = LoggerFactory.getLogger(MongoNamespaceStore.class);

    private static final String DATABASE_NAME = "namespace";
    private static final String COLLECTION_NAME = "config";

    MongoNamespaceStore(MongoClient mongoClient) {
        super(mongoClient, Namespace.class);
    }

    @Override
    public void store(Namespace namespace) throws StoreException {
        logger.debug("Received request to store namespace {}", namespace);
        insertOne(DATABASE_NAME, COLLECTION_NAME, namespace);
    }

    @Override
    public List<Namespace> load() throws StoreException {
        logger.debug("Received request to get all namespaces");
        return findAll(DATABASE_NAME, COLLECTION_NAME);
    }

    @Override
    public Namespace load(NamespaceId namespaceId) throws StoreException {
        logger.debug("Received request to load namespace with id {}", namespaceId);
        return findOne(DATABASE_NAME, COLLECTION_NAME, eq("name", namespaceId.getName()));
    }

    @Override
    public void update(Namespace namespace) throws StoreException {
        logger.debug("Received request to update namespace to {}", namespace);
        findOneAndUpdate(DATABASE_NAME, COLLECTION_NAME,
                eq("name", namespace.getName()),
                set("description", namespace.getDescription()));
    }

    @Override
    public void delete(NamespaceId namespaceId) throws StoreException {
        logger.debug("Received request to delete job with id {}", namespaceId);
        deleteOne(DATABASE_NAME, COLLECTION_NAME, eq("name", namespaceId.getName()));
        dropDatabase(DATABASE_NAME);
    }
}
