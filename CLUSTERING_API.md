# Feature Clustering API Documentation

## Overview
The PDF Feature Extractor now includes advanced clustering functionality that groups features based on the cosine similarity of their embeddings. This is useful for identifying similar content across different PDF documents.

## New Endpoints

### 1. Cluster Features
**POST** `/api/features/cluster`

Clusters all features in the database based on cosine similarity of their embeddings.

**Parameters:**
- `threshold` (optional, default: 0.85): Cosine similarity threshold (0.0 to 1.0)
  - Higher values = more strict clustering (only very similar features grouped)
  - Lower values = more relaxed clustering (somewhat similar features grouped)

**Example Request:**
```bash
curl -X POST "http://localhost:8080/api/features/cluster?threshold=0.85"
```

**Example Response:**
```json
{
  "clusters": {
    "0": [
      {
        "id": "feature1",
        "feature": "Spring Boot application development",
        "embedding": [0.1, 0.2, 0.3, ...],
        "documentPath": "/path/to/doc1.pdf"
      },
      {
        "id": "feature2", 
        "feature": "Spring Boot configuration",
        "embedding": [0.12, 0.18, 0.31, ...],
        "documentPath": "/path/to/doc2.pdf"
      }
    ],
    "1": [
      {
        "id": "feature3",
        "feature": "Database design patterns",
        "embedding": [0.5, 0.6, 0.7, ...],
        "documentPath": "/path/to/doc3.pdf"
      }
    ]
  },
  "statistics": {
    "totalFeatures": 3,
    "totalClusters": 2,
    "largestClusterSize": 2,
    "averageClusterSize": 1.5,
    "threshold": 0.85
  }
}
```

### 2. Clustering Statistics
**GET** `/api/features/cluster/stats`

Returns clustering statistics without the actual feature data. Useful for analysis and preview.

**Parameters:**
- `threshold` (optional, default: 0.85): Cosine similarity threshold

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/features/cluster/stats?threshold=0.85"
```

**Example Response:**
```json
{
  "totalFeatures": 150,
  "totalClusters": 45,
  "largestClusterSize": 8,
  "averageClusterSize": 3.33,
  "threshold": 0.85
}
```

## How It Works

### Cosine Similarity Clustering
1. **Feature Retrieval**: Gets all features that have embeddings from MongoDB
2. **Similarity Calculation**: Computes cosine similarity between embedding vectors
3. **Cluster Formation**: Groups features with similarity ≥ threshold
4. **Single Assignment**: Each feature belongs to exactly one cluster

### Cosine Similarity Formula
The cosine similarity between two vectors A and B is calculated as:
```
similarity = (A · B) / (||A|| × ||B||)
```
Where:
- A · B is the dot product
- ||A|| and ||B|| are the vector norms
- Result ranges from 0.0 (completely different) to 1.0 (identical)

### Threshold Guidelines
- **0.95-1.0**: Very strict - only nearly identical features
- **0.85-0.95**: Strict - similar features (recommended default)
- **0.70-0.85**: Moderate - somewhat related features  
- **0.50-0.70**: Relaxed - loosely related features
- **<0.50**: Very relaxed - may group unrelated features

## Performance Considerations

### Efficient Data Access
The implementation includes several optimization strategies:

1. **Filtered Queries**: Only retrieves features with embeddings
   ```java
   // Only gets features that have embeddings
   findAllWithEmbeddings()
   ```

2. **Minimal Data Projection**: Option to get only essential fields
   ```java
   // Gets only id, feature text, and embedding
   findEmbeddingsOnly()
   ```

3. **Streaming Support**: For large datasets
   ```java
   // Streams features to avoid memory issues
   streamAllWithEmbeddings()
   ```

### Algorithm Complexity
- **Time Complexity**: O(n²) where n = number of features
- **Space Complexity**: O(n) for storing features and clusters
- **Recommendation**: For >10,000 features, consider batching or more advanced clustering algorithms

## Error Handling

### Common Error Responses

**Invalid Threshold:**
```json
{
  "error": "Threshold must be between 0.0 and 1.0"
}
```

**Clustering Failure:**
```json
{
  "error": "Failed to cluster features: [specific error message]"
}
```

**No Features Found:**
Returns empty clusters:
```json
{
  "clusters": {},
  "statistics": {
    "totalFeatures": 0,
    "totalClusters": 0,
    "largestClusterSize": 0,
    "averageClusterSize": 0.0,
    "threshold": 0.85
  }
}
```

## Usage Examples

### Example 1: Find Very Similar Features
```bash
# High threshold for very similar content
curl -X POST "http://localhost:8080/api/features/cluster?threshold=0.95"
```

### Example 2: Broad Grouping
```bash  
# Lower threshold for broader categories
curl -X POST "http://localhost:8080/api/features/cluster?threshold=0.70"
```

### Example 3: Preview Clustering
```bash
# Check statistics before running full clustering
curl -X GET "http://localhost:8080/api/features/cluster/stats?threshold=0.85"
```

## Integration with Existing Workflow

1. **Extract Features**: Use existing PDF extraction endpoints
2. **Generate Embeddings**: Use `/api/features/embed-batch` endpoint  
3. **Cluster Features**: Use `/api/features/cluster` endpoint
4. **Analyze Results**: Review clusters for similar content patterns

This clustering functionality enables powerful content analysis across your PDF knowledge base, helping identify patterns, duplicates, and related topics automatically.
