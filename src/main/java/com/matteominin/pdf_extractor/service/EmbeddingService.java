package com.matteominin.pdf_extractor.service;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.ArrayList;
import java.net.HttpRetryException;

@Service
public class EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Value("${spring.ai.openai.api-key:#{null}}")
    private String apiKey;
    
    public List<Double> generateEmbedding(String text) {
        // Prima controlliamo se l'API key è configurata
        if (apiKey == null || apiKey.trim().isEmpty() || "${OPENAI_API_KEY}".equals(apiKey)) {
            logger.error("OpenAI API key is not configured. Current value: {}", 
                        apiKey == null ? "null" : (apiKey.isEmpty() ? "empty" : "placeholder"));
            throw new RuntimeException("OpenAI API key is not configured. Please set the OPENAI_API_KEY environment variable.");
        }
        
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }
        
        try {
            logger.info("Generating embedding for text: {}", text.substring(0, Math.min(50, text.length())));
            logger.debug("Using API key starting with: {}...", apiKey.substring(0, Math.min(7, apiKey.length())));
            
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            
            if (response == null) {
                throw new RuntimeException("Received null response from OpenAI API");
            }
            
            if (response.getResults() == null || response.getResults().isEmpty()) {
                throw new RuntimeException("Received empty results from OpenAI API");
            }
            
            // Get the embedding as float array and convert to List<Double>
            float[] embeddingArray = response.getResults().get(0).getOutput();
            if (embeddingArray == null || embeddingArray.length == 0) {
                throw new RuntimeException("Received empty embedding array from OpenAI API");
            }
            
            List<Double> embedding = new ArrayList<>();
            for (float f : embeddingArray) {
                embedding.add((double) f);
            }
            
            return embedding;
            
        } catch (HttpClientErrorException e) {
            logger.error("HTTP Client Error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 401) {
                throw new RuntimeException("OpenAI API authentication failed. Invalid API key. Status: " + e.getStatusCode(), e);
            } else if (e.getStatusCode().value() == 429) {
                throw new RuntimeException("OpenAI API rate limit exceeded. Status: " + e.getStatusCode(), e);
            } else {
                throw new RuntimeException("OpenAI API client error. Status: " + e.getStatusCode() + ", Response: " + e.getResponseBodyAsString(), e);
            }
        } catch (HttpServerErrorException e) {
            logger.error("HTTP Server Error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("OpenAI API server error. Status: " + e.getStatusCode() + ", Response: " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            logger.error("REST Client Exception: {}", e.getMessage(), e);
            if (e.getCause() instanceof HttpRetryException) {
                HttpRetryException retryEx = (HttpRetryException) e.getCause();
                logger.error("HTTP Retry Exception - ResponseCode: {}, Location: {}", retryEx.responseCode(), retryEx.getLocation());
                if (retryEx.getMessage().contains("authentication")) {
                    throw new RuntimeException("OpenAI API authentication failed during retry. Check your API key configuration.", e);
                }
            }
            throw new RuntimeException("Network error while calling OpenAI API: " + e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid argument: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error during embedding generation", e);
            logger.error("Exception type: {}", e.getClass().getSimpleName());
            logger.error("Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                logger.error("Caused by: {} - {}", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            throw new RuntimeException("Embedding generation failed: " + e.getMessage(), e);
        }
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
            double[] array1 = vector1.stream().mapToDouble(Double::doubleValue).toArray();
            double[] array2 = vector2.stream().mapToDouble(Double::doubleValue).toArray();
            
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
}
