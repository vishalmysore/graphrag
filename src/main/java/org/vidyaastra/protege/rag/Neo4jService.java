package org.vidyaastra.protege.rag;

import org.neo4j.driver.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Service for Neo4j database operations
 */
public class Neo4jService implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(Neo4jService.class);
    
    private final Driver driver;
    private final String database;
    
    public Neo4jService(String uri, String username, String password, String database) {
        this.database = database;
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(username, password));
        
        // Test connection
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            session.run("RETURN 1").consume();
            logger.info("Successfully connected to Neo4j database: {}", database);
        } catch (Exception e) {
            logger.error("Failed to connect to Neo4j", e);
            throw new RuntimeException("Failed to connect to Neo4j: " + e.getMessage(), e);
        }
    }
    
    /**
     * Get all graph data as text representations for embedding
     */
    public List<GraphChunk> getGraphChunks() {
        List<GraphChunk> chunks = new ArrayList<>();
        
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            // Get all nodes with their properties and relationships
            String query = 
                "MATCH (n) " +
                "OPTIONAL MATCH (n)-[r]->(m) " +
                "RETURN n, labels(n) as nodeLabels, " +
                "       collect({type: type(r), target: m, targetLabels: labels(m)}) as relationships " +
                "LIMIT 1000";
            
            Result result = session.run(query);
            
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Value nodeValue = record.get("n");
                List<Object> labels = record.get("nodeLabels").asList();
                List<Object> relationships = record.get("relationships").asList();
                
                String chunk = buildTextChunk(nodeValue, labels, relationships);
                chunks.add(new GraphChunk(chunk, nodeValue.asMap()));
            }
        }
        
        logger.info("Retrieved {} graph chunks from Neo4j", chunks.size());
        return chunks;
    }
    
    /**
     * Get graph schema
     */
    public String getGraphSchema() {
        StringBuilder schema = new StringBuilder();
        
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            // Get node labels
            Result labelResult = session.run("CALL db.labels()");
            schema.append("Node Labels:\n");
            while (labelResult.hasNext()) {
                schema.append("  - ").append(labelResult.next().get(0).asString()).append("\n");
            }
            
            // Get relationship types
            Result relResult = session.run("CALL db.relationshipTypes()");
            schema.append("\nRelationship Types:\n");
            while (relResult.hasNext()) {
                schema.append("  - ").append(relResult.next().get(0).asString()).append("\n");
            }
        }
        
        return schema.toString();
    }
    
    /**
     * Execute a Cypher query
     */
    public List<Map<String, Object>> executeQuery(String cypherQuery) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try (Session session = driver.session(SessionConfig.forDatabase(database))) {
            Result result = session.run(cypherQuery);
            
            while (result.hasNext()) {
                org.neo4j.driver.Record record = result.next();
                Map<String, Object> row = new HashMap<>();
                for (String key : record.keys()) {
                    row.put(key, record.get(key).asObject());
                }
                results.add(row);
            }
        }
        
        logger.info("Query returned {} results", results.size());
        return results;
    }
    
    private String buildTextChunk(Value node, List<Object> labels, List<Object> relationships) {
        StringBuilder chunk = new StringBuilder();
        
        // Add node description
        chunk.append("Node Type: ").append(labels).append("\n");
        chunk.append("Properties: ").append(node.asMap()).append("\n");
        
        // Add relationships
        if (!relationships.isEmpty() && relationships.get(0) instanceof Map) {
            chunk.append("Relationships:\n");
            for (Object rel : relationships) {
                if (rel instanceof Map) {
                    Map<?, ?> relMap = (Map<?, ?>) rel;
                    chunk.append("  - ").append(relMap.get("type"))
                         .append(" -> ").append(relMap.get("targetLabels"))
                         .append("\n");
                }
            }
        }
        
        return chunk.toString();
    }
    
    @Override
    public void close() {
        if (driver != null) {
            driver.close();
            logger.info("Neo4j driver closed");
        }
    }
    
    /**
     * Represents a chunk of graph data with its text representation and metadata
     */
    public static class GraphChunk {
        private final String text;
        private final Map<String, Object> metadata;
        
        public GraphChunk(String text, Map<String, Object> metadata) {
            this.text = text;
            this.metadata = metadata;
        }
        
        public String getText() {
            return text;
        }
        
        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}
