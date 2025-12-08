package org.vidyaastra.protege.rag;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.JsonWithInt.Value;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.Vectors;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import io.qdrant.client.grpc.Points.PointId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Qdrant vector store - supports Qdrant Cloud with API key authentication
 */
public class QdrantVectorStore implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(QdrantVectorStore.class);
    
    private final String collectionName;
    private final QdrantClient client;
    private final int vectorDimension;
    private long pointIdCounter = 0;
    
    /**
     * Constructor with API key support for Qdrant Cloud
     * 
     * @param collectionName Name of the collection
     * @param vectorDimension Dimension of the vectors (e.g., 1536 for OpenAI embeddings)
     * @param url Cloud URL (e.g., "xyz.gcp.cloud.qdrant.io:6334" or "https://xyz.gcp.cloud.qdrant.io:6334")
     * @param apiKey API key for Qdrant Cloud
     */
    public QdrantVectorStore(String collectionName, int vectorDimension, String url, String apiKey) {
        this.collectionName = collectionName;
        this.vectorDimension = vectorDimension;
        
        try {
            // Parse URL - remove http/https prefix
            String host = url.replace("http://", "").replace("https://", "");
            int port = 6334; // Default gRPC port
            
            if (host.contains(":")) {
                String[] parts = host.split(":");
                host = parts[0];
                port = Integer.parseInt(parts[1]);
            }
            
            logger.info("🌐 Connecting to Qdrant Cloud at: {}:{}", host, port);
            
            // Build client with TLS (required for cloud)
            QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(host, port, true);
            
            if (apiKey != null && !apiKey.isEmpty()) {
                builder.withApiKey(apiKey);
                logger.info("🔑 Using API key authentication");
            }
            
            this.client = new QdrantClient(builder.build());
            logger.info("✅ Connected to Qdrant Cloud successfully");
            
            // Create collection if it doesn't exist
            initializeCollection();
            
        } catch (Exception e) {
            logger.error("Failed to initialize Qdrant client", e);
            throw new RuntimeException("Failed to connect to Qdrant: " + e.getMessage(), e);
        }
    }
    
    /**
     * Constructor with default dimension (1536 for OpenAI embeddings)
     */
    public QdrantVectorStore(String collectionName, String url, String apiKey) {
        this(collectionName, 1536, url, apiKey);
    }
    
    private void initializeCollection() throws ExecutionException, InterruptedException {
        // Check if collection exists
        try {
            var info = client.getCollectionInfoAsync(collectionName).get();
            logger.info("Collection '{}' already exists with {} points", collectionName, info.getPointsCount());
            return;
        } catch (Exception e) {
            logger.debug("Collection doesn't exist, will create new one", e);
        }
        
        // Collection doesn't exist, create it
        logger.info("Creating new collection: {}", collectionName);
        
        VectorParams vectorParams = VectorParams.newBuilder()
            .setSize(vectorDimension)
            .setDistance(Distance.Cosine)
            .build();
        
        client.createCollectionAsync(collectionName, vectorParams).get();
        
        logger.info("✅ Created collection: {}", collectionName);
    }
    
    /**
     * Add vectors to the store
     */
    public void upsert(List<VectorData> vectorDataList) {
        try {
            // Ensure collection exists before upserting
            ensureCollectionExists();
            
            List<PointStruct> points = new ArrayList<>();
            
            for (VectorData data : vectorDataList) {
                pointIdCounter++;
                
                // Convert float[] to List<Float>
                List<Float> vectorList = data.getVector();
                
                // Build vectors
                io.qdrant.client.grpc.Points.Vector vector = io.qdrant.client.grpc.Points.Vector.newBuilder()
                    .addAllData(vectorList)
                    .build();
                
                // Build point with vectors and payload  
                PointStruct point = PointStruct.newBuilder()
                    .setId(PointId.newBuilder().setNum(pointIdCounter).build())
                    .setVectors(Vectors.newBuilder().setVector(vector).build())
                    .putAllPayload(convertMetadataToPayload(data.getMetadata()))
                    .build();
                
                points.add(point);
            }
            
            // Upsert points to Qdrant
            client.upsertAsync(collectionName, points).get();
            
            logger.info("✅ Added {} vectors to collection {}", vectorDataList.size(), collectionName);
            
        } catch (Exception e) {
            logger.error("Failed to upsert vectors", e);
            throw new RuntimeException("Failed to upsert vectors to Qdrant", e);
        }
    }
    
    /**
     * Search for similar vectors using Qdrant's optimized similarity search
     */
    public List<SearchResult> search(float[] queryVector, int limit) {
        try {
            // Convert float[] to List<Float>
            List<Float> queryVectorList = new ArrayList<>(queryVector.length);
            for (float v : queryVector) {
                queryVectorList.add(v);
            }
            
            // Build search request
            SearchPoints searchPoints = SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(queryVectorList)
                .setLimit(limit)
                .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build())
                .build();
            
            // Perform search
            List<ScoredPoint> searchResults = client.searchAsync(searchPoints).get();
            
            // Convert results to our format
            List<SearchResult> results = new ArrayList<>();
            for (ScoredPoint scoredPoint : searchResults) {
                Map<String, Object> metadata = convertPayloadToMetadata(scoredPoint.getPayloadMap());
                results.add(new SearchResult(scoredPoint.getScore(), metadata));
            }
            
            logger.info("Found {} similar vectors (limit: {})", results.size(), limit);
            return results;
            
        } catch (Exception e) {
            logger.error("Failed to search vectors", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Get collection statistics
     */
    public CollectionStats getStats() {
        try {
            // Ensure collection exists
            ensureCollectionExists();
            
            var info = client.getCollectionInfoAsync(collectionName).get();
            return new CollectionStats(info.getPointsCount(), info.getPointsCount());
        } catch (Exception e) {
            logger.error("Failed to get collection stats", e);
            return new CollectionStats(0, 0);
        }
    }
    
    /**
     * Ensure collection exists, create if needed
     */
    private void ensureCollectionExists() {
        try {
            client.getCollectionInfoAsync(collectionName).get();
        } catch (Exception e) {
            // Collection doesn't exist, create it
            try {
                logger.info("Collection '{}' doesn't exist, creating...", collectionName);
                VectorParams vectorParams = VectorParams.newBuilder()
                    .setSize(vectorDimension)
                    .setDistance(Distance.Cosine)
                    .build();
                client.createCollectionAsync(collectionName, vectorParams).get();
                logger.info("✅ Created collection: {}", collectionName);
            } catch (Exception ex) {
                logger.error("Failed to create collection", ex);
                throw new RuntimeException("Failed to create collection: " + ex.getMessage(), ex);
            }
        }
    }
    
    /**
     * Clear all vectors from the collection
     */
    public void clear() {
        try {
            // Delete and recreate collection
            client.deleteCollectionAsync(collectionName).get();
            initializeCollection();
            pointIdCounter = 0;
            logger.info("Cleared all vectors from collection {}", collectionName);
        } catch (Exception e) {
            logger.error("Failed to clear collection", e);
        }
    }
    
    @Override
    public void close() {
        try {
            client.close();
            logger.info("Closed Qdrant vector store: {}", collectionName);
        } catch (Exception e) {
            logger.error("Error closing Qdrant client", e);
        }
    }
    
    // Helper methods for value conversion
    
    /**
     * Convert metadata to Qdrant payload
     */
    private Map<String, Value> convertMetadataToPayload(Map<String, Object> metadata) {
        Map<String, Value> payload = new HashMap<>();
        
        if (metadata != null) {
            for (Map.Entry<String, Object> entry : metadata.entrySet()) {
                Object value = entry.getValue();
                
                if (value instanceof String) {
                    payload.put(entry.getKey(), ValueFactory.value((String) value));
                } else if (value instanceof Integer) {
                    payload.put(entry.getKey(), ValueFactory.value((Integer) value));
                } else if (value instanceof Long) {
                    payload.put(entry.getKey(), ValueFactory.value((Long) value));
                } else if (value instanceof Double) {
                    payload.put(entry.getKey(), ValueFactory.value((Double) value));
                } else if (value instanceof Float) {
                    payload.put(entry.getKey(), ValueFactory.value((Float) value));
                } else if (value instanceof Boolean) {
                    payload.put(entry.getKey(), ValueFactory.value((Boolean) value));
                } else if (value != null) {
                    payload.put(entry.getKey(), ValueFactory.value(value.toString()));
                }
            }
        }
        
        return payload;
    }
    
    /**
     * Convert Qdrant payload to metadata
     */
    private Map<String, Object> convertPayloadToMetadata(Map<String, Value> payload) {
        Map<String, Object> metadata = new HashMap<>();
        
        for (Map.Entry<String, Value> entry : payload.entrySet()) {
            Value value = entry.getValue();
            
            if (value.hasStringValue()) {
                metadata.put(entry.getKey(), value.getStringValue());
            } else if (value.hasIntegerValue()) {
                metadata.put(entry.getKey(), value.getIntegerValue());
            } else if (value.hasDoubleValue()) {
                metadata.put(entry.getKey(), value.getDoubleValue());
            } else if (value.hasBoolValue()) {
                metadata.put(entry.getKey(), value.getBoolValue());
            }
        }
        
        return metadata;
    }
    
    /**
     * Represents vector data with metadata
     */
    public static class VectorData {
        private final String id;
        private final float[] vector;
        private final Map<String, Object> metadata;
        
        public VectorData(String id, float[] vector, Map<String, Object> metadata) {
            this.id = id;
            this.vector = vector;
            this.metadata = metadata;
        }
        
        public String getId() {
            return id;
        }
        
        public List<Float> getVector() {
            List<Float> list = new ArrayList<>(vector.length);
            for (float v : vector) {
                list.add(v);
            }
            return list;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
    
    /**
     * Represents a search result
     */
    public static class SearchResult {
        private final float score;
        private final Map<String, Object> metadata;
        
        public SearchResult(float score, Map<String, Object> metadata) {
            this.score = score;
            this.metadata = metadata;
        }
        
        public float getScore() {
            return score;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
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
