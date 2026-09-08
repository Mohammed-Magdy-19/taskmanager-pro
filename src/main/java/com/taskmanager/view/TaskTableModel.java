package com.taskmanager.view;

import com.taskmanager.model.Task;
import com.taskmanager.model.enums.Priority;
import com.taskmanager.model.enums.Status;
import com.taskmanager.view.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Table model managing task presentation in the main workspace table.
 * Provides custom pill badge renderers for Priority and Status, and zebra striping with hover highlight.
 */
public class TaskTableModel extends AbstractTableModel {

    private static final String[] COLUMN_NAMES = {
            "ID", "Title", "Priority", "Status", "Due Date", "Category"
    };

    public static final int COL_ID = 0;
    public static final int COL_TITLE = 1;
    public static final int COL_PRIORITY = 2;
    public static final int COL_STATUS = 3;
    public static final int COL_DUE_DATE = 4;
    public static final int COL_CATEGORY = 5;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private List<Task> tasks = new ArrayList<>();
    private int hoveredRow = -1;

    /**
     * Updates the tasks displayed in the table and refreshes the view.
     *
     * @param tasks the new list of tasks
     */
    public void setTasks(List<Task> tasks) {
        this.tasks = (tasks != null) ? new ArrayList<>(tasks) : new ArrayList<>();
        fireTableDataChanged();
    }

    /**
     * Retrieves the {@link Task} represented at the specified model row index.
     *
     * @param row the row index
     * @return the corresponding Task, or null if out of bounds
     */
    public Task getTaskAt(int row) {
        if (row >= 0 && row < tasks.size()) {
            return tasks.get(row);
        }
        return null;
    }

    @Override
    public int getRowCount() {
        return tasks.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case COL_ID -> Integer.class;
            case COL_PRIORITY -> Priority.class;
            case COL_STATUS -> Status.class;
            default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Task task = getTaskAt(rowIndex);
        if (task == null) {
            return "";
        }
        return switch (columnIndex) {
            case COL_ID -> task.getId();
            case COL_TITLE -> task.getTitle();
            case COL_PRIORITY -> task.getPriority();
            case COL_STATUS -> task.getStatus();
            case COL_DUE_DATE -> task.getDueDate() != null ? task.getDueDate().format(DATE_FORMATTER) : "-";
            case COL_CATEGORY -> (task.getCategory() != null && !task.getCategory().isBlank()) ? task.getCategory() : "-";
            default -> "";
        };
    }

    /**
     * Applies styled cell renderers, row heights, and hover tracking to a {@link JTable}.
     *
     * @param table the table to configure
     */
    public void configureTable(JTable table) {
        table.setModel(this);
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(AppTheme.FONT_BODY);
        table.getTableHeader().setFont(AppTheme.FONT_SUBHEADING);
        table.getTableHeader().setBackground(AppTheme.BG_CARD);
        table.getTableHeader().setForeground(AppTheme.TEXT_PRIMARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.BORDER_TABLE));

        ZebraCellRenderer defaultRenderer = new ZebraCellRenderer();
        table.setDefaultRenderer(String.class, defaultRenderer);
        table.setDefaultRenderer(Integer.class, defaultRenderer);
        table.setDefaultRenderer(Object.class, defaultRenderer);

        BadgeCellRenderer badgeRenderer = new BadgeCellRenderer();
        table.setDefaultRenderer(Priority.class, badgeRenderer);
        table.setDefaultRenderer(Status.class, badgeRenderer);

        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                table.repaint();
            }
        });
    }

    /**
     * Renderer providing zebra striping, hover feedback, and accessible contrast.
     */
    private class ZebraCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(AppTheme.FONT_BODY);
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

            if (isSelected) {
                setBackground(AppTheme.BG_TABLE_SELECTED);
                setForeground(AppTheme.PRIMARY);
            } else if (row == hoveredRow) {
                setBackground(AppTheme.BG_TABLE_HOVER);
                setForeground(AppTheme.TEXT_BODY);
            } else if (row % 2 == 1) {
                setBackground(AppTheme.BG_TABLE_ZEBRA);
                setForeground(AppTheme.TEXT_BODY);
            } else {
                setBackground(AppTheme.BG_CARD);
                setForeground(AppTheme.TEXT_BODY);
            }
            return this;
        }
    }

    /**
     * Renderer drawing scannable, rounded pill badges for Priority and Status enums.
     */
    private class BadgeCellRenderer implements TableCellRenderer {
        private final PillLabel pillLabel = new PillLabel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Color rowBg = isSelected ? AppTheme.BG_TABLE_SELECTED :
                    (row == hoveredRow ? AppTheme.BG_TABLE_HOVER :
                            (row % 2 == 1 ? AppTheme.BG_TABLE_ZEBRA : AppTheme.BG_CARD));

            pillLabel.setContainerBackground(rowBg);

            if (value instanceof Priority priority) {
                pillLabel.setText(priority.name());
                switch (priority) {
                    case HIGH -> pillLabel.setPillColors(AppTheme.PILL_HIGH_BG, AppTheme.PILL_HIGH_FG);
                    case MEDIUM -> pillLabel.setPillColors(AppTheme.PILL_MED_BG, AppTheme.PILL_MED_FG);
                    case LOW -> pillLabel.setPillColors(AppTheme.PILL_LOW_BG, AppTheme.PILL_LOW_FG);
                }
            } else if (value instanceof Status status) {
                pillLabel.setText(status.name().replace('_', ' '));
                switch (status) {
                    case PENDING -> pillLabel.setPillColors(AppTheme.PILL_PENDING_BG, AppTheme.PILL_PENDING_FG);
                    case IN_PROGRESS -> pillLabel.setPillColors(AppTheme.PILL_PROGRESS_BG, AppTheme.PILL_PROGRESS_FG);
                    case COMPLETED -> pillLabel.setPillColors(AppTheme.PILL_COMPLETED_BG, AppTheme.PILL_COMPLETED_FG);
                }
            } else {
                pillLabel.setText(value != null ? value.toString() : "-");
                pillLabel.setPillColors(AppTheme.STEP_INACTIVE, AppTheme.TEXT_MUTED);
            }
            return pillLabel;
        }
    }

    /**
     * Custom component rendering a centered rounded pill badge with WCAG AA verified contrast.
     */
    private static class PillLabel extends JLabel {
        private Color containerBg = AppTheme.BG_CARD;
        private Color pillBg = AppTheme.PRIMARY;
        private static final int PILL_HEIGHT = 24;
        private static final int PILL_ARC = 12;

        public PillLabel() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(AppTheme.FONT_PILL);
            setOpaque(false);
        }

        public void setContainerBackground(Color bg) {
            this.containerBg = bg;
        }

        public void setPillColors(Color bg, Color fg) {
            this.pillBg = bg;
            setForeground(fg);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(containerBg);
            g2.fillRect(0, 0, getWidth(), getHeight());

            int pillWidth = Math.min(getWidth() - 16, Math.max(76, getFontMetrics(getFont()).stringWidth(getText()) + 18));
            int x = (getWidth() - pillWidth) / 2;
            int y = (getHeight() - PILL_HEIGHT) / 2;

            g2.setColor(pillBg);
            g2.fillRoundRect(x, y, pillWidth, PILL_HEIGHT, PILL_ARC, PILL_ARC);

            g2.dispose();
            super.paintComponent(g);
        }
    }
}
