/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/**
 * This is an override of ScrollPaneLayout which fixes a bug that occurs when a
 * window containing a scroll pane is repacked.  When that occurs,
 * viewport.getViewSize() returns the existing size of the client view window
 * rather than its preferred size, which can cause problems because the
 * existing size is not necessarily the final client size after the repack. To
 * fix this, this class reimplements the preferredLayoutSize method to always
 * use the preferred size of the view client.
 */
public class ScrollPaneLayout extends javax.swing.ScrollPaneLayout {

    public Dimension preferredLayoutSize(Container parent) 
    {
	/* Sync the (now obsolete) policy fields with the
	 * JScrollPane.
	 */
	JScrollPane scrollPane = (JScrollPane)parent;
	vsbPolicy = scrollPane.getVerticalScrollBarPolicy();
	hsbPolicy = scrollPane.getHorizontalScrollBarPolicy();

	Insets insets = parent.getInsets();
	int prefWidth = insets.left + insets.right;
	int prefHeight = insets.top + insets.bottom;

	/* Note that viewport.getViewSize() is equivalent to 
	 * viewport.getView().getPreferredSize() modulo a null
	 * view or a view whose size was explicitly set.
	 */

	Dimension extentSize = null;
	Dimension viewSize = null;
	Component view = null;

	if (viewport !=  null) {
	    extentSize = viewport.getPreferredSize();
            //Bug fix: always use the preferred size for the client.
	    //viewSize = viewport.getViewSize();
	    viewSize = viewport.getView().getPreferredSize();
	    view = viewport.getView();
	}

	/* If there's a viewport add its preferredSize.
	 */

	if (extentSize != null) {
	    prefWidth += extentSize.width;
	    prefHeight += extentSize.height;
	}

	/* If there's a JScrollPane.viewportBorder, add its insets.
	 */

	Border viewportBorder = scrollPane.getViewportBorder();
	if (viewportBorder != null) {
	    Insets vpbInsets = viewportBorder.getBorderInsets(parent);
	    prefWidth += vpbInsets.left + vpbInsets.right;
	    prefHeight += vpbInsets.top + vpbInsets.bottom;
	}

	/* If a header exists and it's visible, factor its
	 * preferred size in.
	 */

	if ((rowHead != null) && rowHead.isVisible()) {
	    prefWidth += rowHead.getPreferredSize().width;
	}

	if ((colHead != null) && colHead.isVisible()) {
	    prefHeight += colHead.getPreferredSize().height;
	}

	/* If a scrollbar is going to appear, factor its preferred size in.
	 * If the scrollbars policy is AS_NEEDED, this can be a little
	 * tricky:
	 * 
	 * - If the view is a Scrollable then scrollableTracksViewportWidth
	 * and scrollableTracksViewportHeight can be used to effectively 
	 * disable scrolling (if they're true) in their respective dimensions.
	 * 
	 * - Assuming that a scrollbar hasn't been disabled by the 
	 * previous constraint, we need to decide if the scrollbar is going 
	 * to appear to correctly compute the JScrollPanes preferred size.
	 * To do this we compare the preferredSize of the viewport (the 
	 * extentSize) to the preferredSize of the view.  Although we're
	 * not responsible for laying out the view we'll assume that the 
	 * JViewport will always give it its preferredSize.
	 */

	if ((vsb != null) && (vsbPolicy != VERTICAL_SCROLLBAR_NEVER)) {
	    if (vsbPolicy == VERTICAL_SCROLLBAR_ALWAYS) {
		prefWidth += vsb.getPreferredSize().width;
	    }
	    else if ((viewSize != null) && (extentSize != null)) {
		boolean canScroll = true;
		if (view instanceof Scrollable) {
		    canScroll = !((Scrollable)view).getScrollableTracksViewportHeight();
		}
		if (canScroll && (viewSize.height > extentSize.height)) {
		    prefWidth += vsb.getPreferredSize().width;
		}
	    }
	}

	if ((hsb != null) && (hsbPolicy != HORIZONTAL_SCROLLBAR_NEVER)) {
	    if (hsbPolicy == HORIZONTAL_SCROLLBAR_ALWAYS) {
		prefHeight += hsb.getPreferredSize().height;
	    }
	    else if ((viewSize != null) && (extentSize != null)) {
		boolean canScroll = true;
		if (view instanceof Scrollable) {
		    canScroll = !((Scrollable)view).getScrollableTracksViewportWidth();
		}
		if (canScroll && (viewSize.width > extentSize.width)) {
		    prefHeight += hsb.getPreferredSize().height;
		}
	    }
	}
        Dimension dim = new Dimension(prefWidth, prefHeight);
	return dim;
    }
}
