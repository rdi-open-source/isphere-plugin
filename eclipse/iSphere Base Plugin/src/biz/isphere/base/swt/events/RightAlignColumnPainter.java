/*******************************************************************************
 * Copyright (c) 2012-2026 iSphere Project Owners
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/

package biz.isphere.base.swt.events;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

/**
 * This class paints the values of the columns of a {@link Table} object
 * right-aligned. The columns that shall be painted right-aligned must be
 * registered by calling {@link #addRightAlignedColumn(TableColumn)}. The
 * RightAlignColumnPainter adds itself as a {@link Listener} to the table, when
 * the first column is registered.
 * <p>
 * SWT does not allow the alignment of the values of a column to be configured
 * separately from the alignment of the column header. Calling
 * {@link TableColumn#setAlignment(int)} with <code>SWT.RIGHT</code> would
 * right-align the header text, too. Therefore the native drawing of the text
 * is suppressed on the <code>EraseItem</code> event and the text is painted
 * right-aligned on the <code>PaintItem</code> event, which leaves the
 * alignment of the header untouched.
 * 
 * @author Thomas Raddatz
 */
public class RightAlignColumnPainter implements Listener {

    /** Space between the right border of a cell and its text. */
    public static final int DEFAULT_MARGIN = 5;

    private Table tableParent;
    private int margin;

    private Set<Integer> rightAlignedColumns;
    private boolean isListening;

    public RightAlignColumnPainter(Table tableParent) {
        this(tableParent, DEFAULT_MARGIN);
    }

    /**
     * Creates a painter that paints the values of the registered columns
     * right-aligned.
     * 
     * @param tableParent - table the columns belong to
     * @param margin - space between the right border of a cell and its text
     */
    public RightAlignColumnPainter(Table tableParent, int margin) {

        this.tableParent = tableParent;
        this.margin = margin;

        this.rightAlignedColumns = new HashSet<Integer>();
        this.isListening = false;
    }

    /**
     * Registers a column whose values are painted right-aligned.
     * 
     * @param column - column that is painted right-aligned
     */
    public void addRightAlignedColumn(TableColumn column) {

        if (column.getParent() != tableParent) {
            throw new IllegalArgumentException("Column does not belong to the table of this painter."); //$NON-NLS-1$
        }

        rightAlignedColumns.add(Integer.valueOf(tableParent.indexOf(column)));

        if (!isListening) {
            tableParent.addListener(SWT.EraseItem, this);
            tableParent.addListener(SWT.PaintItem, this);
            isListening = true;
        }
    }

    /**
     * Callback method that is called by the framework on 'erase' and 'paint'
     * events triggered by the table.
     */
    public void handleEvent(Event event) {

        if (!rightAlignedColumns.contains(Integer.valueOf(event.index))) {
            return;
        }

        if (event.type == SWT.EraseItem) {
            /*
             * Suppresses the native drawing of the text, because it is painted
             * right-aligned on the 'paint' event.
             */
            event.detail &= ~SWT.FOREGROUND;
        } else if (event.type == SWT.PaintItem) {
            paintRightAlignedText(event);
        }
    }

    /**
     * Paints the text of a cell right-aligned.
     */
    private void paintRightAlignedText(Event event) {

        TableItem item = (TableItem)event.item;
        String text = item.getText(event.index);

        GC gc = event.gc;
        Point size = gc.stringExtent(text);
        Rectangle bounds = item.getBounds(event.index);

        int x = bounds.x + bounds.width - size.x - margin;
        int y = bounds.y + (bounds.height - size.y) / 2;

        gc.drawString(text, x, y, true);
    }
}
