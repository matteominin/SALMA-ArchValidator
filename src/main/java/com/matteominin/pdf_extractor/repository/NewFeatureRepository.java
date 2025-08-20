package com.matteominin.pdf_extractor.repository;

import com.matteominin.pdf_extractor.config.DBManager;
import com.matteominin.pdf_extractor.model.Feature;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Repository
public class NewFeatureRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(NewFeatureRepository.class);
    
    @Value("${app.mongodb.collection:features}")
    private String collectionName;
    
    @Autowired
    private DBManager dbManager;
    
    private MongoCollection<Document> getCollection() {
        return dbManager.getCollection(collectionName);
    }

    public List<Feature> findAll() {
        try {
            MongoCollection<Document> collection = getCollection();
            return collection.find()
                .map(this::convertToFeature)
                .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error retrieving features: {}", e.getMessage(), e);
            throw new RuntimeException("Database retrieval operation failed", e);
        }
    }

    // Efficient method to get only features with embeddings (for clustering)
    public List<Feature> findAllWithEmbeddings() {
        try {
            MongoCollection<Document> collection = getCollection();
            return collection.find(new Document("embedding", new Document("$exists", true).append("$ne", null)))
                .map(this::convertToFeature)
                .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error retrieving features with embeddings: {}", e.getMessage(), e);
            throw new RuntimeException("Database retrieval operation failed", e);
        }
    }

    // Stream-based method for large datasets
    public java.util.stream.Stream<Feature> streamAllWithEmbeddings() {
        return findAllWithEmbeddings().stream();
    }

    // Efficient method to get only id, feature text, and embedding (minimal data for clustering)
    public List<Document> findEmbeddingsOnly() {
        try {
            MongoCollection<Document> collection = getCollection();
            return collection.find(new Document("embedding", new Document("$exists", true).append("$ne", null)))
                .projection(new Document("_id", 1).append("feature", 1).append("embedding", 1))
                .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error retrieving embeddings only: {}", e.getMessage(), e);
            throw new RuntimeException("Database retrieval operation failed", e);
        }
    }

    private Feature convertToFeature(Document doc) {
        Feature feature = new Feature();
        feature.setId(doc.getObjectId("_id").toString());
        feature.setFeature(doc.getString("feature"));
        feature.setDescription(doc.getString("description"));
        feature.setCategory(doc.getString("category"));
        feature.setEvidence(doc.getString("evidence"));
        feature.setConfidence(doc.getDouble("confidence"));
        feature.setSource_title(doc.getString("source_title"));
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
    
    public String save(Feature feature) {
        try {
            feature.setCreatedAt(new Date());
            feature.setUpdatedAt(new Date());
            
            MongoCollection<Document> collection = getCollection();
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

    public List<String> saveList(List<Feature> features) {
        List<String> ids = new ArrayList<>();
        List<Document> docs = new ArrayList<>();
        Date now = new Date();
        
        for (Feature feature : features) {
            feature.setCreatedAt(now);
            feature.setUpdatedAt(now);
            docs.add(convertToDocument(feature));
        }
        
        try {
            MongoCollection<Document> collection = getCollection();
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
            .append("source_title", feature.getSource_title())
            .append("filePath", feature.getFilePath())
            .append("embedding", feature.getEmbedding())
            .append("createdAt", feature.getCreatedAt())
            .append("updatedAt", feature.getUpdatedAt());
    }
}
