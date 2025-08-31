package com.matteominin.pdf_extractor.util;

import smile.math.distance.Distance;

/**
 * Custom implementation of cosine distance for double arrays.
 * Cosine distance = 1 - cosine similarity
 * Used for measuring similarity between embedding vectors.
 */
public class CosineDistance implements Distance<double[]> {
    
    @Override
    public double d(double[] a, double[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        
        // Handle zero vectors
        if (normA == 0 || normB == 0) {
            return 1.0;
        }
        
        // Cosine distance = 1 - cosine similarity
        return 1.0 - (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
