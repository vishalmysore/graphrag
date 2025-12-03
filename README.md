# GraphRag : GraphRag For Java

A Protégé plugin that combines Neo4j graph databases, in-memory vector storage, and AI models to enable **Retrieval Augmented Generation (RAG)** for ontology exploration and knowledge graph querying.
![Vidya Astra Architecture](health2.jpg)
## 🎯 What Does This Plugin Do?

This plugin creates an intelligent bridge between:
- **Neo4j** - Your graph database with interconnected data
- **In-Memory Vector Store** - Fast semantic search without external dependencies
- **Embedding Models** - Convert graph data to semantic vectors
- **AI Models** - Generate intelligent responses using RAG

### Key Capabilities

1. **Store Graphs as RAG Context**
   - Automatically converts Neo4j graph data into vector embeddings
   - Stores embeddings in in-memory vector database
   - Enables semantic search over your graph structure

2. **RAG-Enhanced Queries**
   - Ask questions in natural language
   - Plugin retrieves relevant graph context using vector similarity
   - AI generates responses based on actual graph data + ontology structure
   - More accurate than pure LLM responses

3. **Unified Configuration**
   - Single interface to configure:
     - Neo4j connection (local or cloud)
     - Vector store collection name
     - Embedding model (OpenAI, Cohere, or local)
     - AI model (GPT-4, Claude, or Ollama)

4. **Bidirectional Data Flow**
   - Index Neo4j → Vector Store (store graph as vectors)
   - Index Protégé Ontology → Vector Store (store ontology as vectors)
   - Query with RAG → Get context-aware answers

## 🏗️ Architecture

```
┌─────────────────┐
│   Protégé UI    │
│  (OWL Ontology) │
└────────┬────────┘
         │
    ┌────▼──────────────────────────┐
    │  RAG Query Panel (Plugin)      │
    │  - Configuration Tab           │
    │  - RAG Query Tab               │
    │  - Vector Store Management Tab │
    └────┬───────────────────────────┘
         │
    ┌────▼────────┐
    │ RAG Service │
    └─┬─────┬─────┘
      │     │
      │     └──────────────┐
      │                    │
┌─────▼──────┐   ┌────────▼─────────┐
│  Neo4j     │   │  In-Memory Store │
│  (Graphs)  │   │  (Vectors)       │
└─────┬──────┘   └────────┬─────────┘
      │                   │
      └─────────┬─────────┘
                │
         ┌──────▼────────┐
         │ Embedding API │
         │ (OpenAI/etc)  │
         └───────────────┘
                │
         ┌──────▼────────┐
         │  AI Model API │
         │ (GPT/Claude)  │
         └───────────────┘
```

## 📦 Installation

### Prerequisites

1. **Protégé 5.6+**
2. **Java 11+**
3. **Neo4j Database** (Aura free tier or local Docker)
4. **API Keys** (optional):
   - OpenAI API key (for embeddings + AI)
   - Or Anthropic API key
   - Or use Ollama locally (no API key needed)

### Install Plugin

1. Download `neo4j-rag-plugin-1.0.0.jar` from releases
2. Copy to Protégé plugins folder:
   - Windows: `C:\Users\<you>\AppData\Roaming\Protege\plugins\`
   - macOS: `~/Library/Application Support/Protege/plugins/`
   - Linux: `~/.Protege/plugins/`
3. Restart Protégé
4. Open: `Window → Views → Ontology views → RAG Query`

## ⚙️ Configuration

### Tab 1: Configuration

**Neo4j Connection:**
```
URI: neo4j+s://xxxxx.databases.neo4j.io  (or bolt://localhost:7687)
Username: neo4j
Password: [your-password]
Database: neo4j
```

**Vector Store Configuration:**
```
Collection: ontology_graphs
```

**Embedding Model:**
```
Model: text-embedding-3-small (OpenAI)
API Key: sk-xxxxx  (leave empty for local models)
```

**AI Model:**
```
Model: gpt-4o-mini (OpenAI)
API Key: sk-xxxxx  (leave empty for Ollama)
```

Click **"Save Settings"** → **"Connect All"**

## 🚀 Usage

### Step 1: Index Your Graph Data

**Option A: Index Neo4j Graph**
1. Go to "Vector Store" tab
2. Click "Index Neo4j Graph to Vector Store"
3. Wait for indexing to complete
4. Graph nodes/relationships converted to embeddings and stored in memory

**Option B: Index Protégé Ontology**
1. Load your ontology in Protégé
2. Go to "Vector Store" tab
3. Click "Index Current Ontology to Vector Store"
4. OWL classes, individuals, properties → embeddings

### Step 2: Ask RAG-Enhanced Questions

1. Go to "RAG Query" tab
2. Type your question:
   ```
   "What are all the relationships between Patient and Disease nodes?"
   "Explain the class hierarchy in my ontology"
   "Find all symptoms related to diabetes in the graph"
   ```
3. Click "Execute RAG Query"

**What Happens:**
1. Your question is converted to an embedding
2. Vector store finds the 5 most similar graph/ontology chunks using cosine similarity
3. Retrieved context + graph schema + ontology summary sent to AI
4. AI generates answer based on YOUR actual data (not hallucinated!)

### Step 3: View Results

The response includes:
- **Direct answer** to your question
- **Relevant graph context** that was retrieved
- **Suggested Cypher queries** (if applicable)
- **Ontology insights** based on current state

## 🎓 Why RAG for Ontologies?

### The Problem with Pure LLMs

Traditional LLM queries about your graph/ontology:
- ❌ Hallucinate non-existent node labels
- ❌ Generate invalid Cypher queries
- ❌ Don't know your actual data structure
- ❌ Can't access current ontology state

### RAG Solution

With vector storage + retrieval:
- ✅ Retrieves actual graph data before answering
- ✅ Grounds responses in your real data
- ✅ Uses current ontology structure
- ✅ Generates valid queries based on schema
- ✅ Explains relationships that actually exist

## 📊 Use Cases

### For Students

**Learning Ontology Concepts:**
- "Explain the difference between classes and individuals in my ontology"
- Plugin retrieves your actual OWL entities as examples
- Learn by seeing real ontology data, not textbook definitions

**Understanding Graph Patterns:**
- "Show me examples of hierarchical relationships in the graph"
- Vector search finds actual subclass relationships
- See patterns in real data

### For Researchers

**Ontology Validation:**
```
Query: "Are there any inconsistencies in how Disease and Symptom are related?"

RAG retrieves:
- All Disease-Symptom relationships from Neo4j
- OWL axioms about Disease class
- Compares and highlights discrepancies
```

**Cross-Domain Analysis:**
```
Query: "How is 'causation' modeled in this medical ontology 
        compared to legal ontologies?"

RAG retrieves:
- Causal relationships from current graph
- Similar patterns from indexed ontologies
- Provides comparative analysis
```

**Hypothesis Generation:**
```
Query: "What disease pathways involve both genetic and environmental factors?"

RAG retrieves:
- Gene-Disease edges
- Environment-Disease edges  
- Finds intersection patterns
- Suggests new research directions
```

## 🔄 Workflow Examples

### Medical Research Workflow

```
1. Load medical ontology in Protégé
2. Index Neo4j clinical trial graph to vector store
3. Index ontology to vector store
4. Query: "Find all FDA-approved drugs for autoimmune diseases"
   → RAG retrieves relevant drug-disease relationships
   → AI generates list with explanations
5. Query: "What are the common side effects?"
   → RAG finds drug-side-effect patterns
   → Returns evidence-based answer
6. Export findings back to Neo4j as new relationships
```

### Ontology Development Workflow

```
1. Design initial ontology structure in Protégé
2. Connect to domain knowledge graph (Neo4j)
3. Index both to vector store
4. Query: "What classes are missing from my ontology?"
   → RAG compares graph labels vs. OWL classes
   → Identifies gaps
5. Add missing classes
6. Query: "Suggest axioms for the new classes"
   → RAG retrieves similar patterns
   → Recommends axioms based on graph data
```

## 🔧 Advanced Features

### Custom Embedding Models

Supports multiple embedding providers:
- **OpenAI**: `text-embedding-3-small`, `text-embedding-3-large`
- **Cohere**: `embed-english-v3.0`
- **Local**: `all-MiniLM-L6-v2`, `all-mpnet-base-v2` (coming soon)

### Multiple AI Models

Choose based on your needs:
- **GPT-4o**: Best accuracy, higher cost
- **GPT-4o-mini**: Balanced performance
- **Claude-3-Opus**: Strong reasoning
- **Ollama (llama3)**: Free, runs locally, private

### Vector Store Statistics

View current state:
```
Collection: ontology_graphs
Vectors: 1,247 (graph chunks + ontology elements)
Dimensions: 1536 (embedding dimensions)
Last indexed: Session-based (in-memory)
```

## 🔒 Security & Privacy

- **In-memory vector storage**: No external dependencies, data stored during session only
- **Encrypted credentials**: Settings stored in Protégé preferences
- **No data persistence**: Vector data cleared when Protégé closes
- **API keys masked**: Password fields in UI
- **Self-hosted option**: Use Ollama for complete local setup

## 🐛 Troubleshooting

### "Neo4j Connection Failed"
```
# Check Neo4j service is running
# For local: http://localhost:7474
# For Aura: verify URI and credentials
```

### "Embedding Generation Failed"
- Verify API key is correct
- Check API rate limits
- Try switching to different model

### "No Results from Vector Search"
- Ensure you've indexed data first
- Check collection name matches
- View stats to confirm vectors exist

### "Embedding Generation Failed"
- Verify API key is correct
- Check API rate limits
- Try switching to different model

### "No Results Found"
- Ensure you've indexed data first
- Check collection name matches
- Try re-indexing your data

### "AI Response Empty"
- Check AI model API key
- Verify API quota not exceeded
- Try simpler query first

## 📚 Technical Details

### Embedding Dimensions
- OpenAI text-embedding-3-small: **1536 dimensions**
- In-memory storage with cosine similarity

### RAG Pipeline
```
User Query 
  → Embedding (1536-dim vector)
  → In-memory similarity search (top-5)
  → Context building (retrieved chunks)
  → Schema injection (Neo4j schema)
  → Ontology summary (OWL stats)
  → AI prompt construction
  → Response generation
```

### Performance
- **Indexing**: ~100 chunks/second
- **Query**: <2 seconds end-to-end
- **Vector search**: <50ms for 10k vectors (in-memory)

## 🛣️ Roadmap

- [ ] Batch embedding optimization
- [ ] Local embedding models (sentence-transformers)
- [ ] Multi-modal embeddings (images, diagrams)
- [ ] Persistent vector storage option
- [ ] Query history and caching
- [ ] Graph visualization of retrieved context
- [ ] SPARQL integration for ontology queries
- [ ] Export RAG responses to ontology annotations

## 📖 Resources

- [Neo4j Graph Database](https://neo4j.com/docs/)
- [RAG Tutorial](https://www.anthropic.com/index/contextual-retrieval)
- [Embeddings Guide](https://platform.openai.com/docs/guides/embeddings)
- [Protégé Platform](https://protege.stanford.edu/)

## 🤝 Contributing

Contributions welcome! This plugin is open source.

## 📄 License

Apache 2.0 License

---

**Built with ❤️ for students and researchers exploring ontologies and knowledge graphs**
