package org.vidyaastra.protege.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Simple in-memory vector store for RAG operations
 * Stores vectors and performs similarity search using cosine similarity
 */
public class InMemoryVectorStore implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(InMemoryVectorStore.class);
    
    private final String collectionName;
    private final Map<Long, VectorEntry> vectors;
    private final AtomicLong idGenerator;
    
    public InMemoryVectorStore(String collectionName) {
        this.collectionName = collectionName;
        this.vectors = new ConcurrentHashMap<>();
        this.idGenerator = new AtomicLong(0);
        logger.info("Created in-memory vector store: {}", collectionName);
    }
    
    /**
     * Add vectors to the store
     */
    public void upsertVectors(List<VectorData> vectorDataList) {
        for (VectorData data : vectorDataList) {
            long id = idGenerator.incrementAndGet();
            vectors.put(id, new VectorEntry(id, data.getVector(), data.getPayload()));
        }
        logger.info("Added {} vectors to collection {}", vectorDataList.size(), collectionName);
    }
    
    /**
     * Search for similar vectors using cosine similarity
     */
    public List<SearchResult> search(List<Float> queryVector, int limit) {
        if (vectors.isEmpty()) {
            logger.warn("No vectors in collection {} to search", collectionName);
            return new ArrayList<>();
        }
        
        // Calculate similarity for all vectors
        List<SearchResult> results = vectors.values().stream()
            .map(entry -> {
                float similarity = cosineSimilarity(queryVector, entry.vector);
                return new SearchResult(similarity, entry.payload);
            })
            .sorted((a, b) -> Float.compare(b.getScore(), a.getScore())) // Sort by score descending
            .limit(limit)
            .collect(Collectors.toList());
        
        logger.info("Found {} similar vectors (limit: {})", results.size(), limit);
        return results;
    }
    
    /**
     * Calculate cosine similarity between two vectors
     */
    private float cosineSimilarity(List<Float> vec1, List<Float> vec2) {
        if (vec1.size() != vec2.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;
        
        for (int i = 0; i < vec1.size(); i++) {
            dotProduct += vec1.get(i) * vec2.get(i);
            norm1 += vec1.get(i) * vec1.get(i);
            norm2 += vec2.get(i) * vec2.get(i);
        }
        
        if (norm1 == 0.0f || norm2 == 0.0f) {
            return 0.0f;
        }
        
        return dotProduct / (float)(Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * Get collection statistics
     */
    public CollectionStats getStats() {
        return new CollectionStats(vectors.size(), vectors.size());
    }
    
    /**
     * Clear all vectors from the store
     */
    public void clear() {
        vectors.clear();
        logger.info("Cleared all vectors from collection {}", collectionName);
    }
    
    @Override
    public void close() {
        vectors.clear();
        logger.info("Closed in-memory vector store: {}", collectionName);
    }
    
    /**
     * Internal representation of a vector entry
     */
    private static class VectorEntry {
        final long id;
        final List<Float> vector;
        final Map<String, Object> payload;
        
        VectorEntry(long id, List<Float> vector, Map<String, Object> payload) {
            this.id = id;
            this.vector = vector;
            this.payload = payload;
        }
    }
    
    /**
     * Represents vector data with payload
     */
    public static class VectorData {
        private final List<Float> vector;
        private final Map<String, Object> payload;
        
        public VectorData(List<Float> vector, Map<String, Object> payload) {
            this.vector = vector;
            this.payload = payload;
        }
        
        public List<Float> getVector() {
            return vector;
        }
        
        public Map<String, Object> getPayload() {
            return payload;
        }
    }
    
    /**
     * Represents a search result
     */
    public static class SearchResult {
        private final float score;
        private final Map<String, Object> payload;
        
        public SearchResult(float score, Map<String, Object> payload) {
            this.score = score;
            this.payload = payload;
        }
        
        public float getScore() {
            return score;
        }
        
        public Map<String, Object> getPayload() {
            return payload;
        }
    }
    
    /**
     * Collection statistics
     */
    public static class CollectionStats {
        private final long pointsCount;
        private final long vectorsCount;
        
        public CollectionStats(long pointsCount, long vectorsCount) {
            this.pointsCount = pointsCount;
            this.vectorsCount = vectorsCount;
        }
        
        public long getPointsCount() {
            return pointsCount;
        }
        
        public long getVectorsCount() {
            return vectorsCount;
        }
    }
}
