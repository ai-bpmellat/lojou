package com.aiagent.plugin.ui;

import javax.swing.*;
import java.awt.*;

/**
 * A JPanel that tracks the viewport width inside a JScrollPane.
 * Prevents horizontal scrolling and forces children to word-wrap within the visible width.
 */
public class ScrollablePanel extends JPanel implements Scrollable {

    public ScrollablePanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 20;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 100;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true; // Strictly lock to scroll pane viewport width
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
