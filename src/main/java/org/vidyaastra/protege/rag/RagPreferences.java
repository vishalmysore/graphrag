package org.vidyaastra.protege.rag;

import org.protege.editor.core.prefs.Preferences;
import org.protege.editor.core.prefs.PreferencesManager;

/**
 * Manages plugin preferences for Neo4j, vector store, and AI model configurations
 */
public class RagPreferences {
    
    private static final String PREFERENCES_ID = "com.protege.rag";
    
    // Neo4j preference keys
    private static final String NEO4J_URI_KEY = "neo4j.uri";
    private static final String NEO4J_USERNAME_KEY = "neo4j.username";
    private static final String NEO4J_PASSWORD_KEY = "neo4j.password";
    private static final String NEO4J_DATABASE_KEY = "neo4j.database";
    
    // Vector Store preference keys
    private static final String VECTOR_STORE_COLLECTION_KEY = "vectorstore.collection";
    private static final String QDRANT_URL_KEY = "qdrant.url";
    private static final String QDRANT_API_KEY_KEY = "qdrant.apikey";
    
    // Embedding model preference keys
    private static final String EMBEDDING_MODEL_KEY = "embedding.model";
    private static final String EMBEDDING_API_KEY_KEY = "embedding.apikey";
    
    // AI model preference keys
    private static final String AI_MODEL_KEY = "ai.model";
    private static final String AI_API_KEY_KEY = "ai.apikey";
    
    // Default values
    private static final String DEFAULT_NEO4J_URI = "bolt://localhost:7687";
    private static final String DEFAULT_NEO4J_USERNAME = "neo4j";
    private static final String DEFAULT_NEO4J_DATABASE = "neo4j";
    
    private static final String DEFAULT_VECTOR_STORE_COLLECTION = "ontology_graphs";
    private static final String DEFAULT_QDRANT_URL = "./qdrant_local";
    
    private static final String DEFAULT_EMBEDDING_MODEL = "text-embedding-3-small (OpenAI)";
    private static final String DEFAULT_AI_MODEL = "gpt-4o-mini (OpenAI)";
    
    private Preferences getPreferences() {
        return PreferencesManager.getInstance()
            .getPreferencesForSet(PREFERENCES_ID, PREFERENCES_ID);
    }
    
    // Neo4j getters and setters
    public String getNeo4jUri() {
        return getPreferences().getString(NEO4J_URI_KEY, DEFAULT_NEO4J_URI);
    }
    
    public void setNeo4jUri(String uri) {
        getPreferences().putString(NEO4J_URI_KEY, uri);
    }
    
    public String getNeo4jUsername() {
        return getPreferences().getString(NEO4J_USERNAME_KEY, DEFAULT_NEO4J_USERNAME);
    }
    
    public void setNeo4jUsername(String username) {
        getPreferences().putString(NEO4J_USERNAME_KEY, username);
    }
    
    public String getNeo4jPassword() {
        return getPreferences().getString(NEO4J_PASSWORD_KEY, "");
    }
    
    public void setNeo4jPassword(String password) {
        getPreferences().putString(NEO4J_PASSWORD_KEY, password);
    }
    
    public String getNeo4jDatabase() {
        return getPreferences().getString(NEO4J_DATABASE_KEY, DEFAULT_NEO4J_DATABASE);
    }
    
    public void setNeo4jDatabase(String database) {
        getPreferences().putString(NEO4J_DATABASE_KEY, database);
    }
    
    // Vector Store getters and setters
    public String getVectorStoreCollection() {
        return getPreferences().getString(VECTOR_STORE_COLLECTION_KEY, DEFAULT_VECTOR_STORE_COLLECTION);
    }
    
    public void setVectorStoreCollection(String collection) {
        getPreferences().putString(VECTOR_STORE_COLLECTION_KEY, collection);
    }
    
    public String getQdrantUrl() {
        return getPreferences().getString(QDRANT_URL_KEY, DEFAULT_QDRANT_URL);
    }
    
    public void setQdrantUrl(String url) {
        getPreferences().putString(QDRANT_URL_KEY, url);
    }
    
    public String getQdrantApiKey() {
        return getPreferences().getString(QDRANT_API_KEY_KEY, "");
    }
    
    public void setQdrantApiKey(String apiKey) {
        getPreferences().putString(QDRANT_API_KEY_KEY, apiKey);
    }
    
    // Embedding model getters and setters
    public String getEmbeddingModel() {
        return getPreferences().getString(EMBEDDING_MODEL_KEY, DEFAULT_EMBEDDING_MODEL);
    }
    
    public void setEmbeddingModel(String model) {
        getPreferences().putString(EMBEDDING_MODEL_KEY, model);
    }
    
    public String getEmbeddingApiKey() {
        return getPreferences().getString(EMBEDDING_API_KEY_KEY, "");
    }
    
    public void setEmbeddingApiKey(String apiKey) {
        getPreferences().putString(EMBEDDING_API_KEY_KEY, apiKey);
    }
    
    // AI model getters and setters
    public String getAiModel() {
        return getPreferences().getString(AI_MODEL_KEY, DEFAULT_AI_MODEL);
    }
    
    public void setAiModel(String model) {
        getPreferences().putString(AI_MODEL_KEY, model);
    }
    
    public String getAiApiKey() {
        return getPreferences().getString(AI_API_KEY_KEY, "");
    }
    
    public void setAiApiKey(String apiKey) {
        getPreferences().putString(AI_API_KEY_KEY, apiKey);
    }
}
