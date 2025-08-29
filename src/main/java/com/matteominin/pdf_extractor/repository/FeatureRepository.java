package com.matteominin.pdf_extractor.repository;

import com.matteominin.pdf_extractor.model.pdf.Feature;
import com.matteominin.pdf_extractor.model.feature.SummaryFeature;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class FeatureRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureRepository.class);
    
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;
    
    @Value("${app.mongodb.database:features_repo}")
    private String databaseName;
    
    @Value("${app.mongodb.collection:features}")
    private String collectionName;
    
    @Value("${app.mongodb.collection.summary:summary_features}")
    private String summaryCollectionName;
    
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;
    private MongoCollection<Document> summaryCollection;
    
    @PostConstruct
    public void initMongoDB() {
        try {
            mongoClient = MongoClients.create(mongoUri);
            database = mongoClient.getDatabase(databaseName);
            collection = database.getCollection(collectionName);
            summaryCollection = database.getCollection(summaryCollectionName);
            logger.info("MongoDB connection initialized successfully");
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

    public List<Feature> findAll() {
        return collection.find()
        .map(this::convertToFeature)
        .into(new ArrayList<>());
    }

    // Efficient method to get only features with embeddings (for clustering)
    public List<Feature> findAllWithEmbeddings() {
        return collection.find(new Document("embedding", new Document("$exists", true).append("$ne", null)))
        .map(this::convertToFeature)
        .into(new ArrayList<>());
    }

    // Stream-based method for large datasets
    public java.util.stream.Stream<Feature> streamAllWithEmbeddings() {
        return collection.find(new Document("embedding", new Document("$exists", true).append("$ne", null)))
        .map(this::convertToFeature)
        .into(new ArrayList<>())
        .stream();
    }

    // Efficient method to get only id, feature text, and embedding (minimal data for clustering)
    public List<Document> findEmbeddingsOnly() {
        return collection.find(new Document("embedding", new Document("$exists", true).append("$ne", null)))
        .projection(new Document("_id", 1).append("feature", 1).append("embedding", 1))
        .into(new ArrayList<>());
    }

    private Feature convertToFeature(Document doc) {
        Feature feature = new Feature();
        feature.setId(doc.getObjectId("_id").toString());
        feature.setFeature(doc.getString("feature"));
        feature.setDescription(doc.getString("description"));
        feature.setCategory(doc.getString("category"));
        feature.setEvidence(doc.getString("evidence"));
        feature.setConfidence(doc.getDouble("confidence"));
        feature.setSource_title(doc.getString("sourceTitle"));
        feature.setFilePath(doc.getString("filePath"));
        if (doc.get("embedding") != null) {
            @SuppressWarnings("unchecked")
            List<Double> embedding = (List<Double>) doc.get("embedding");
            feature.setEmbedding(embedding);
        }
        feature.setCreatedAt(doc.getDate("createdAt"));
        feature.setUpdatedAt(doc.getDate("updatedAt"));
        return feature;
    }
    
    public String saveFeature(Feature feature) {
        try {
            feature.setCreatedAt(new Date());
            feature.setUpdatedAt(new Date());
            
            Document doc = convertToDocument(feature);
            InsertOneResult result = collection.insertOne(doc);
            
            if (result.wasAcknowledged()) {
                ObjectId insertedId = result.getInsertedId().asObjectId().getValue();
                feature.setId(insertedId.toString());
                logger.debug("Feature saved successfully with ID: {}", insertedId);
                return insertedId.toString();
            } else {
                throw new RuntimeException("Failed to save feature to database");
            }
        } catch (Exception e) {
            logger.error("Error saving feature: {}", e.getMessage(), e);
            throw new RuntimeException("Database save operation failed", e);
        }
    }

    public List<String> saveFeatureList(List<Feature> features) {
        List<String> ids = new ArrayList<>();
        List<Document> docs = new ArrayList<>();
        Date now = new Date();
        for (Feature feature : features) {
            feature.setCreatedAt(now);
            feature.setUpdatedAt(now);
            docs.add(convertToDocument(feature));
        }
        try {
            collection.insertMany(docs);
            for (Document doc : docs) {
                ObjectId id = doc.getObjectId("_id");
                ids.add(id != null ? id.toString() : "");
            }
            logger.debug("Batch saved {} features", docs.size());
            return ids;
        } catch (Exception e) {
            logger.error("Error saving feature list: {}", e.getMessage(), e);
            throw new RuntimeException("Database batch save operation failed", e);
        }
    }
    
    private Document convertToDocument(Feature feature) {
        return new Document()
            .append("feature", feature.getFeature())
            .append("description", feature.getDescription())
            .append("category", feature.getCategory())
            .append("evidence", feature.getEvidence())
            .append("confidence", feature.getConfidence())
            .append("sourceTitle", feature.getSource_title())
            .append("filePath", feature.getFilePath())
            .append("section_text", feature.getSection_text())
            .append("embedding", feature.getEmbedding())
            .append("createdAt", feature.getCreatedAt())
            .append("updatedAt", feature.getUpdatedAt());
    }
    
    // SummaryFeature methods
    public List<String> saveSummaryFeatures(List<SummaryFeature> summaryFeatures) {
        List<String> ids = new ArrayList<>();
        List<Document> docs = new ArrayList<>();
        Date now = new Date();
        
        for (SummaryFeature summaryFeature : summaryFeatures) {
            docs.add(convertToSummaryDocument(summaryFeature, now));
        }
        
        try {
            summaryCollection.insertMany(docs);
            for (Document doc : docs) {
                ObjectId id = doc.getObjectId("_id");
                ids.add(id != null ? id.toString() : "");
            }
            logger.debug("Batch saved {} summary features", docs.size());
            return ids;
        } catch (Exception e) {
            logger.error("Error saving summary feature list: {}", e.getMessage(), e);
            throw new RuntimeException("Database batch save operation failed", e);
        }
    }
    
    public List<SummaryFeature> findAllSummaryFeatures() {
        return summaryCollection.find()
            .map(this::convertToSummaryFeature)
            .into(new ArrayList<>());
    }
    
    private Document convertToSummaryDocument(SummaryFeature summaryFeature, Date timestamp) {
        return new Document()
            .append("feature", summaryFeature.getFeature())
            .append("description", summaryFeature.getDescription())
            .append("count", summaryFeature.getCount())
            .append("example", summaryFeature.getExample())
            .append("embedding", summaryFeature.getEmbedding())
            .append("createdAt", timestamp)
            .append("updatedAt", timestamp);
    }
    
    private SummaryFeature convertToSummaryFeature(Document doc) {
        return SummaryFeature.builder()
            .feature(doc.getString("feature"))
            .description(doc.getString("description"))
            .count(doc.getString("count"))
            .example(doc.getString("example"))
            .embedding(doc.getList("embedding", Double.class))
            .build();
    }
}
