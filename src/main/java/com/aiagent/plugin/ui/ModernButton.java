package com.aiagent.plugin.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A modern, anti-aliased button with smooth rounded corners, state colors, and crisp borders.
 */
public class ModernButton extends JButton {

    private final Color normalBg;
    private final Color hoverBg;
    private final Color pressedBg;
    private final Color borderColor;
    private final int cornerRadius;

    public ModernButton(String text, Color normalBg, Color hoverBg, Color fg, Color borderColor, int cornerRadius) {
        super(text);
        this.normalBg = normalBg;
        this.hoverBg = hoverBg;
        this.pressedBg = hoverBg != null ? hoverBg.darker() : normalBg.darker();
        this.borderColor = borderColor;
        this.cornerRadius = cornerRadius;

        setForeground(fg);
        setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(6, 14, 6, 14));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (isEnabled()) repaint();
            }
        });
    }

    public ModernButton(String text, Color normalBg, Color hoverBg, Color fg, Color borderColor) {
        this(text, normalBg, hoverBg, fg, borderColor, 8);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        ButtonModel model = getModel();
        Color bg = normalBg;

        if (!isEnabled()) {
            bg = new Color(normalBg.getRed(), normalBg.getGreen(), normalBg.getBlue(), 90);
        } else if (model.isPressed()) {
            bg = (hoverBg != null ? hoverBg : normalBg).darker();
        } else if (model.isRollover() && hoverBg != null) {
            bg = hoverBg;
        }

        // Fill rounded background
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // Draw border if defined
        if (borderColor != null && isEnabled()) {
            g2.setColor(model.isRollover() ? borderColor.brighter() : borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
