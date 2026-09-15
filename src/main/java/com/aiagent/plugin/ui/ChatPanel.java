package com.aiagent.plugin.ui;

import com.aiagent.plugin.agent.AgentContext;
import com.aiagent.plugin.agent.AgentCore;
import com.aiagent.plugin.settings.SettingsConfigurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

/**
 * Main chat panel for offAiAgent Tool Window.
 */
public class ChatPanel extends JPanel {

    private final Project project;

    // UI Components
    private ScrollablePanel messagesPanel;
    private JScrollPane   scrollPane;
    private JTextArea     inputField;
    private ModernButton  sendButton;
    private ModernButton  clearButton;
    private ModernButton  stopButton;
    private JLabel        statusLabel;

    // ── Agent & Threading ─────────────────────────────────────────────────
    private ExecutorService executor = null;
    private Future<?> currentTask = null;
    private volatile boolean running = false;
    private MessageBubble currentAgentBubble = null;

    private synchronized ExecutorService getExecutor() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "offaiagent-worker");
                t.setDaemon(true);
                return t;
            });
        }
        return executor;
    }

    public ChatPanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout(0, 0));
        setBackground(ThemeColors.WINDOW_BG);

        // ── Header Bar ────────────────────────────────────────────────────
        JPanel header = buildHeader();
        add(header, BorderLayout.NORTH);

        // ── Messages Area ─────────────────────────────────────────────────
        messagesPanel = new ScrollablePanel();
        messagesPanel.setBackground(ThemeColors.MESSAGES_BG);
        messagesPanel.setBorder(new EmptyBorder(8, 6, 8, 6));

        scrollPane = new JScrollPane(messagesPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ThemeColors.MESSAGES_BG);
        scrollPane.getViewport().setBackground(ThemeColors.MESSAGES_BG);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // No horizontal overflow!
        add(scrollPane, BorderLayout.CENTER);

        // ── Input Area ────────────────────────────────────────────────────
        JPanel inputPanel = buildInputPanel();
        add(inputPanel, BorderLayout.SOUTH);

        // ── Listen for IntelliJ Theme Switches (Light <-> Dark) ───────────
        try {
            com.intellij.openapi.application.ApplicationManager.getApplication()
                    .getMessageBus().connect(project)
                    .subscribe(com.intellij.ide.ui.LafManagerListener.TOPIC, source -> {
                        SwingUtilities.invokeLater(() -> {
                            repaint();
                            revalidate();
                        });
                    });
        } catch (Exception ignored) {}

        // ── Welcome message ────────────────────────────────────────────────
        appendMessage(MessageBubble.Role.SYSTEM,
                "offAiAgent is ready! (Offline Mode)\n\n" +
                "Type your request and press Send or Ctrl+Enter.\n\n" +
                "Examples:\n" +
                "  • Write a Java class for a binary search tree\n" +
                "  • Add logging to all methods in UserService.java\n" +
                "  • Find all TODO comments in the project\n" +
                "  • Refactor the selected code to use streams");
    }

    // ─────────────────────────── UI Builders ─────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(ThemeColors.HEADER_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeColors.BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));

        // Line 1: Title and offline status
        JPanel line1 = new JPanel(new BorderLayout());
        line1.setOpaque(false);

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleBox.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setForeground(ThemeColors.STATUS_DOT);
        dot.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        titleBox.add(dot);

        JLabel title = new JLabel("offAiAgent");
        title.setForeground(ThemeColors.TITLE_TEXT);
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        titleBox.add(title);

        JLabel modeBadge = new JLabel("|  Offline");
        modeBadge.setForeground(ThemeColors.MUTED_TEXT);
        modeBadge.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        titleBox.add(modeBadge);

        line1.add(titleBox, BorderLayout.WEST);

        // Line 2: Actions toolbar (Settings, Clear, Stop)
        JPanel line2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        line2.setOpaque(false);

        ModernButton settingsButton = new ModernButton("⚙ Settings",
                ThemeColors.SETTINGS_BTN_BG, ThemeColors.SETTINGS_BTN_HOVER,
                ThemeColors.SETTINGS_BTN_FG, ThemeColors.SETTINGS_BTN_BORDER, 6);
        settingsButton.setToolTipText("Open offAiAgent configuration");
        settingsButton.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        settingsButton.setBorder(new EmptyBorder(3, 8, 3, 8));
        settingsButton.addActionListener(e -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsConfigurable.class);
        });
        line2.add(settingsButton);

        clearButton = new ModernButton("Clear",
                ThemeColors.CLEAR_BTN_BG, ThemeColors.CLEAR_BTN_HOVER,
                ThemeColors.CLEAR_BTN_FG, ThemeColors.CLEAR_BTN_BORDER, 6);
        clearButton.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        clearButton.setBorder(new EmptyBorder(3, 8, 3, 8));
        clearButton.addActionListener(e -> clearChat());
        line2.add(clearButton);

        stopButton = new ModernButton("Stop",
                ThemeColors.STOP_BTN_BG, ThemeColors.STOP_BTN_HOVER,
                ThemeColors.STOP_BTN_FG, ThemeColors.STOP_BTN_BORDER, 6);
        stopButton.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        stopButton.setBorder(new EmptyBorder(3, 8, 3, 8));
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopAgent());
        line2.add(stopButton);

        header.add(line1);
        header.add(Box.createVerticalStrut(5));
        header.add(line2);
        return header;
    }

    private JPanel buildInputPanel() {
        JPanel container = new JPanel(new BorderLayout(0, 0));
        container.setBackground(ThemeColors.INPUT_CONTAINER_BG);
        container.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeColors.BORDER));

        // Status label
        statusLabel = new JLabel(" ");
        statusLabel.setForeground(ThemeColors.MUTED_TEXT);
        statusLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        statusLabel.setBorder(new EmptyBorder(4, 12, 2, 12));
        container.add(statusLabel, BorderLayout.NORTH);

        // Input text area
        inputField = new JTextArea(3, 30);
        inputField.setLineWrap(true);
        inputField.setWrapStyleWord(true);
        inputField.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        inputField.setBackground(ThemeColors.INPUT_FIELD_BG);
        inputField.setForeground(ThemeColors.INPUT_TEXT);
        inputField.setCaretColor(ThemeColors.INPUT_CARET);
        inputField.setSelectedTextColor(Color.WHITE);
        inputField.setSelectionColor(new Color(0x6366F1));
        inputField.setBorder(new EmptyBorder(8, 12, 8, 12));

        // Enter shortcuts:
        // Ctrl+Enter / Meta+Enter -> Send message
        // Shift+Enter -> Insert new line (\n)
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isControlDown() || e.isMetaDown()) {
                        e.consume();
                        sendMessage();
                    } else if (e.isShiftDown()) {
                        e.consume();
                        inputField.replaceSelection("\n");
                    }
                }
            }
        });

        JScrollPane inputScroll = new JScrollPane(inputField);
        inputScroll.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(4, 10, 4, 10),
                BorderFactory.createLineBorder(ThemeColors.INPUT_BORDER, 1, true)
        ));
        inputScroll.setBackground(ThemeColors.INPUT_FIELD_BG);
        container.add(inputScroll, BorderLayout.CENTER);

        // Send button bar
        sendButton = new ModernButton("Send  Ctrl+Enter",
                ThemeColors.SEND_BTN_BG, ThemeColors.SEND_BTN_HOVER,
                ThemeColors.SEND_BTN_FG, ThemeColors.SEND_BTN_BORDER, 8);
        sendButton.addActionListener(e -> sendMessage());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        btnPanel.setOpaque(false);
        btnPanel.add(sendButton);
        container.add(btnPanel, BorderLayout.SOUTH);

        return container;
    }

    // ─────────────────────────── Agent Interaction ───────────────────────────

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty() || running) return;

        inputField.setText("");
        appendMessage(MessageBubble.Role.USER, text);

        setRunning(true);
        statusLabel.setText("Thinking...");

        // Create a placeholder bubble for the agent's response
        currentAgentBubble = appendMessage(MessageBubble.Role.AGENT, "Thinking...");

        Runnable worker = () -> {
            try {
                AgentContext context = AgentContext.from(project);
                AgentCore agent = new AgentCore(
                        // onStep
                        stepMsg -> SwingUtilities.invokeLater(() -> {
                            statusLabel.setText(stepMsg.contains("\n") ? stepMsg.substring(stepMsg.lastIndexOf("\n")).trim() : stepMsg);
                            if (currentAgentBubble != null) {
                                currentAgentBubble.setText(stepMsg);
                            }
                            scrollToBottom();
                        }),
                        // onDone
                        answer -> SwingUtilities.invokeLater(() -> {
                            if (currentAgentBubble != null) {
                                currentAgentBubble.setText(answer);
                            }
                            statusLabel.setText("Done");
                            setRunning(false);
                            scrollToBottom();
                        }),
                        // onError
                        error -> SwingUtilities.invokeLater(() -> {
                            if (currentAgentBubble != null) {
                                currentAgentBubble.setText(error);
                            }
                            statusLabel.setText("Error");
                            setRunning(false);
                            scrollToBottom();
                        })
                );
                agent.process(text, context);
            } catch (Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    if (currentAgentBubble != null) {
                        currentAgentBubble.setText("Error: " + (t.getMessage() != null ? t.getMessage() : t.toString()));
                    }
                    statusLabel.setText("Error");
                    setRunning(false);
                    scrollToBottom();
                });
            }
        };

        try {
            currentTask = getExecutor().submit(worker);
        } catch (Exception e) {
            // Self-heal: reset executor and submit again
            executor = null;
            currentTask = getExecutor().submit(worker);
        }
    }

    private synchronized void stopAgent() {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
        executor = null;
        if (currentAgentBubble != null) {
            currentAgentBubble.setText("Stopped by user.");
        }
        statusLabel.setText("Stopped.");
        setRunning(false);
    }

    private void clearChat() {
        messagesPanel.removeAll();
        messagesPanel.revalidate();
        messagesPanel.repaint();
        statusLabel.setText(" ");
    }

    // ─────────────────────────── UI Helpers ──────────────────────────────────

    private MessageBubble appendMessage(MessageBubble.Role role, String text) {
        MessageBubble bubble = new MessageBubble(role, text);
        messagesPanel.add(bubble);
        messagesPanel.add(Box.createVerticalStrut(6));
        messagesPanel.revalidate();
        messagesPanel.repaint();
        scrollToBottom();
        return bubble;
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scrollPane.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void setRunning(boolean r) {
        running = r;
        sendButton.setEnabled(!r);
        stopButton.setEnabled(r);
        inputField.setEnabled(!r);
    }
}
