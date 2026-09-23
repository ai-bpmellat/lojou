package com.aiagent.plugin.ui;

import com.intellij.ui.JBColor;
import java.awt.Color;

/**
 * Adaptive color tokens that automatically switch between Light and Dark themes
 * according to IntelliJ's active Look & Feel.
 */
public final class ThemeColors {

    private ThemeColors() {}

    // ─── Surfaces ─────────────────────────────────────────────────────────────
    /** Main tool window background */
    public static final JBColor WINDOW_BG = new JBColor(new Color(0xFFFFFF), new Color(0x0F1013));

    /** Messages scrollable viewport background */
    public static final JBColor MESSAGES_BG = new JBColor(new Color(0xF8FAFC), new Color(0x0F1013));

    /** Header panel background */
    public static final JBColor HEADER_BG = new JBColor(new Color(0xFFFFFF), new Color(0x16171B));

    /** Input container background */
    public static final JBColor INPUT_CONTAINER_BG = new JBColor(new Color(0xFFFFFF), new Color(0x16171B));

    /** Input text area background */
    public static final JBColor INPUT_FIELD_BG = new JBColor(new Color(0xFFFFFF), new Color(0x1D1E24));

    // ─── Borders & Dividers ───────────────────────────────────────────────────
    /** Subtle divider line (header / input separators) */
    public static final JBColor BORDER = new JBColor(new Color(0xE2E8F0), new Color(0x262830));

    /** Input field border */
    public static final JBColor INPUT_BORDER = new JBColor(new Color(0xCBD5E1), new Color(0x2D3039));

    // ─── Text & Typography ────────────────────────────────────────────────────
    /** Primary title ("lojou") */
    public static final JBColor TITLE_TEXT = new JBColor(new Color(0x1E293B), new Color(0xC084FC));

    /** Muted labels (Offline status, status text) */
    public static final JBColor MUTED_TEXT = new JBColor(new Color(0x64748B), new Color(0x9CA3AF));

    /** Input field foreground text */
    public static final JBColor INPUT_TEXT = new JBColor(new Color(0x0F172A), new Color(0xF3F4F6));

    /** Input caret color */
    public static final JBColor INPUT_CARET = new JBColor(new Color(0x4F46E5), new Color(0xC084FC));

    /** Online indicator dot */
    public static final Color STATUS_DOT = new Color(0x10B981); // Emerald green

    // ─── Header Buttons ───────────────────────────────────────────────────────
    // Settings button
    public static final JBColor SETTINGS_BTN_BG = new JBColor(new Color(0xF1F5F9), new Color(0x22252C));
    public static final JBColor SETTINGS_BTN_HOVER = new JBColor(new Color(0xE2E8F0), new Color(0x2D323C));
    public static final JBColor SETTINGS_BTN_FG = new JBColor(new Color(0x334155), new Color(0xE2E8F0));
    public static final JBColor SETTINGS_BTN_BORDER = new JBColor(new Color(0xCBD5E1), new Color(0x3B404D));

    // Clear button
    public static final JBColor CLEAR_BTN_BG = new JBColor(new Color(0xF8FAFC), new Color(0x1C1E24));
    public static final JBColor CLEAR_BTN_HOVER = new JBColor(new Color(0xF1F5F9), new Color(0x272A33));
    public static final JBColor CLEAR_BTN_FG = new JBColor(new Color(0x64748B), new Color(0x94A3B8));
    public static final JBColor CLEAR_BTN_BORDER = new JBColor(new Color(0xE2E8F0), new Color(0x2E323D));

    // Stop button
    public static final JBColor STOP_BTN_BG = new JBColor(new Color(0xFEE2E2), new Color(0x3B1818));
    public static final JBColor STOP_BTN_HOVER = new JBColor(new Color(0xFECACA), new Color(0x5A1F1F));
    public static final JBColor STOP_BTN_FG = new JBColor(new Color(0xDC2626), new Color(0xFCA5A5));
    public static final JBColor STOP_BTN_BORDER = new JBColor(new Color(0xFCA5A5), new Color(0x7F1D1D));

    // Send button (vibrant indigo)
    public static final Color SEND_BTN_BG = new Color(0x4F46E5);
    public static final Color SEND_BTN_HOVER = new Color(0x6366F1);
    public static final Color SEND_BTN_FG = Color.WHITE;
    public static final Color SEND_BTN_BORDER = new Color(0x3730A3);

    // ─── Message Cards ────────────────────────────────────────────────────────
    // User bubble
    public static final Color USER_CARD_BG = new Color(0x2563EB); // Vibrant royal blue
    public static final Color USER_CARD_BORDER = new Color(0x1D4ED8);
    public static final Color USER_BADGE_FG = new Color(0xDBEAFE);
    public static final Color USER_TEXT_FG = Color.WHITE;

    // Agent bubble
    public static final JBColor AGENT_CARD_BG = new JBColor(new Color(0xFFFFFF), new Color(0x181A20));
    public static final JBColor AGENT_CARD_BORDER = new JBColor(new Color(0xE2E8F0), new Color(0x2E323D));
    public static final JBColor AGENT_BADGE_FG = new JBColor(new Color(0x4F46E5), new Color(0xC084FC));
    public static final JBColor AGENT_TEXT_FG = new JBColor(new Color(0x0F172A), new Color(0xF1F5F9));

    // System bubble
    public static final JBColor SYSTEM_CARD_BG = new JBColor(new Color(0xF0FDF4), new Color(0x0F2018));
    public static final JBColor SYSTEM_CARD_BORDER = new JBColor(new Color(0xBBF7D0), new Color(0x065F46));
    public static final JBColor SYSTEM_BADGE_FG = new JBColor(new Color(0x15803D), new Color(0x6EE7B7));
    public static final JBColor SYSTEM_TEXT_FG = new JBColor(new Color(0x166534), new Color(0xD1FAE5));
}
