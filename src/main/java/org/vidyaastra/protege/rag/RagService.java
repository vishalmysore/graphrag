package org.vidyaastra.protege.rag;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Main RAG service that orchestrates Neo4j, Qdrant vector store, and AI models
 */
public class RagService {
    
    private static final Logger logger = LoggerFactory.getLogger(RagService.class);
    
    private final Neo4jService neo4jService;
    private final QdrantVectorStore vectorStore;
    private final EmbeddingService embeddingService;
    private final String aiModel;
    private final String aiApiKey;
    private final OkHttpClient httpClient;
    private final Gson gson;
    
    public RagService(Neo4jService neo4jService, QdrantVectorStore vectorStore,
                     EmbeddingService embeddingService, String aiModel, String aiApiKey) {
        this.neo4jService = neo4jService;
        this.vectorStore = vectorStore;
        this.embeddingService = embeddingService;
        this.aiModel = extractModelName(aiModel);
        this.aiApiKey = aiApiKey;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        this.gson = new Gson();
        
        logger.info("Initialized RagService with AI model: {} and Qdrant Cloud vector store", 
            this.aiModel);
    }
    
    private String extractModelName(String selection) {
        if (selection.contains("(")) {
            return selection.substring(0, selection.indexOf("(")).trim();
        }
        return selection;
    }
    
    /**
     * Execute a RAG-enhanced query
     */
    public String executeRagQuery(String userQuery, OWLOntology ontology) throws Exception {
        logger.info("Executing RAG query: {}", userQuery);
        
        // Step 1: Generate embedding for the user query
        List<Float> queryEmbedding = embeddingService.generateEmbedding(userQuery);
        
        // Convert to float[] for FileBasedVectorStore
        float[] queryVector = new float[queryEmbedding.size()];
        for (int i = 0; i < queryEmbedding.size(); i++) {
            queryVector[i] = queryEmbedding.get(i);
        }
        
        // Step 2: Search for relevant graph chunks in vector store
        List<QdrantVectorStore.SearchResult> searchResults = vectorStore.search(queryVector, 5);
        
        // Step 3: Retrieve graph schema from Neo4j
        String graphSchema = neo4jService.getGraphSchema();
        
        // Step 4: Build context from search results
        String retrievedContext = buildContext(searchResults);
        
        // Step 5: Get ontology summary
        String ontologySummary = buildOntologySummary(ontology);
        
        // Step 6: Generate response using AI model with RAG context
        String response = generateAIResponse(userQuery, retrievedContext, graphSchema, ontologySummary);
        
        return response;
    }
    
    /**
     * Index Neo4j graph data into vector store
     */
    public void indexGraphToVectorStore() throws IOException {
        logger.info("Starting graph indexing to vector store...");
        
        // Get graph chunks from Neo4j
        List<Neo4jService.GraphChunk> graphChunks = neo4jService.getGraphChunks();
        
        // Generate embeddings for each chunk
        List<String> texts = new ArrayList<>();
        for (Neo4jService.GraphChunk chunk : graphChunks) {
            texts.add(chunk.getText());
        }
        
        List<List<Float>> embeddings = embeddingService.generateEmbeddings(texts);
        
        // Prepare vector data for Qdrant
        List<QdrantVectorStore.VectorData> vectorDataList = new ArrayList<>();
        for (int i = 0; i < graphChunks.size(); i++) {
            Neo4jService.GraphChunk chunk = graphChunks.get(i);
            List<Float> embedding = embeddings.get(i);
            
            // Convert to float[]
            float[] vector = new float[embedding.size()];
            for (int j = 0; j < embedding.size(); j++) {
                vector[j] = embedding.get(j);
            }
            
            // Convert metadata to payload format
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", chunk.getText());
            payload.putAll(chunk.getMetadata());
            
            vectorDataList.add(new QdrantVectorStore.VectorData("graph_" + i, vector, payload));
        }
        
        // Upsert to vector store
        vectorStore.upsert(vectorDataList);
        
        logger.info("Successfully indexed {} graph chunks to vector store", graphChunks.size());
    }
    
    /**
     * Index current ontology into vector store
     */
    public void indexOntologyToVectorStore(OWLOntology ontology) throws IOException {
        logger.info("Starting ontology indexing to vector store...");
        
        List<String> chunks = new ArrayList<>();
        
        // Extract classes with their superclasses
        ontology.getClassesInSignature().forEach(cls -> {
            StringBuilder chunk = new StringBuilder();
            chunk.append(String.format("OWL Class: %s", cls.getIRI().getShortForm()));
            
            // Add superclass information
            ontology.getSubClassAxiomsForSubClass(cls).forEach(axiom -> {
                chunk.append(String.format(" subClassOf %s", 
                    axiom.getSuperClass().toString().replaceAll("<.*#(.+)>", "$1")));
            });
            
            chunks.add(chunk.toString());
        });
        
        // Extract individuals with their types and relationships
        ontology.getIndividualsInSignature().forEach(ind -> {
            StringBuilder chunk = new StringBuilder();
            chunk.append(String.format("Individual: %s", ind.getIRI().getShortForm()));
            
            // Add class types (rdf:type)
            ontology.getClassAssertionAxioms(ind).forEach(axiom -> {
                String className = axiom.getClassExpression().toString()
                    .replaceAll("<.*#(.+)>", "$1");
                chunk.append(String.format(" type:%s", className));
            });
            
            // Add object property assertions (relationships)
            ontology.getObjectPropertyAssertionAxioms(ind).forEach(axiom -> {
                String property = axiom.getProperty().toString()
                    .replaceAll("<.*#(.+)>", "$1");
                String target = axiom.getObject().toString()
                    .replaceAll("<.*#(.+)>", "$1");
                chunk.append(String.format(" %s:%s", property, target));
            });
            
            // Add data property assertions
            ontology.getDataPropertyAssertionAxioms(ind).forEach(axiom -> {
                String property = axiom.getProperty().toString()
                    .replaceAll("<.*#(.+)>", "$1");
                String value = axiom.getObject().getLiteral();
                chunk.append(String.format(" %s:'%s'", property, value));
            });
            
            chunks.add(chunk.toString());
        });
        
        // Extract object properties with domain/range
        ontology.getObjectPropertiesInSignature().forEach(prop -> {
            StringBuilder chunk = new StringBuilder();
            chunk.append(String.format("OWL Object Property: %s", prop.getIRI().getShortForm()));
            
            ontology.getObjectPropertyDomainAxioms(prop).forEach(axiom -> {
                chunk.append(String.format(" domain:%s", 
                    axiom.getDomain().toString().replaceAll("<.*#(.+)>", "$1")));
            });
            
            ontology.getObjectPropertyRangeAxioms(prop).forEach(axiom -> {
                chunk.append(String.format(" range:%s", 
                    axiom.getRange().toString().replaceAll("<.*#(.+)>", "$1")));
            });
            
            chunks.add(chunk.toString());
        });
        
        // Generate embeddings
        List<List<Float>> embeddings = embeddingService.generateEmbeddings(chunks);
        
        // Prepare vector data for Qdrant
        List<QdrantVectorStore.VectorData> vectorDataList = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            List<Float> embedding = embeddings.get(i);
            float[] vector = new float[embedding.size()];
            for (int j = 0; j < embedding.size(); j++) {
                vector[j] = embedding.get(j);
            }
            
            Map<String, Object> payload = new HashMap<>();
            payload.put("text", chunks.get(i));
            payload.put("source", "ontology");
            
            vectorDataList.add(new QdrantVectorStore.VectorData("ontology_" + i, vector, payload));
        }
        
        // Upsert to vector store
        vectorStore.upsert(vectorDataList);
        
        logger.info("Successfully indexed {} ontology elements to vector store", chunks.size());
    }
    
    private String buildContext(List<QdrantVectorStore.SearchResult> searchResults) {
        StringBuilder context = new StringBuilder();
        context.append("Relevant graph data retrieved from vector store:\n\n");
        
        for (int i = 0; i < searchResults.size(); i++) {
            QdrantVectorStore.SearchResult result = searchResults.get(i);
            context.append(String.format("Result %d (similarity: %.3f):\n", i + 1, result.getScore()));
            
            Object textValue = result.getMetadata().get("text");
            if (textValue != null) {
                context.append(textValue.toString()).append("\n\n");
            }
        }
        
        return context.toString();
    }
    
    private String buildOntologySummary(OWLOntology ontology) {
        StringBuilder summary = new StringBuilder();
        summary.append("Current Protégé Ontology Summary:\n");
        summary.append(String.format("- Classes: %d\n", ontology.getClassesInSignature().size()));
        summary.append(String.format("- Individuals: %d\n", ontology.getIndividualsInSignature().size()));
        summary.append(String.format("- Object Properties: %d\n", ontology.getObjectPropertiesInSignature().size()));
        summary.append(String.format("- Data Properties: %d\n", ontology.getDataPropertiesInSignature().size()));
        
        return summary.toString();
    }
    
    private String generateAIResponse(String userQuery, String context, 
                                     String graphSchema, String ontologySummary) throws IOException {
        
        // Build prompt with RAG context
        String systemPrompt = String.format(
            "You are an expert knowledge graph analyst specializing in ontology-based question answering. " +
            "Your task is to answer questions using ONLY the provided graph context.\n\n" +
            "CRITICAL INSTRUCTIONS:\n" +
            "1. Answer ONLY based on the RETRIEVED CONTEXT below\n" +
            "2. Map user question terms to actual ontology relationships:\n" +
            "   - 'created/made/generated' → check for 'detectedBy', 'discoveredBy', 'identifiedBy'\n" +
            "   - 'involved/participated' → check for 'involvedIn', 'partOf'\n" +
            "   - 'connected/linked' → check for relationship properties\n" +
            "3. When individuals have relationships, ALWAYS mention them\n" +
            "4. Use the actual property names from the context (e.g., 'detectedBy' not 'createdBy')\n" +
            "5. If context is insufficient, say so - DO NOT make up information\n\n" +
            "GRAPH SCHEMA:\n%s\n\n" +
            "ONTOLOGY SUMMARY:\n%s\n\n" +
            "RETRIEVED CONTEXT:\n%s\n\n" +
            "Answer the question using the relationships and properties shown in the context above.",
            graphSchema, ontologySummary, context
        );
        
        JsonObject requestBody = new JsonObject();
        JsonArray messages = new JsonArray();
        
        JsonObject systemMessage = new JsonObject();
        systemMessage.addProperty("role", "system");
        systemMessage.addProperty("content", systemPrompt);
        messages.add(systemMessage);
        
        JsonObject userMessage = new JsonObject();
        userMessage.addProperty("role", "user");
        userMessage.addProperty("content", userQuery);
        messages.add(userMessage);
        
        requestBody.add("messages", messages);
        requestBody.addProperty("model", aiModel);
        requestBody.addProperty("temperature", 0.7);
        requestBody.addProperty("max_tokens", 1000);
        
        String apiUrl = getApiUrl();
        
        Request request = new Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer " + aiApiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(
                gson.toJson(requestBody),
                MediaType.get("application/json")
            ))
            .build();
        
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("AI request failed: " + response.code() + " " + response.body().string());
            }
            
            JsonObject responseJson = gson.fromJson(response.body().string(), JsonObject.class);
            String content = responseJson
                .getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();
            
            logger.info("Generated AI response (length: {})", content.length());
            return content;
        }
    }
    
    private String getApiUrl() {
        if (aiModel.startsWith("gpt")) {
            return "https://api.openai.com/v1/chat/completions";
        } else if (aiModel.startsWith("claude")) {
            return "https://api.anthropic.com/v1/messages";
        } else {
            // Ollama local
            return "http://localhost:11434/v1/chat/completions";
        }
    }
}

