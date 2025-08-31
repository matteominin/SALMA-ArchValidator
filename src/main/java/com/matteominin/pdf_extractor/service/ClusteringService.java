package com.matteominin.pdf_extractor.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.matteominin.pdf_extractor.util.CosineDistance;

import smile.clustering.DBSCAN;

public class ClusteringService {    
    /**
     * Clusters embeddings using cosine similarity with DBSCAN algorithm.
     * This method groups similar embeddings together based on their cosine distance.
     * 
     * @param ids List of identifiers corresponding to each embedding
     * @param embeddings 2D array of embedding vectors
     * @param epsilon Maximum cosine distance for points to be in the same neighborhood (0.0 to 2.0)
     * @param minPts Minimum number of points in a neighborhood to form a cluster
     * @return List of clusters, where each cluster contains the IDs of similar embeddings
     */
    public static List<List<String>> clusterWithCosineSimilarity(List<String> ids, double[][] embeddings, 
                                                                 double epsilon, int minPts) {
        if (ids.size() != embeddings.length) {
            throw new IllegalArgumentException("Number of IDs must match number of embeddings");
        }
        
        // Use DBSCAN with CosineDistance for clustering
        DBSCAN<double[]> dbscan = DBSCAN.fit(embeddings, new CosineDistance(), minPts, epsilon);
        int[] labels = dbscan.y;
        
        // Group IDs by cluster labels
        Map<Integer, List<String>> clusterMap = new HashMap<>();
        List<String> noisePoints = new ArrayList<>();
        
        for (int i = 0; i < labels.length; i++) {
            int clusterLabel = labels[i];
            String id = ids.get(i);
            
            if (clusterLabel == -1) {
                // Noise point (not assigned to any cluster)
                noisePoints.add(id);
            } else {
                // Add to corresponding cluster
                clusterMap.computeIfAbsent(clusterLabel, k -> new ArrayList<>()).add(id);
            }
        }
        
        // Convert clusters to list format
        List<List<String>> clusters = new ArrayList<>(clusterMap.values());
        
        return clusters;
    }
    
    /**
     * Convenience method for clustering with default cosine similarity parameters.
     * Uses epsilon=0.15 (85% similarity threshold) and minPts=2.
     * 
     * @param ids List of identifiers corresponding to each embedding
     * @param embeddings 2D array of embedding vectors
     * @return List of clusters grouped by cosine similarity
     */
    public static List<List<String>> clusterWithCosineSimilarity(List<String> ids, double[][] embeddings) {
        // Default parameters: epsilon=0.15 means ~85% cosine similarity threshold
        // minPts=2 means at least 2 points needed to form a cluster
        return clusterWithCosineSimilarity(ids, embeddings, 0.15, 2);
    }
}
