package org.vidyaastra.protege.rag;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for generating embeddings using various AI providers
 */
public class EmbeddingService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmbeddingService.class);
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String model;
    private final String apiKey;
    private final EmbeddingProvider provider;
    
    public EmbeddingService(String modelSelection, String apiKey) {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        this.model = extractModelName(modelSelection);
        this.apiKey = apiKey;
        this.provider = determineProvider(modelSelection);
        
        logger.info("Initialized EmbeddingService with model: {} (provider: {})", model, provider);
    }
    
    private String extractModelName(String selection) {
        // Extract model name from combo box selection like "text-embedding-3-small (OpenAI)"
        if (selection.contains("(")) {
            return selection.substring(0, selection.indexOf("(")).trim();
        }
        return selection;
    }
    
    private EmbeddingProvider determineProvider(String selection) {
        if (selection.contains("OpenAI")) {
            return EmbeddingProvider.OPENAI;
        } else if (selection.contains("Cohere")) {
            return EmbeddingProvider.COHERE;
        } else if (selection.contains("Local")) {
            return EmbeddingProvider.LOCAL;
        }
        return EmbeddingProvider.OPENAI; // default
    }
    
    /**
     * Generate embeddings for a text
     */
    public List<Float> generateEmbedding(String text) throws IOException {
        switch (provider) {
            case OPENAI:
                return generateOpenAIEmbedding(text);
            case COHERE:
                return generateCohereEmbedding(text);
            case LOCAL:
                return generateLocalEmbedding(text);
            default:
                throw new IllegalStateException("Unknown provider: " + provider);
        }
    }
    
    /**
     * Generate embeddings for multiple texts in batch
     */
    public List<List<Float>> generateEmbeddings(List<String> texts) throws IOException {
        List<List<Float>> embeddings = new ArrayList<>();
        
        // For now, process one by one
        // TODO: Implement batch processing for providers that support it
        for (String text : texts) {
            embeddings.add(generateEmbedding(text));
        }
        
        return embeddings;
    }
    
    private List<Float> generateOpenAIEmbedding(String text) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("input", text);
        requestBody.addProperty("model", model);
        
        Request request = new Request.Builder()
            .url("https://api.openai.com/v1/embeddings")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(
                gson.toJson(requestBody),
                MediaType.get("application/json")
            ))
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("OpenAI embedding request failed: " + response.code());
            }
            
            JsonObject responseJson = gson.fromJson(response.body().string(), JsonObject.class);
            var embeddingArray = responseJson
                .getAsJsonArray("data")
                .get(0).getAsJsonObject()
                .getAsJsonArray("embedding");
            
            List<Float> embedding = new ArrayList<>();
            for (var element : embeddingArray) {
                embedding.add(element.getAsFloat());
            }
            
            logger.debug("Generated OpenAI embedding with dimension: {}", embedding.size());
            return embedding;
        }
    }
    
    private List<Float> generateCohereEmbedding(String text) throws IOException {
        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("texts", text);
        requestBody.addProperty("model", model);
        requestBody.addProperty("input_type", "search_document");
        
        Request request = new Request.Builder()
            .url("https://api.cohere.ai/v1/embed")
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(
                gson.toJson(requestBody),
                MediaType.get("application/json")
            ))
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Cohere embedding request failed: " + response.code());
            }
            
            JsonObject responseJson = gson.fromJson(response.body().string(), JsonObject.class);
            var embeddingArray = responseJson
                .getAsJsonArray("embeddings")
                .get(0).getAsJsonArray();
            
            List<Float> embedding = new ArrayList<>();
            for (var element : embeddingArray) {
                embedding.add(element.getAsFloat());
            }
            
            logger.debug("Generated Cohere embedding with dimension: {}", embedding.size());
            return embedding;
        }
    }
    
    private List<Float> generateLocalEmbedding(String text) {
        // TODO: Implement local embedding using sentence-transformers or similar
        // For now, return a simple hash-based embedding as placeholder
        logger.warn("Local embedding not fully implemented, using placeholder");
        
        List<Float> embedding = new ArrayList<>();
        // Generate 1536-dimensional placeholder
        for (int i = 0; i < 1536; i++) {
            embedding.add((float) Math.random());
        }
        
        return embedding;
    }
    
    private enum EmbeddingProvider {
        OPENAI, COHERE, LOCAL
    }
}
