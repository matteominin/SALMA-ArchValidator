package com.matteominin.pdf_extractor.repository;

import com.matteominin.pdf_extractor.config.DBManager;
import com.matteominin.pdf_extractor.model.CoverageReport;
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
public class CoverageReportRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(CoverageReportRepository.class);
    
    @Value("${app.mongodb.collection.coverage:coverage_reports}")
    private String collectionName;
    
    @Autowired
    private DBManager dbManager;
    
    private MongoCollection<Document> getCollection() {
        return dbManager.getCollection(collectionName);
    }
    
    private MongoCollection<Document> getCustomCollection(String customCollectionName) {
        return dbManager.getCollection(customCollectionName);
    }

    public List<CoverageReport> findAll() {
        try {
            MongoCollection<Document> collection = getCollection();
            return collection.find()
                .map(this::convertToCoverageReport)
                .into(new ArrayList<>());
        } catch (Exception e) {
            logger.error("Error retrieving coverage reports: {}", e.getMessage(), e);
            throw new RuntimeException("Database retrieval operation failed", e);
        }
    }

    public CoverageReport findById(String id) {
        try {
            MongoCollection<Document> collection = getCollection();
            Document doc = collection.find(new Document("_id", new ObjectId(id))).first();
            return doc != null ? convertToCoverageReport(doc) : null;
        } catch (Exception e) {
            logger.error("Error retrieving coverage report by id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Database retrieval operation failed", e);
        }
    }

    public String save(CoverageReport coverageReport) {
        try {
            coverageReport.setCreatedAt(new Date());
            coverageReport.setUpdatedAt(new Date());
            
            MongoCollection<Document> collection = getCollection();
            Document doc = convertToDocument(coverageReport);
            InsertOneResult result = collection.insertOne(doc);
            
            if (result.wasAcknowledged()) {
                ObjectId insertedId = result.getInsertedId().asObjectId().getValue();
                coverageReport.setId(insertedId.toString());
                logger.debug("Coverage report saved successfully with ID: {}", insertedId);
                return insertedId.toString();
            } else {
                throw new RuntimeException("Failed to save coverage report to database");
            }
        } catch (Exception e) {
            logger.error("Error saving coverage report: {}", e.getMessage(), e);
            throw new RuntimeException("Database save operation failed", e);
        }
    }
    
    public String saveToCustomCollection(CoverageReport coverageReport, String customCollectionName, String documentName) {
        try {
            coverageReport.setCreatedAt(new Date());
            coverageReport.setUpdatedAt(new Date());
            coverageReport.setReportName(documentName);
            
            MongoCollection<Document> collection = getCustomCollection(customCollectionName);
            Document doc = convertToDocument(coverageReport);
            
            // Replace existing document with same reportName or insert new one
            collection.replaceOne(
                new Document("reportName", documentName),
                doc,
                new com.mongodb.client.model.ReplaceOptions().upsert(true)
            );
            
            logger.debug("Coverage report saved to collection '{}' with document name '{}'", customCollectionName, documentName);
            return documentName; // Return document name as identifier
            
        } catch (Exception e) {
            logger.error("Error saving coverage report to custom collection: {}", e.getMessage(), e);
            throw new RuntimeException("Database save operation failed", e);
        }
    }

    private CoverageReport convertToCoverageReport(Document doc) {
        CoverageReport report = new CoverageReport();
        report.setId(doc.getObjectId("_id").toString());
        report.setProvidedFeatures(doc.getInteger("providedFeatures", 0));
        report.setSuccess(doc.getBoolean("success", false));
        report.setThreshold(doc.getDouble("threshold"));
        report.setTotalSummaryFeatures(doc.getInteger("totalSummaryFeatures", 0));
        report.setReportName(doc.getString("reportName"));
        report.setDescription(doc.getString("description"));
        report.setCreatedAt(doc.getDate("createdAt"));
        report.setUpdatedAt(doc.getDate("updatedAt"));
        
        // Convert coverage with full details
        Document coverageDoc = doc.get("coverage", Document.class);
        if (coverageDoc != null) {
            report.setCoverage(convertDocumentToCoverage(coverageDoc));
        }
        
        return report;
    }
    
    private com.matteominin.pdf_extractor.model.Coverage convertDocumentToCoverage(Document coverageDoc) {
        com.matteominin.pdf_extractor.model.Coverage coverage = 
            com.matteominin.pdf_extractor.model.Coverage.builder().build();
        
        coverage.setCoveragePercentage(coverageDoc.getDouble("coveragePercentage"));
        coverage.setCoveredCount(coverageDoc.getInteger("coveredCount", 0));
        coverage.setUncoveredCount(coverageDoc.getInteger("uncoveredCount", 0));
        
        // Convert covered features
        List<Document> coveredFeaturesDocs = coverageDoc.getList("coveredFeatures", Document.class);
        if (coveredFeaturesDocs != null) {
            for (Document coveredDoc : coveredFeaturesDocs) {
                com.matteominin.pdf_extractor.model.CoveredFeature coveredFeature = 
                    convertDocumentToCoveredFeature(coveredDoc);
                coverage.addCoveredFeature(coveredFeature);
            }
        }
        
        // Convert uncovered features
        List<Document> uncoveredFeaturesDocs = coverageDoc.getList("uncoveredFeatures", Document.class);
        if (uncoveredFeaturesDocs != null) {
            for (Document uncoveredDoc : uncoveredFeaturesDocs) {
                com.matteominin.pdf_extractor.model.UncoveredFeature uncoveredFeature = 
                    convertDocumentToUncoveredFeature(uncoveredDoc);
                coverage.addUncoveredFeature(uncoveredFeature);
            }
        }
        
        return coverage;
    }
    
    private com.matteominin.pdf_extractor.model.CoveredFeature convertDocumentToCoveredFeature(Document doc) {
        com.matteominin.pdf_extractor.model.CoveredFeature.CoveredFeatureBuilder builder = 
            com.matteominin.pdf_extractor.model.CoveredFeature.builder();
        
        // Reference feature
        Document refDoc = doc.get("referenceFeature", Document.class);
        if (refDoc != null) {
            com.matteominin.pdf_extractor.model.ReferenceFeature refFeature = 
                com.matteominin.pdf_extractor.model.ReferenceFeature.builder()
                    .feature(refDoc.getString("feature"))
                    .description(refDoc.getString("description"))
                    .build();
            builder.referenceFeature(refFeature);
        }
        
        // Matched feature
        Document matchedDoc = doc.get("matchedFeature", Document.class);
        if (matchedDoc != null) {
            com.matteominin.pdf_extractor.model.MatchedFeature matchedFeature = 
                com.matteominin.pdf_extractor.model.MatchedFeature.builder()
                    .feature(matchedDoc.getString("feature"))
                    .description(matchedDoc.getString("description"))
                    .sectionText(matchedDoc.getString("sectionText"))
                    .build();
            builder.matchedFeature(matchedFeature);
        }
        
        builder.similarity(doc.getDouble("similarity"));
        return builder.build();
    }
    
    private com.matteominin.pdf_extractor.model.UncoveredFeature convertDocumentToUncoveredFeature(Document doc) {
        com.matteominin.pdf_extractor.model.UncoveredFeature.UncoveredFeatureBuilder builder = 
            com.matteominin.pdf_extractor.model.UncoveredFeature.builder();
        
        // Reference feature
        Document refDoc = doc.get("referenceFeature", Document.class);
        if (refDoc != null) {
            com.matteominin.pdf_extractor.model.ReferenceFeature refFeature = 
                com.matteominin.pdf_extractor.model.ReferenceFeature.builder()
                    .feature(refDoc.getString("feature"))
                    .description(refDoc.getString("description"))
                    .build();
            builder.referenceFeature(refFeature);
        }
        
        // Matched feature (usually null for uncovered)
        Document matchedDoc = doc.get("matchedFeature", Document.class);
        if (matchedDoc != null) {
            com.matteominin.pdf_extractor.model.MatchedFeature matchedFeature = 
                com.matteominin.pdf_extractor.model.MatchedFeature.builder()
                    .feature(matchedDoc.getString("feature"))
                    .description(matchedDoc.getString("description"))
                    .sectionText(matchedDoc.getString("sectionText"))
                    .build();
            builder.matchedFeature(matchedFeature);
        }
        
        builder.similarity(doc.getDouble("similarity"));
        return builder.build();
    }
    
    private Document convertToDocument(CoverageReport report) {
        return new Document()
            .append("providedFeatures", report.getProvidedFeatures())
            .append("success", report.isSuccess())
            .append("threshold", report.getThreshold())
            .append("totalSummaryFeatures", report.getTotalSummaryFeatures())
            .append("reportName", report.getReportName())
            .append("description", report.getDescription())
            .append("coverage", convertCoverageToDocument(report.getCoverage()))
            .append("createdAt", report.getCreatedAt())
            .append("updatedAt", report.getUpdatedAt());
    }
    
    private Document convertCoverageToDocument(com.matteominin.pdf_extractor.model.Coverage coverage) {
        if (coverage == null) return null;
        
        Document coverageDoc = new Document()
            .append("coveragePercentage", coverage.getCoveragePercentage())
            .append("coveredCount", coverage.getCoveredCount())
            .append("uncoveredCount", coverage.getUncoveredCount());
        
        // Convert covered features with full details
        List<Document> coveredFeaturesDocs = new ArrayList<>();
        if (coverage.getCoveredFeatures() != null) {
            for (com.matteominin.pdf_extractor.model.CoveredFeature coveredFeature : coverage.getCoveredFeatures()) {
                Document coveredDoc = new Document();
                
                // Reference feature details
                if (coveredFeature.getReferenceFeature() != null) {
                    Document refDoc = new Document()
                        .append("feature", coveredFeature.getReferenceFeature().getFeature())
                        .append("description", coveredFeature.getReferenceFeature().getDescription());
                    coveredDoc.append("referenceFeature", refDoc);
                }
                
                // Matched feature details
                if (coveredFeature.getMatchedFeature() != null) {
                    Document matchedDoc = new Document()
                        .append("feature", coveredFeature.getMatchedFeature().getFeature())
                        .append("description", coveredFeature.getMatchedFeature().getDescription())
                        .append("sectionText", coveredFeature.getMatchedFeature().getSectionText());
                    coveredDoc.append("matchedFeature", matchedDoc);
                }
                
                coveredDoc.append("similarity", coveredFeature.getSimilarity());
                coveredFeaturesDocs.add(coveredDoc);
            }
        }
        coverageDoc.append("coveredFeatures", coveredFeaturesDocs);
        
        // Convert uncovered features with full details
        List<Document> uncoveredFeaturesDocs = new ArrayList<>();
        if (coverage.getUncoveredFeatures() != null) {
            for (com.matteominin.pdf_extractor.model.UncoveredFeature uncoveredFeature : coverage.getUncoveredFeatures()) {
                Document uncoveredDoc = new Document();
                
                // Reference feature details
                if (uncoveredFeature.getReferenceFeature() != null) {
                    Document refDoc = new Document()
                        .append("feature", uncoveredFeature.getReferenceFeature().getFeature())
                        .append("description", uncoveredFeature.getReferenceFeature().getDescription());
                    uncoveredDoc.append("referenceFeature", refDoc);
                }
                
                // Matched feature (usually null for uncovered, but include if present)
                if (uncoveredFeature.getMatchedFeature() != null) {
                    Document matchedDoc = new Document()
                        .append("feature", uncoveredFeature.getMatchedFeature().getFeature())
                        .append("description", uncoveredFeature.getMatchedFeature().getDescription())
                        .append("sectionText", uncoveredFeature.getMatchedFeature().getSectionText());
                    uncoveredDoc.append("matchedFeature", matchedDoc);
                }
                
                uncoveredDoc.append("similarity", uncoveredFeature.getSimilarity());
                uncoveredFeaturesDocs.add(uncoveredDoc);
            }
        }
        coverageDoc.append("uncoveredFeatures", uncoveredFeaturesDocs);
        
        return coverageDoc;
    }
}
