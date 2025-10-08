package com.matteominin.pdf_extractor.service;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

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
    public static List<List<String>> clusterWithDBSCAN(List<String> ids, double[][] embeddings,
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

    public static List<List<String>> pairwiseClusteringWithCosineSimilarity(List<String> ids, double[][] embeddings,
            double threshold) {
        return pairwiseClusteringWithCosineSimilarity(ids, embeddings, threshold, 0);
    }

    public static List<List<String>> pairwiseClusteringWithCosineSimilarity(List<String> ids, double[][] embeddings,
            double threshold, int minClusterSize) {
        if (ids.size() != embeddings.length) {
            throw new IllegalArgumentException("Number of IDs must match number of embeddings");
        }

        int n = ids.size();
        List<List<Integer>> graph = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            graph.add(new ArrayList<>());
        }

        // Build graph: connect nodes if cosine similarity > threshold
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double similarity = calculateCosineSimilarityFromArrays(embeddings[i], embeddings[j]);
                if (similarity > threshold) {
                    graph.get(i).add(j);
                    graph.get(j).add(i);
                }
            }
        }

        // Find connected components using DFS
        boolean[] visited = new boolean[n];
        List<List<String>> clusters = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            if (!visited[i]) {
                List<String> component = new ArrayList<>();
                dfs(i, graph, visited, ids, component);
                if (component.size() > minClusterSize) { // Only add clusters with more than minClusterSize items
                    clusters.add(component);
                }
            }
        }

        return clusters;
    }

    private static void dfs(int node, List<List<Integer>> graph, boolean[] visited, List<String> ids,
            List<String> component) {
        visited[node] = true;
        component.add(ids.get(node));
        for (int neighbor : graph.get(node)) {
            if (!visited[neighbor]) {
                dfs(neighbor, graph, visited, ids, component);
            }
        }
    }

    private static double calculateCosineSimilarityFromArrays(double[] a, double[] b) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Convenience method for clustering with default cosine similarity parameters.
     * Uses epsilon=0.15 (85% similarity threshold) and minPts=2.
     * 
     * @param ids List of identifiers corresponding to each embedding
     * @param embeddings 2D array of embedding vectors
     * @return List of clusters grouped by cosine similarity
     */
    public static List<List<String>> clusterWithDBSCAN(List<String> ids, double[][] embeddings) {
        // Default parameters: epsilon=0.15 means ~85% cosine similarity threshold
        // minPts=2 means at least 2 points needed to form a cluster
        return clusterWithDBSCAN(ids, embeddings, 0.15, 2);
    }

    /**
     * Calculates cosine similarity between two embedding vectors.
     * 
     * @param vector1 first embedding vector
     * @param vector2 second embedding vector
     * @return cosine similarity value between 0.0 and 1.0
     */
    public static double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1 == null || vector2 == null || vector1.size() != vector2.size()) {
            return 0.0;
        }

        try {
            // Convert to Apache Commons Math vectors
            double[] array1 = convertToDoubleArray(vector1);
            double[] array2 = convertToDoubleArray(vector2);

            RealVector v1 = new ArrayRealVector(array1);
            RealVector v2 = new ArrayRealVector(array2);

            // Calculate cosine similarity
            double dotProduct = v1.dotProduct(v2);
            double norm1 = v1.getNorm();
            double norm2 = v2.getNorm();

            if (norm1 == 0.0 || norm2 == 0.0) {
                return 0.0;
            }

            return dotProduct / (norm1 * norm2);
        } catch (Exception e) {
            // Log error in real application
            return 0.0;
        }
    }

    /**
     * Converts a single List<Double> embedding to double[] format.
     * 
     * @param embedding single embedding vector
     * @return double array
     */
    public static double[] convertToDoubleArray(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty()) {
            return new double[0];
        }

        return embedding.stream().mapToDouble(Double::doubleValue).toArray();
    }
}
