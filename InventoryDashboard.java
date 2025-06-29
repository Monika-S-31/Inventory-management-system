package Inventory_Dashboard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import DB.DBConnection;

import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;

public class InventoryDashboard extends JPanel {
    private JTable lowStockTable, bestSellersTable;
    private DefaultTableModel lowStockModel, bestSellersModel;
    private JLabel summaryLabel;
    private JButton refreshBtn;

    public InventoryDashboard() {
        setLayout(new BorderLayout(10, 10));
        initializeUI();
        refreshData();
    }

    private void initializeUI() {
        summaryLabel = new JLabel("Loading inventory data...", SwingConstants.CENTER);
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        refreshBtn = new JButton("Refresh Data");
        refreshBtn.addActionListener(e -> refreshData());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(refreshBtn);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Low Stock Alerts", createLowStockPanel());
        tabbedPane.addTab("Best Sellers", createBestSellersPanel());

        add(summaryPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createLowStockPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        lowStockModel = new DefaultTableModel(
                new Object[] { "ID", "Product", "Category", "Current", "Threshold", "Status" }, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 5 ? Icon.class : super.getColumnClass(columnIndex);
            }
        };

        lowStockTable = new JTable(lowStockModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        lowStockTable.setRowHeight(25);
        lowStockTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        lowStockTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        lowStockTable.getColumnModel().getColumn(4).setPreferredWidth(80);
        lowStockTable.getColumnModel().getColumn(5).setPreferredWidth(30);

        panel.add(new JScrollPane(lowStockTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBestSellersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        bestSellersModel = new DefaultTableModel(
                new Object[] { "Rank", "Product", "Sales", "Revenue" }, 0);

        bestSellersTable = new JTable(bestSellersModel) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        bestSellersTable.setRowHeight(25);

        panel.add(new JScrollPane(bestSellersTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshData() {
        refreshBtn.setEnabled(false);
        summaryLabel.setText("Loading data...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    updateSummary();
                    loadLowStockItems();
                    loadBestSellers();
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        summaryLabel.setText("Error loading data from database");
                        JOptionPane.showMessageDialog(InventoryDashboard.this,
                                "Database error: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                refreshBtn.setEnabled(true);
            }
        };
        worker.execute();
    }

    private void updateSummary() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT " +
                    "COUNT(*) as total_products, " +
                    "SUM(CASE WHEN quantity < stock_threshold THEN 1 ELSE 0 END) as low_stock, " +
                    "SUM(CASE WHEN quantity < stock_threshold * 0.3 THEN 1 ELSE 0 END) as critical_stock " +
                    "FROM products";

            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    String summary = String.format(
                            "<html><div style='text-align: center;'>" +
                                    "<b>Inventory Summary</b><br>" +
                                    "Total Products: %d | " +
                                    "<span style='color: orange;'>Low Stock: %d</span> | " +
                                    "<span style='color: red;'>Critical: %d</span>" +
                                    "</div></html>",
                            rs.getInt("total_products"),
                            rs.getInt("low_stock"),
                            rs.getInt("critical_stock"));

                    SwingUtilities.invokeLater(() -> summaryLabel.setText(summary));
                }
            }
        }
    }

    private void loadLowStockItems() throws SQLException {
        SwingUtilities.invokeLater(() -> lowStockModel.setRowCount(0));

        String sql = "SELECT id, name, category, quantity, stock_threshold " +
                "FROM products WHERE quantity < stock_threshold " +
                "ORDER BY (quantity/stock_threshold) ASC";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement(sql);
                ResultSet rs = pst.executeQuery()) {

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getInt("stock_threshold"),
                        getStatusIcon((double) rs.getInt("quantity") / rs.getInt("stock_threshold"))
                };
                SwingUtilities.invokeLater(() -> lowStockModel.addRow(row));
            }
        }
    }

    private Icon getStatusIcon(double ratio) {
        if (ratio < 0.3)
            return UIManager.getIcon("OptionPane.errorIcon");
        if (ratio < 0.5)
            return UIManager.getIcon("OptionPane.warningIcon");
        return UIManager.getIcon("OptionPane.informationIcon");
    }

    private void loadBestSellers() throws SQLException {
        SwingUtilities.invokeLater(() -> bestSellersModel.setRowCount(0));

        // Changed table name from 'sales' to 'Sales1'
        String sql = "SELECT p.name, SUM(s.quantity) as total_sales, " +
                "SUM(s.quantity * p.price) as revenue " +
                "FROM products p LEFT JOIN Sales1 s ON p.id = s.product_id " +
                "GROUP BY p.name " +
                "ORDER BY total_sales DESC " +
                "LIMIT 10";

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pst = conn.prepareStatement(sql);
                ResultSet rs = pst.executeQuery()) {

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

            for (int rank = 1; rs.next(); rank++) {
                Object[] row = {
                        rank,
                        rs.getString("name"),
                        rs.getInt("total_sales"),
                        rs.getDouble("revenue") > 0 ? currencyFormat.format(rs.getDouble("revenue")) : "N/A"
                };
                SwingUtilities.invokeLater(() -> bestSellersModel.addRow(row));
            }
        }
    }
}
