package com.matteominin.pdf_extractor.service;

import com.matteominin.pdf_extractor.model.Feature;
import com.matteominin.pdf_extractor.repository.FeatureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FeatureClusteringService {

    @Autowired
    private FeatureRepository featureRepository;

    /**
     * Clusters features based on cosine similarity with the given threshold.
     * Each feature belongs to at most one cluster.
     * 
     * @param threshold cosine similarity threshold (0.0 to 1.0)
     * @return Map where key is cluster ID and value is list of features in that cluster
     */
    public List<List<Feature>> clusterFeatures(double threshold) {
        List<Feature> features = featureRepository.findAllWithEmbeddings();
        
        if (features.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<Feature>> clusters = new ArrayList<>();
        Set<String> assignedFeatures = new HashSet<>();

        for (Feature feature : features) {
            if (assignedFeatures.contains(feature.getId())) {
                continue; // Already assigned to a cluster
            }

            List<Feature> cluster = new ArrayList<>();
            cluster.add(feature);
            assignedFeatures.add(feature.getId());

            // Find all similar features for this cluster
            for (Feature otherFeature : features) {
                if (assignedFeatures.contains(otherFeature.getId())) {
                    continue; // Already assigned
                }

                double similarity = EmbeddingService.calculateCosineSimilarity(feature.getEmbedding(), otherFeature.getEmbedding());
                if (similarity >= threshold) {
                    cluster.add(otherFeature);
                    assignedFeatures.add(otherFeature.getId());
                }
            }
            Set<String> clusterDocs = new HashSet<>(cluster.stream().map(Feature::getFilePath).toList());
            if(clusterDocs.size() > 3*2/3)    // TODO: insert the number of documents
                clusters.add(removeEmbeddings(cluster));
        }

        return clusters;
    }

    /**
     * Returns a list of features with embedding set to null (for response serialization).
     */
    private List<Feature> removeEmbeddings(List<Feature> features) {
        List<Feature> result = new ArrayList<>();
        for (Feature f : features) {
            Feature copy = Feature.builder()
                .id(f.getId())
                .feature(f.getFeature())
                .description(f.getDescription())
                .category(f.getCategory())
                .evidence(f.getEvidence())
                .confidence(f.getConfidence())
                .source_title(f.getSource_title())
                .embedding(null)
                .filePath(f.getFilePath())
                .createdAt(f.getCreatedAt())
                .updatedAt(f.getUpdatedAt())
                .build();
            result.add(copy);
        }
        return result;
    }

    /**
     * Gets clustering statistics.
     * 
     * @param threshold cosine similarity threshold
     * @return statistics about the clustering result
     */
    public ClusteringStats getClusteringStats(double threshold) {
        List<List<Feature>> clusters = clusterFeatures(threshold);
        
        int totalFeatures = clusters.stream().mapToInt(List::size).sum();
        int totalClusters = clusters.size();
        int largestClusterSize = clusters.stream().mapToInt(List::size).max().orElse(0);
        double averageClusterSize = totalClusters > 0 ? (double) totalFeatures / totalClusters : 0.0;

        return new ClusteringStats(totalFeatures, totalClusters, largestClusterSize, averageClusterSize, threshold);
    }

    /**
     * Data class for clustering statistics.
     */
    public static class ClusteringStats {
        private final int totalFeatures;
        private final int totalClusters;
        private final int largestClusterSize;
        private final double averageClusterSize;
        private final double threshold;

        public ClusteringStats(int totalFeatures, int totalClusters, int largestClusterSize, 
                             double averageClusterSize, double threshold) {
            this.totalFeatures = totalFeatures;
            this.totalClusters = totalClusters;
            this.largestClusterSize = largestClusterSize;
            this.averageClusterSize = averageClusterSize;
            this.threshold = threshold;
        }

        public int getTotalFeatures() { return totalFeatures; }
        public int getTotalClusters() { return totalClusters; }
        public int getLargestClusterSize() { return largestClusterSize; }
        public double getAverageClusterSize() { return averageClusterSize; }
        public double getThreshold() { return threshold; }
    }
}
