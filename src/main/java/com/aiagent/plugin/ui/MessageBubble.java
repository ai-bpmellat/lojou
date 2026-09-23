package com.aiagent.plugin.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Modern chat message card with rounded corners, role badges, and responsive word wrapping.
 */
public class MessageBubble extends JPanel {

    public enum Role { USER, AGENT, SYSTEM }

    private final JTextArea textArea;
    private final Role role;

    public MessageBubble(Role role, String text) {
        this.role = role;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(4, 4, 6, 4));

        // Rounded card container
        RoundedCard card = new RoundedCard(role);
        card.setLayout(new BorderLayout(0, 4));
        card.setBorder(new EmptyBorder(8, 12, 10, 12));

        // Header label badge
        JLabel headerLabel = new JLabel(roleTitle(role));
        headerLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        headerLabel.setForeground(roleBadgeColor(role));
        headerLabel.setBorder(new EmptyBorder(0, 0, 2, 0));
        card.add(headerLabel, BorderLayout.NORTH);

        // Text area with responsive wrapping
        textArea = new JTextArea(text) {
            @Override
            public Dimension getPreferredSize() {
                Container p = getParent();
                if (p != null && p.getWidth() > 40) {
                    setSize(p.getWidth() - 24, Short.MAX_VALUE);
                }
                return super.getPreferredSize();
            }
        };
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        textArea.setOpaque(false);
        textArea.setForeground(roleTextColor(role));
        textArea.setMargin(new Insets(0, 0, 0, 0));
        card.add(textArea, BorderLayout.CENTER);

        if (role == Role.USER) {
            // User message on the right with left margin
            JPanel rightContainer = new JPanel(new BorderLayout());
            rightContainer.setOpaque(false);
            rightContainer.setBorder(new EmptyBorder(0, 40, 0, 0));
            rightContainer.add(card, BorderLayout.CENTER);
            add(rightContainer, BorderLayout.CENTER);
        } else {
            // Agent / System message with right margin
            JPanel leftContainer = new JPanel(new BorderLayout());
            leftContainer.setOpaque(false);
            leftContainer.setBorder(new EmptyBorder(0, 0, 0, 20));
            leftContainer.add(card, BorderLayout.CENTER);
            add(leftContainer, BorderLayout.CENTER);
        }
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    /** Append text (used for incremental outputs). */
    public void appendText(String chunk) {
        textArea.setText(textArea.getText() + chunk);
        revalidate();
        repaint();
    }

    /** Replace the full text of this bubble. */
    public void setText(String text) {
        textArea.setText(text);
        revalidate();
        repaint();
    }

    // ─────────────────────────── Colors & Badges ─────────────────────────────

    private String roleTitle(Role role) {
        return switch (role) {
            case USER   -> "You";
            case AGENT  -> "lojou";
            case SYSTEM -> "System";
        };
    }

    private Color roleBadgeColor(Role role) {
        return switch (role) {
            case USER   -> ThemeColors.USER_BADGE_FG;
            case AGENT  -> ThemeColors.AGENT_BADGE_FG;
            case SYSTEM -> ThemeColors.SYSTEM_BADGE_FG;
        };
    }

    private Color roleTextColor(Role role) {
        return switch (role) {
            case USER   -> ThemeColors.USER_TEXT_FG;
            case AGENT  -> ThemeColors.AGENT_TEXT_FG;
            case SYSTEM -> ThemeColors.SYSTEM_TEXT_FG;
        };
    }

    // ─────────────────────────── Rounded Card ────────────────────────────────

    private static class RoundedCard extends JPanel {
        private final Role role;
        private final int radius = 12;

        RoundedCard(Role role) {
            this.role = role;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color bg;
            Color border;
            switch (role) {
                case USER -> {
                    bg = ThemeColors.USER_CARD_BG;
                    border = ThemeColors.USER_CARD_BORDER;
                }
                case AGENT -> {
                    bg = ThemeColors.AGENT_CARD_BG;
                    border = ThemeColors.AGENT_CARD_BORDER;
                }
                default -> { // SYSTEM
                    bg = ThemeColors.SYSTEM_CARD_BG;
                    border = ThemeColors.SYSTEM_CARD_BORDER;
                }
            }

            // Fill card
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            // Draw border
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
