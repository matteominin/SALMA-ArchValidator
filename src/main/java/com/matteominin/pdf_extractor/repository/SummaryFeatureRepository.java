package com.matteominin.pdf_extractor.repository;

import com.matteominin.pdf_extractor.config.DBManager;
import com.matteominin.pdf_extractor.model.feature.SummaryFeature;
import com.mongodb.client.MongoCollection;
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
public class SummaryFeatureRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(SummaryFeatureRepository.class);
    
    @Value("${app.mongodb.collection.summary:summary_features}")
    private String collectionName;
    
    @Autowired
    private DBManager dbManager;
    
    private MongoCollection<Document> getCollection() {
        return dbManager.getCollection(collectionName);
    }
    
    /**
     * Save a list of summary features to MongoDB.
     * 
     * @param summaryFeatures List of summary features to save
     * @return List of IDs of saved summary features
     */
    public List<String> saveAll(List<SummaryFeature> summaryFeatures) {
        List<String> ids = new ArrayList<>();
        List<Document> docs = new ArrayList<>();
        Date now = new Date();
        
        for (SummaryFeature summaryFeature : summaryFeatures) {
            docs.add(convertToDocument(summaryFeature, now));
        }
        
        try {
            MongoCollection<Document> collection = getCollection();
            collection.insertMany(docs);
            
            for (Document doc : docs) {
                ObjectId id = doc.getObjectId("_id");
                ids.add(id != null ? id.toString() : "");
            }
            
            logger.debug("Batch saved {} summary features", docs.size());
            return ids;
        } catch (Exception e) {
            logger.error("Error saving summary features: {}", e.getMessage(), e);
            throw new RuntimeException("Database batch save operation failed", e);
        }
    }
    
    /**
     * Find all summary features.
     * 
     * @return List of all summary features
     */
    public List<SummaryFeature> findAll() {
        try {
            MongoCollection<Document> collection = getCollection();
            return collection.find()
                .map(this::convertFromDocument)
                .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error retrieving summary features: {}", e.getMessage(), e);
            throw new RuntimeException("Database retrieval operation failed", e);
        }
    }
    
    /**
     * Find summary feature by ID.
     * 
     * @param id The ID of the summary feature to find
     * @return The summary feature or null if not found
     */
    public SummaryFeature findById(String id) {
        try {
            MongoCollection<Document> collection = getCollection();
            Document doc = collection.find(new Document("_id", new ObjectId(id))).first();
            return doc != null ? convertFromDocument(doc) : null;
        } catch (Exception e) {
            logger.error("Error retrieving summary feature by ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Database retrieval operation failed", e);
        }
    }

    /**
     * Delete all summary features.
     * 
     * @return number of deleted documents
     */
    public long deleteAll() {
        try {
            MongoCollection<Document> collection = getCollection();
            long deletedCount = collection.deleteMany(new Document()).getDeletedCount();
            logger.debug("Deleted {} summary features", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            logger.error("Error deleting summary features: {}", e.getMessage(), e);
            throw new RuntimeException("Database delete operation failed", e);
        }
    }
    
    /**
     * Count total summary features.
     * 
     * @return count of summary features
     */
    public long count() {
        try {
            MongoCollection<Document> collection = getCollection();
            return collection.countDocuments();
        } catch (Exception e) {
            logger.error("Error counting summary features: {}", e.getMessage(), e);
            throw new RuntimeException("Database count operation failed", e);
        }
    }
    
    private Document convertToDocument(SummaryFeature summaryFeature, Date timestamp) {
        return new Document()
            .append("feature", summaryFeature.getFeature())
            .append("description", summaryFeature.getDescription())
            .append("count", summaryFeature.getCount())
            .append("example", summaryFeature.getExample())
            .append("embedding", summaryFeature.getEmbedding())
            .append("checklist", summaryFeature.getChecklist())
            .append("createdAt", timestamp)
            .append("updatedAt", timestamp);
    }
    
    private SummaryFeature convertFromDocument(Document doc) {
        return SummaryFeature.builder()
                .id(doc.getObjectId("_id") != null ? doc.getObjectId("_id").toString() : null)
            .feature(doc.getString("feature"))
            .description(doc.getString("description"))
            .count(doc.getString("count"))
            .embedding(doc.getList("embedding", Double.class))
            .checklist(doc.getList("checklist", String.class))
            .example(doc.getString("example"))
            .build();
    }
}
