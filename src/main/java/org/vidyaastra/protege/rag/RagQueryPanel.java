package org.vidyaastra.protege.rag;

import org.protege.editor.owl.ui.view.AbstractOWLViewComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Main UI panel for Neo4j-Qdrant RAG plugin
 * Allows users to configure connections and perform RAG-based queries
 */
public class RagQueryPanel extends AbstractOWLViewComponent {
    
    private static final Logger logger = LoggerFactory.getLogger(RagQueryPanel.class);
    
    // Connection Configuration Fields
    private JTextField neo4jUriField;
    private JTextField neo4jUsernameField;
    private JPasswordField neo4jPasswordField;
    private JTextField neo4jDatabaseField;
    
    private JTextField vectorStoreCollectionField;
    
    private JComboBox<String> embeddingModelCombo;
    private JPasswordField embeddingApiKeyField;
    
    private JComboBox<String> aiModelCombo;
    private JPasswordField aiApiKeyField;
    
    // Status indicators
    private JLabel neo4jStatusLabel;
    private JLabel vectorStoreStatusLabel;
    
    // Query Components
    private JTextArea queryTextArea;
    private JTextArea resultsTextArea;
    private JTextArea statsArea;
    private JButton executeButton;
    private JButton saveSettingsButton;
    private JButton connectButton;
    
    // Service instances
    private RagPreferences preferences;
    private Neo4jService neo4jService;
    private InMemoryVectorStore vectorStore;
    private EmbeddingService embeddingService;
    private RagService ragService;
    
    @Override
    protected void initialiseOWLView() {
        setLayout(new BorderLayout());
        preferences = new RagPreferences();
        
        // Create main panel with tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Configuration", createConfigurationPanel());
        tabbedPane.addTab("RAG Query", createQueryPanel());
        tabbedPane.addTab("Vector Store", createVectorStorePanel());
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Load saved preferences
        loadPreferences();
    }
    
    private JPanel createConfigurationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel configGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Neo4j Configuration Section
        addSectionHeader(configGrid, gbc, 0, "Neo4j Configuration");
        
        gbc.gridy = 1;
        addLabeledField(configGrid, gbc, "URI:", neo4jUriField = new JTextField(30));
        neo4jUriField.setToolTipText("e.g., neo4j+s://xxxxx.databases.neo4j.io or bolt://localhost:7687");
        
        gbc.gridy = 2;
        addLabeledField(configGrid, gbc, "Username:", neo4jUsernameField = new JTextField(30));
        
        gbc.gridy = 3;
        addLabeledField(configGrid, gbc, "Password:", neo4jPasswordField = new JPasswordField(30));
        
        gbc.gridy = 4;
        addLabeledField(configGrid, gbc, "Database:", neo4jDatabaseField = new JTextField(30));
        neo4jDatabaseField.setText("neo4j");
        
        gbc.gridy = 5;
        gbc.gridx = 1;
        neo4jStatusLabel = new JLabel("⚪ Not Connected");
        configGrid.add(neo4jStatusLabel, gbc);
        
        // Vector Store Configuration Section
        gbc.gridy = 6;
        addSectionHeader(configGrid, gbc, 6, "Vector Store Configuration (In-Memory)");
        
        gbc.gridy = 7;
        addLabeledField(configGrid, gbc, "Collection:", vectorStoreCollectionField = new JTextField(30));
        vectorStoreCollectionField.setText("ontology_graphs");
        
        gbc.gridy = 8;
        gbc.gridx = 1;
        vectorStoreStatusLabel = new JLabel("⚪ Not Initialized");
        configGrid.add(vectorStoreStatusLabel, gbc);
        
        // Embedding Model Configuration
        gbc.gridy = 9;
        addSectionHeader(configGrid, gbc, 9, "Embedding Model");
        
        gbc.gridy = 12;
        String[] embeddingModels = {
            "text-embedding-3-small (OpenAI)",
            "text-embedding-3-large (OpenAI)",
            "text-embedding-ada-002 (OpenAI)",
            "embed-english-v3.0 (Cohere)",
            "all-MiniLM-L6-v2 (Local)",
            "all-mpnet-base-v2 (Local)"
        };
        addLabeledField(configGrid, gbc, "Model:", embeddingModelCombo = new JComboBox<>(embeddingModels));
        
        gbc.gridy = 13;
        addLabeledField(configGrid, gbc, "API Key:", embeddingApiKeyField = new JPasswordField(30));
        embeddingApiKeyField.setToolTipText("Leave empty for local models");
        
        // AI Model Configuration
        gbc.gridy = 14;
        addSectionHeader(configGrid, gbc, 14, "AI Model (for RAG)");
        
        gbc.gridy = 15;
        String[] aiModels = {
            "gpt-4o (OpenAI)",
            "gpt-4o-mini (OpenAI)",
            "claude-3-5-sonnet (Anthropic)",
            "claude-3-opus (Anthropic)",
            "llama3:8b (Ollama Local)",
            "mistral (Ollama Local)"
        };
        addLabeledField(configGrid, gbc, "Model:", aiModelCombo = new JComboBox<>(aiModels));
        
        gbc.gridy = 16;
        addLabeledField(configGrid, gbc, "API Key:", aiApiKeyField = new JPasswordField(30));
        aiApiKeyField.setToolTipText("Leave empty for Ollama local models");
        
        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveSettingsButton = new JButton("Save Settings");
        saveSettingsButton.addActionListener(this::handleSaveSettings);
        
        connectButton = new JButton("Connect All");
        connectButton.addActionListener(this::handleConnect);
        
        buttonPanel.add(saveSettingsButton);
        buttonPanel.add(connectButton);
        
        panel.add(new JScrollPane(configGrid), BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Query input area
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.setBorder(BorderFactory.createTitledBorder("Ask a Question (RAG-Enhanced)"));
        
        queryTextArea = new JTextArea(4, 40);
        queryTextArea.setLineWrap(true);
        queryTextArea.setWrapStyleWord(true);
        queryTextArea.setText("What are all the classes in my ontology and how are they related?");
        queryPanel.add(new JScrollPane(queryTextArea), BorderLayout.CENTER);
        
        executeButton = new JButton("Execute RAG Query");
        executeButton.addActionListener(this::handleExecuteQuery);
        queryPanel.add(executeButton, BorderLayout.SOUTH);
        
        // Results area
        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Results & Explanation"));
        
        resultsTextArea = new JTextArea(20, 40);
        resultsTextArea.setEditable(false);
        resultsTextArea.setLineWrap(true);
        resultsTextArea.setWrapStyleWord(true);
        resultsPanel.add(new JScrollPane(resultsTextArea), BorderLayout.CENTER);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, queryPanel, resultsPanel);
        splitPane.setDividerLocation(150);
        
        panel.add(splitPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createVectorStorePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton indexGraphButton = new JButton("Index Neo4j Graph to Vector Store");
        indexGraphButton.addActionListener(this::handleIndexGraph);
        indexGraphButton.setToolTipText("Convert Neo4j graph to embeddings and store in memory");
        
        JButton indexOntologyButton = new JButton("Index Current Ontology to Vector Store");
        indexOntologyButton.addActionListener(this::handleIndexOntology);
        indexOntologyButton.setToolTipText("Convert current Protégé ontology to embeddings");
        
        JButton viewStatsButton = new JButton("View Vector Store Stats");
        viewStatsButton.addActionListener(this::handleViewStats);
        
        controlPanel.add(indexGraphButton);
        controlPanel.add(indexOntologyButton);
        controlPanel.add(viewStatsButton);
        
        statsArea = new JTextArea(20, 40);
        statsArea.setEditable(false);
        statsArea.setText("Vector Store Statistics:\n\nClick 'View Vector Store Stats' to see current status.");
        
        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(statsArea), BorderLayout.CENTER);
        
        return panel;
    }
    
    private void addSectionHeader(JPanel panel, GridBagConstraints gbc, int row, String title) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JLabel header = new JLabel(title);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        panel.add(header, gbc);
        gbc.gridwidth = 1;
    }
    
    private void addLabeledField(JPanel panel, GridBagConstraints gbc, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel(label), gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }
    
    private void handleSaveSettings(ActionEvent e) {
        // Save Neo4j settings
        preferences.setNeo4jUri(neo4jUriField.getText());
        preferences.setNeo4jUsername(neo4jUsernameField.getText());
        preferences.setNeo4jPassword(new String(neo4jPasswordField.getPassword()));
        preferences.setNeo4jDatabase(neo4jDatabaseField.getText());
        
        // Save vector store settings
        preferences.setVectorStoreCollection(vectorStoreCollectionField.getText());
        
        // Save model settings
        preferences.setEmbeddingModel((String) embeddingModelCombo.getSelectedItem());
        preferences.setEmbeddingApiKey(new String(embeddingApiKeyField.getPassword()));
        preferences.setAiModel((String) aiModelCombo.getSelectedItem());
        preferences.setAiApiKey(new String(aiApiKeyField.getPassword()));
        
        JOptionPane.showMessageDialog(this, "Settings saved successfully!", 
            "Success", JOptionPane.INFORMATION_MESSAGE);
        
        logger.info("Settings saved to preferences");
    }
    
    private void handleConnect(ActionEvent e) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                // Connect to Neo4j
                neo4jService = new Neo4jService(
                    neo4jUriField.getText(),
                    neo4jUsernameField.getText(),
                    new String(neo4jPasswordField.getPassword()),
                    neo4jDatabaseField.getText()
                );
                
                // Create in-memory vector store
                vectorStore = new InMemoryVectorStore(
                    vectorStoreCollectionField.getText()
                );
                
                // Initialize embedding service
                embeddingService = new EmbeddingService(
                    (String) embeddingModelCombo.getSelectedItem(),
                    new String(embeddingApiKeyField.getPassword())
                );
                
                // Initialize RAG service
                ragService = new RagService(
                    neo4jService,
                    vectorStore,
                    embeddingService,
                    (String) aiModelCombo.getSelectedItem(),
                    new String(aiApiKeyField.getPassword())
                );
                
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    neo4jStatusLabel.setText("🟢 Connected");
                    vectorStoreStatusLabel.setText("🟢 Ready");
                    JOptionPane.showMessageDialog(RagQueryPanel.this,
                        "Successfully connected to all services!",
                        "Connected", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    neo4jStatusLabel.setText("🔴 Connection Failed");
                    vectorStoreStatusLabel.setText("🔴 Failed");
                    JOptionPane.showMessageDialog(RagQueryPanel.this,
                        "Connection failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    logger.error("Connection failed", ex);
                }
            }
        }.execute();
    }
    
    private void handleExecuteQuery(ActionEvent e) {
        if (ragService == null) {
            JOptionPane.showMessageDialog(this,
                "Please connect to services first!",
                "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String query = queryTextArea.getText().trim();
        if (query.isEmpty()) {
            return;
        }
        
        resultsTextArea.setText("Processing RAG query...\n\n");
        executeButton.setEnabled(false);
        
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return ragService.executeRagQuery(query, getOWLModelManager().getActiveOntology());
            }
            
            @Override
            protected void done() {
                try {
                    String result = get();
                    resultsTextArea.setText(result);
                } catch (Exception ex) {
                    resultsTextArea.setText("Error: " + ex.getMessage());
                    logger.error("RAG query failed", ex);
                } finally {
                    executeButton.setEnabled(true);
                }
            }
        }.execute();
    }
    
    private void handleIndexGraph(ActionEvent e) {
        if (ragService == null) {
            JOptionPane.showMessageDialog(this,
                "Please connect to services first!",
                "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                logger.info("Starting Neo4j graph indexing...");
                ragService.indexGraphToVectorStore();
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    InMemoryVectorStore.CollectionStats stats = vectorStore.getStats();
                    JOptionPane.showMessageDialog(RagQueryPanel.this,
                        "Successfully indexed Neo4j graph!\n\nTotal vectors: " + stats.getVectorsCount(),
                        "Indexing Complete", JOptionPane.INFORMATION_MESSAGE);
                    logger.info("Graph indexing completed successfully");
                    
                    // Auto-refresh stats
                    handleViewStats(null);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RagQueryPanel.this,
                        "Graph indexing failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    logger.error("Graph indexing failed", ex);
                }
            }
        }.execute();
    }
    
    private void handleIndexOntology(ActionEvent e) {
        if (ragService == null) {
            JOptionPane.showMessageDialog(this,
                "Please connect to services first!",
                "Not Connected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (getOWLModelManager().getActiveOntology() == null) {
            JOptionPane.showMessageDialog(this,
                "No ontology is currently loaded!",
                "No Ontology", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                logger.info("Starting ontology indexing...");
                ragService.indexOntologyToVectorStore(getOWLModelManager().getActiveOntology());
                return null;
            }
            
            @Override
            protected void done() {
                try {
                    get();
                    InMemoryVectorStore.CollectionStats stats = vectorStore.getStats();
                    JOptionPane.showMessageDialog(RagQueryPanel.this,
                        "Successfully indexed ontology!\n\nTotal vectors: " + stats.getVectorsCount(),
                        "Indexing Complete", JOptionPane.INFORMATION_MESSAGE);
                    logger.info("Ontology indexing completed successfully");
                    
                    // Auto-refresh stats
                    handleViewStats(null);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(RagQueryPanel.this,
                        "Ontology indexing failed: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                    logger.error("Ontology indexing failed", ex);
                }
            }
        }.execute();
    }
    
    private void handleViewStats(ActionEvent e) {
        if (vectorStore == null) {
            statsArea.setText("Vector Store Statistics:\n\nStatus: Not initialized\n\nPlease connect to services first.");
            return;
        }
        
        try {
            InMemoryVectorStore.CollectionStats stats = vectorStore.getStats();
            StringBuilder sb = new StringBuilder();
            sb.append("Vector Store Statistics:\n\n");
            sb.append("Collection: ").append(vectorStoreCollectionField.getText()).append("\n");
            sb.append("Total Vectors: ").append(stats.getVectorsCount()).append("\n");
            sb.append("Total Points: ").append(stats.getPointsCount()).append("\n");
            sb.append("Storage Type: In-Memory\n");
            sb.append("Status: ").append(stats.getVectorsCount() > 0 ? "Ready" : "Empty - Please index data").append("\n\n");
            
            if (stats.getVectorsCount() == 0) {
                sb.append("No vectors indexed yet.\n");
                sb.append("Use 'Index Neo4j Graph' or 'Index Current Ontology' buttons above.");
            } else {
                sb.append("Vector store is ready for queries.");
            }
            
            statsArea.setText(sb.toString());
            logger.info("Displayed vector store stats: {} vectors", stats.getVectorsCount());
        } catch (Exception ex) {
            statsArea.setText("Error retrieving stats: " + ex.getMessage());
            logger.error("Failed to get vector store stats", ex);
        }
    }
    
    private void loadPreferences() {
        neo4jUriField.setText(preferences.getNeo4jUri());
        neo4jUsernameField.setText(preferences.getNeo4jUsername());
        neo4jPasswordField.setText(preferences.getNeo4jPassword());
        neo4jDatabaseField.setText(preferences.getNeo4jDatabase());
        
        vectorStoreCollectionField.setText(preferences.getVectorStoreCollection());
        
        String embModel = preferences.getEmbeddingModel();
        if (embModel != null) {
            embeddingModelCombo.setSelectedItem(embModel);
        }
        embeddingApiKeyField.setText(preferences.getEmbeddingApiKey());
        
        String aiModel = preferences.getAiModel();
        if (aiModel != null) {
            aiModelCombo.setSelectedItem(aiModel);
        }
        aiApiKeyField.setText(preferences.getAiApiKey());
    }
    
    @Override
    protected void disposeOWLView() {
        if (neo4jService != null) {
            neo4jService.close();
        }
        if (vectorStore != null) {
            try {
                vectorStore.close();
            } catch (Exception e) {
                logger.error("Error closing vector store", e);
            }
        }
    }
}
