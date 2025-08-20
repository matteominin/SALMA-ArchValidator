package com.matteominin.pdf_extractor.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DBManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DBManager.class);
    
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;
    
    @Value("${app.mongodb.database:features_repo}")
    private String databaseName;
    
    private MongoClient mongoClient;
    private MongoDatabase database;
    
    @PostConstruct
    public void initMongoDB() {
        try {
            mongoClient = MongoClients.create(mongoUri);
            database = mongoClient.getDatabase(databaseName);
            logger.info("MongoDB connection initialized successfully for database: {}", databaseName);
        } catch (Exception e) {
            logger.error("Failed to initialize MongoDB connection", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    @PreDestroy
    public void closeMongoDB() {
        if (mongoClient != null) {
            mongoClient.close();
            logger.info("MongoDB connection closed");
        }
    }
    
    /**
     * Get a collection from the database.
     * 
     * @param collectionName the name of the collection
     * @return MongoCollection<Document>
     */
    public MongoCollection<Document> getCollection(String collectionName) {
        if (database == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return database.getCollection(collectionName);
    }
    
    /**
     * Get the database instance.
     * 
     * @return MongoDatabase
     */
    public MongoDatabase getDatabase() {
        if (database == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return database;
    }
    
    /**
     * Check if the database connection is ready.
     * 
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return database != null;
    }
}
