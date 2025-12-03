# Neo4j-Qdrant RAG Plugin for Protégé - Installation Guide

## Installation Instructions

1. **Build the plugin** (if not already built):
   ```bash
   mvn clean package
   ```

2. **Locate the plugin JAR**:
   - The plugin JAR is located at: `target/neo4j-qdrant-rag-plugin-1.0.0.jar`
   - File size: ~28MB

3. **Install in Protégé**:
   
   ### Option A: Using Protégé's Plugin Manager
   1. Open Protégé
   2. Go to `File` → `Preferences` → `Plugins`
   3. Click `Install Plugin from Local File`
   4. Select `neo4j-qdrant-rag-plugin-1.0.0.jar`
   5. Restart Protégé
   
   ### Option B: Manual Installation
   1. Close Protégé (if running)
   2. Copy `neo4j-qdrant-rag-plugin-1.0.0.jar` to your Protégé plugins directory:
      - **Windows**: `%APPDATA%\Protege\plugins\`
      - **macOS**: `~/Library/Application Support/Protege/plugins/`
      - **Linux**: `~/.Protege/plugins/`
   3. Start Protégé

4. **Verify Installation**:
   - Open an ontology in Protégé
   - Go to `Window` → `Tabs` → `RAG Query`
   - The RAG Query panel should appear

## Using the Plugin

### First Time Setup

1. **Configure Neo4j Connection**:
   - Neo4j URI: `bolt://localhost:7687` or your Neo4j server URI
   - Username: Your Neo4j username (default: `neo4j`)
   - Password: Your Neo4j password
   - Database: Database name (default: `neo4j`)

2. **Configure Qdrant Vector Database**:
   - Host: `localhost` or your Qdrant server
   - Port: `6334` (default Qdrant port)
   - Collection: Name for your vector collection (e.g., `ontology_vectors`)

3. **Configure AI Services**:
   - Embedding Model: Select from available models (e.g., `text-embedding-3-small`)
   - Embedding API Key: Your OpenAI or compatible API key
   - AI Model: Select LLM model (e.g., `gpt-4o-mini`, `gpt-4o`)
   - AI API Key: Your OpenAI or compatible API key

4. **Click "Save Settings"** to persist your configuration

### Features

- **Natural Language Queries**: Ask questions about your ontology in plain English
- **Graph Integration**: Export ontology to Neo4j and query with Cypher
- **Vector Search**: Semantic search using Qdrant vector database
- **RAG (Retrieval-Augmented Generation)**: Combine graph data with AI for intelligent responses

## Troubleshooting

### Plugin Not Loading

If the plugin doesn't appear in Protégé:

1. **Check Protégé Version**: This plugin requires Protégé 5.6.x
2. **Check Logs**: Look for errors in:
   - Windows: `%APPDATA%\Protege\logs\`
   - macOS/Linux: Check console output when starting Protégé

3. **Verify JAR Integrity**:
   - Check that the JAR file is not corrupted
   - Rebuild: `mvn clean package`

4. **OSGi Bundle Check**:
   - Open the JAR with an archive tool
   - Verify `META-INF/MANIFEST.MF` exists and contains proper OSGi headers
   - Verify `plugin.xml` exists in the root of the JAR

### Connection Issues

- **Neo4j**: Ensure Neo4j is running and accessible
- **Qdrant**: Ensure Qdrant vector database is running
- **API Keys**: Verify your AI service API keys are valid

## System Requirements

- **Java**: JDK 11 or higher
- **Protégé**: Version 5.6.4 (tested)
- **Neo4j**: Version 4.4+ (tested with 4.4.13)
- **Qdrant**: Latest version
- **Memory**: At least 2GB RAM recommended due to embedded dependencies

## Dependencies Included

The plugin includes all necessary dependencies:
- Neo4j Java Driver
- Qdrant Client
- Protocol Buffers
- Google Guava
- OkHttp
- Gson
- And all transitive dependencies

No additional installation required!
