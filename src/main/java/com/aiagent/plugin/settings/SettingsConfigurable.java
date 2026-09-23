package com.aiagent.plugin.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Settings page: Settings → Tools → AI Agent
 */
public class SettingsConfigurable implements Configurable {

    private JPanel rootPanel;

    // Backend selection
    private JRadioButton ollamaRadio;
    private JRadioButton llamaRadio;

    // Ollama Fields
    private JSpinner ollamaPortSpinner;
    private JComboBox<String> ollamaModelCombo;

    // llama-server Fields
    private JTextField llamaServerField;
    private JTextField modelPathField;
    private JSpinner llamaPortSpinner;
    private JSpinner threadsSpinner;
    private JSpinner gpuLayersSpinner;
    private JCheckBox keepAliveCheck;
    private JCheckBox autoStartCheck;

    // Common Model Parameters
    private JSpinner contextSizeSpinner;
    private JSpinner temperatureSpinner;
    private JSpinner topPSpinner;
    private JSpinner maxTokensSpinner;

    @Nls
    @Override
    public String getDisplayName() {
        return "lojou";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        PluginSettings.State s = PluginSettings.getInstance().getState();
        assert s != null;

        rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        // ── Backend Selector ─────────────────────────────────────────────────
        JPanel backendSection = createSection("LLM Backend Provider");
        ollamaRadio = new JRadioButton("Ollama (Local service - Recommended)", s.useOllama);
        llamaRadio = new JRadioButton("llama-server (llama.cpp standalone binary + GGUF)", !s.useOllama);
        ButtonGroup group = new ButtonGroup();
        group.add(ollamaRadio);
        group.add(llamaRadio);

        backendSection.add(ollamaRadio);
        backendSection.add(Box.createVerticalStrut(4));
        backendSection.add(llamaRadio);
        content.add(backendSection);
        content.add(Box.createVerticalStrut(10));

        // ── Ollama Section ───────────────────────────────────────────────────
        JPanel ollamaSection = createSection("Ollama Settings");
        ollamaPortSpinner = new JSpinner(new SpinnerNumberModel(s.ollamaPort, 1024, 65535, 1));
        ollamaSection.add(labeledRow("Ollama Port:", ollamaPortSpinner));

        ollamaModelCombo = new JComboBox<>();
        ollamaModelCombo.setEditable(true);
        populateOllamaModels(s.ollamaPort, s.ollamaModel);

        JButton refreshModelsBtn = new JButton("Refresh Models");
        refreshModelsBtn.addActionListener(e -> refreshOllamaModels());
        ollamaSection.add(labeledRow("Model Name:", ollamaModelCombo, refreshModelsBtn));

        JButton testOllamaBtn = new JButton("Test Ollama Connection");
        testOllamaBtn.addActionListener(e -> testOllamaConnection());
        ollamaSection.add(labeledRow("", testOllamaBtn));

        content.add(ollamaSection);
        content.add(Box.createVerticalStrut(10));

        // ── llama-server Section ─────────────────────────────────────────────
        JPanel serverSection = createSection("llama-server Configuration");

        llamaServerField = new JTextField(s.llamaServerPath, 34);
        serverSection.add(labeledRow("llama-server Path:", llamaServerField,
                browseFileButton("Select llama-server executable", () -> llamaServerField)));

        modelPathField = new JTextField(s.modelPath, 34);
        serverSection.add(labeledRow("Model (.gguf) Path:", modelPathField,
                browseFileButton("Select GGUF model", () -> modelPathField)));

        llamaPortSpinner = new JSpinner(new SpinnerNumberModel(s.serverPort, 1024, 65535, 1));
        serverSection.add(labeledRow("Server Port:", llamaPortSpinner));

        threadsSpinner = new JSpinner(new SpinnerNumberModel(s.threads, 1, 64, 1));
        serverSection.add(labeledRow("CPU Threads:", threadsSpinner));

        gpuLayersSpinner = new JSpinner(new SpinnerNumberModel(s.gpuLayers, 0, 200, 1));
        serverSection.add(labeledRow("GPU Layers (0 = CPU only):", gpuLayersSpinner));

        keepAliveCheck = new JCheckBox("Keep llama-server running between chats", s.keepServerAlive);
        autoStartCheck = new JCheckBox("Auto-start server when plugin loads", s.autoStartServer);
        serverSection.add(keepAliveCheck);
        serverSection.add(Box.createVerticalStrut(4));
        serverSection.add(autoStartCheck);
        serverSection.add(Box.createVerticalStrut(6));

        JButton testLlamaBtn = new JButton("Test llama-server Connection");
        testLlamaBtn.addActionListener(e -> testLlamaConnection());
        serverSection.add(labeledRow("", testLlamaBtn));

        content.add(serverSection);
        content.add(Box.createVerticalStrut(10));

        // Toggle UI based on backend
        Runnable updateUIState = () -> {
            boolean isOllama = ollamaRadio.isSelected();
            setContainerEnabled(ollamaSection, isOllama);
            setContainerEnabled(serverSection, !isOllama);
        };
        ollamaRadio.addActionListener(e -> updateUIState.run());
        llamaRadio.addActionListener(e -> updateUIState.run());
        updateUIState.run();

        // ── Common Model Parameters Section ──────────────────────────────────
        JPanel modelSection = createSection("Model Parameters");

        contextSizeSpinner = new JSpinner(new SpinnerNumberModel(s.contextSize, 512, 131072, 512));
        modelSection.add(labeledRow("Context Size (tokens):", contextSizeSpinner));

        temperatureSpinner = new JSpinner(new SpinnerNumberModel((double) s.temperature, 0.0, 2.0, 0.05));
        ((JSpinner.DefaultEditor) temperatureSpinner.getEditor()).getTextField().setColumns(5);
        modelSection.add(labeledRow("Temperature:", temperatureSpinner));

        topPSpinner = new JSpinner(new SpinnerNumberModel((double) s.topP, 0.0, 1.0, 0.05));
        ((JSpinner.DefaultEditor) topPSpinner.getEditor()).getTextField().setColumns(5);
        modelSection.add(labeledRow("Top-P:", topPSpinner));

        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(s.maxTokens, 64, 16384, 128));
        modelSection.add(labeledRow("Max Output Tokens:", maxTokensSpinner));
        content.add(modelSection);
        content.add(Box.createVerticalStrut(10));

        // ── Help ──────────────────────────────────────────────────────────────
        JLabel helpLabel = new JLabel(
                "<html><small><b>Ollama:</b> Download from ollama.ai. Run <code>ollama run qwen3.5:2b</code>.<br/>" +
                "<b>llama.cpp:</b> Download llama-server from github.com/ggerganov/llama.cpp/releases and a GGUF model.</small></html>");
        helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(helpLabel);

        rootPanel.add(new JScrollPane(content), BorderLayout.CENTER);
        return rootPanel;
    }

    // ─────────────────────────────────── Helpers ────────────────────────────

    private void populateOllamaModels(int port, String currentSelected) {
        ollamaModelCombo.removeAllItems();
        List<String> models = PluginSettings.getOllamaModels(port);
        if (models.isEmpty()) {
            ollamaModelCombo.addItem(currentSelected != null && !currentSelected.isEmpty() ? currentSelected : "qwen3.5:2b");
        } else {
            for (String m : models) {
                ollamaModelCombo.addItem(m);
            }
            if (currentSelected != null && models.contains(currentSelected)) {
                ollamaModelCombo.setSelectedItem(currentSelected);
            } else {
                ollamaModelCombo.setSelectedIndex(0);
            }
        }
    }

    private void refreshOllamaModels() {
        int port = (int) ollamaPortSpinner.getValue();
        List<String> models = PluginSettings.getOllamaModels(port);
        if (models.isEmpty()) {
            Messages.showWarningDialog(
                    "Could not retrieve models from Ollama on port " + port + ".\n" +
                    "Make sure Ollama is running ('ollama serve').",
                    "Ollama Offline"
            );
        } else {
            ollamaModelCombo.removeAllItems();
            for (String m : models) {
                ollamaModelCombo.addItem(m);
            }
            ollamaModelCombo.setSelectedIndex(0);
            Messages.showInfoMessage("Discovered " + models.size() + " installed model(s) in Ollama.", "Models Updated");
        }
    }

    private void testOllamaConnection() {
        int port = (int) ollamaPortSpinner.getValue();
        List<String> models = PluginSettings.getOllamaModels(port);
        if (!models.isEmpty()) {
            Messages.showInfoMessage("Successfully connected to Ollama on port " + port + "!\n" +
                    "Found models: " + String.join(", ", models), "Connection Successful");
        } else {
            try {
                URL url = new URL("http://127.0.0.1:" + port + "/api/tags");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                if (conn.getResponseCode() == 200) {
                    Messages.showInfoMessage("Connected to Ollama on port " + port + "!\nNo models installed yet.", "Connection OK");
                    return;
                }
            } catch (Exception ignored) {}
            Messages.showErrorDialog("Could not connect to Ollama on port " + port + ".\nPlease start Ollama with 'ollama serve'.", "Connection Failed");
        }
    }

    private void testLlamaConnection() {
        int port = (int) llamaPortSpinner.getValue();
        try {
            URL url = new URL("http://127.0.0.1:" + port + "/health");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            if (code == 200) {
                Messages.showInfoMessage("Connected to llama-server on port " + port + "!", "Connection OK");
            } else {
                Messages.showWarningDialog("Server returned HTTP " + code, "Unexpected Response");
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(
                    "Could not connect to llama-server on port " + port + ".\n" +
                    "Make sure the server is running.\n\nError: " + ex.getMessage(),
                    "Connection Failed");
        }
    }

    private void setContainerEnabled(Container container, boolean enabled) {
        for (Component c : container.getComponents()) {
            c.setEnabled(enabled);
            if (c instanceof Container childContainer) {
                setContainerEnabled(childContainer, enabled);
            }
        }
    }

    private JPanel createSection(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new TitledBorder(title));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    private JPanel labeledRow(String label, JComponent... components) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 3));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (!label.isEmpty()) {
            JLabel l = new JLabel(label);
            l.setPreferredSize(new Dimension(130, 24));
            row.add(l);
        }
        for (JComponent c : components) row.add(c);
        return row;
    }

    @FunctionalInterface
    interface FieldSupplier { JTextField get(); }

    private JButton browseFileButton(String tooltip, FieldSupplier supplier) {
        JButton btn = new JButton("Browse...");
        btn.setToolTipText(tooltip);
        btn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (chooser.showOpenDialog(rootPanel) == JFileChooser.APPROVE_OPTION) {
                supplier.get().setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        return btn;
    }

    private String getSelectedOllamaModel() {
        Object item = ollamaModelCombo.getSelectedItem();
        return item != null ? item.toString().trim() : "qwen3.5:2b";
    }

    @Override
    public boolean isModified() {
        PluginSettings.State s = PluginSettings.getInstance().getState();
        assert s != null;
        return ollamaRadio.isSelected() != s.useOllama
                || (int) ollamaPortSpinner.getValue() != s.ollamaPort
                || !getSelectedOllamaModel().equals(s.ollamaModel)
                || !llamaServerField.getText().equals(s.llamaServerPath)
                || !modelPathField.getText().equals(s.modelPath)
                || (int) llamaPortSpinner.getValue() != s.serverPort
                || (int) contextSizeSpinner.getValue() != s.contextSize
                || (int) threadsSpinner.getValue() != s.threads
                || (int) gpuLayersSpinner.getValue() != s.gpuLayers
                || ((Double) temperatureSpinner.getValue()).floatValue() != s.temperature
                || ((Double) topPSpinner.getValue()).floatValue() != s.topP
                || (int) maxTokensSpinner.getValue() != s.maxTokens
                || keepAliveCheck.isSelected() != s.keepServerAlive
                || autoStartCheck.isSelected() != s.autoStartServer;
    }

    @Override
    public void apply() {
        PluginSettings.State s = PluginSettings.getInstance().getState();
        assert s != null;
        s.useOllama = ollamaRadio.isSelected();
        s.ollamaPort = (int) ollamaPortSpinner.getValue();
        s.ollamaModel = getSelectedOllamaModel();
        s.llamaServerPath = llamaServerField.getText().trim();
        s.modelPath = modelPathField.getText().trim();
        s.serverPort = (int) llamaPortSpinner.getValue();
        s.contextSize = (int) contextSizeSpinner.getValue();
        s.threads = (int) threadsSpinner.getValue();
        s.gpuLayers = (int) gpuLayersSpinner.getValue();
        s.temperature = ((Double) temperatureSpinner.getValue()).floatValue();
        s.topP = ((Double) topPSpinner.getValue()).floatValue();
        s.maxTokens = (int) maxTokensSpinner.getValue();
        s.keepServerAlive = keepAliveCheck.isSelected();
        s.autoStartServer = autoStartCheck.isSelected();
    }

    @Override
    public void reset() {
        PluginSettings.State s = PluginSettings.getInstance().getState();
        assert s != null;
        if (s.useOllama) {
            ollamaRadio.setSelected(true);
        } else {
            llamaRadio.setSelected(true);
        }
        ollamaPortSpinner.setValue(s.ollamaPort);
        populateOllamaModels(s.ollamaPort, s.ollamaModel);
        llamaServerField.setText(s.llamaServerPath);
        modelPathField.setText(s.modelPath);
        llamaPortSpinner.setValue(s.serverPort);
        contextSizeSpinner.setValue(s.contextSize);
        threadsSpinner.setValue(s.threads);
        gpuLayersSpinner.setValue(s.gpuLayers);
        temperatureSpinner.setValue((double) s.temperature);
        topPSpinner.setValue((double) s.topP);
        maxTokensSpinner.setValue(s.maxTokens);
        keepAliveCheck.setSelected(s.keepServerAlive);
        autoStartCheck.setSelected(s.autoStartServer);
    }
}
