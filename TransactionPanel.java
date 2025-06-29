package Transaction;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import DB.DBConnection;

import java.awt.*;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Date;
import java.text.SimpleDateFormat;

public class TransactionPanel extends JPanel {
    private JTable transactionHistoryTable;
    private DefaultTableModel transactionHistoryModel;
    private JTable highlySoldProfitTable;
    private DefaultTableModel highlySoldProfitModel;
    private JLabel summaryLabel;
    private JButton refreshButton;

    public TransactionPanel() {
        setLayout(new BorderLayout(10, 10));
        initializeUI();
        refreshData();
    }

    private void initializeUI() {
        summaryLabel = new JLabel("Loading transaction data...", SwingConstants.CENTER);
        summaryLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);
        summaryPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        refreshButton = new JButton("Refresh Data");
        refreshButton.addActionListener(e -> refreshData());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(refreshButton);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Transaction History", createTransactionHistoryPanel());
        tabbedPane.addTab("Highly Sold Product Profit", createHighlySoldProductProfitPanel());

        add(summaryPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private JPanel createTransactionHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        transactionHistoryModel = new DefaultTableModel(
                new Object[]{"Transaction ID", "Product", "Quantity Sold", "Sale Price", "Sale Date"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        transactionHistoryTable = new JTable(transactionHistoryModel);
        transactionHistoryTable.setRowHeight(25);
        panel.add(new JScrollPane(transactionHistoryTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createHighlySoldProductProfitPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Assuming profit is calculated as revenue (quantity * price) since cost_price is not in schema
        highlySoldProfitModel = new DefaultTableModel(
                new Object[]{"Rank", "Product", "Total Quantity Sold", "Total Revenue (Profit)"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        highlySoldProfitTable = new JTable(highlySoldProfitModel);
        highlySoldProfitTable.setRowHeight(25);
        panel.add(new JScrollPane(highlySoldProfitTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshData() {
        refreshButton.setEnabled(false);
        summaryLabel.setText("Loading data...");

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    updateSummary();
                    loadTransactionHistory();
                    loadHighlySoldProductProfit();
                } catch (SQLException ex) {
                    SwingUtilities.invokeLater(() -> {
                        summaryLabel.setText("Error loading data from database");
                        JOptionPane.showMessageDialog(TransactionPanel.this,
                                "Database error: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    });
                    ex.printStackTrace();
                }
                return null;
            }

            @Override
            protected void done() {
                refreshButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    private void updateSummary() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            // Updated 'Sales' to 'Sales1'
            String sql = "SELECT COUNT(*) as total_transactions, SUM(s.quantity * p.price) as total_revenue " +
                         "FROM Sales1 s JOIN products p ON s.product_id = p.id";

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
                    String summary = String.format(
                            "<html><div style='text-align: center;'>" +
                                    "<b>Transaction Summary</b><br>" +
                                    "Total Transactions: %d | " +
                                    "Total Revenue (Assumed Profit): %s" +
                                    "</div></html>",
                            rs.getInt("total_transactions"),
                            currencyFormat.format(rs.getDouble("total_revenue")));

                    SwingUtilities.invokeLater(() -> summaryLabel.setText(summary));
                }
            }
        }
    }

    private void loadTransactionHistory() throws SQLException {
        SwingUtilities.invokeLater(() -> transactionHistoryModel.setRowCount(0));

        // Updated 'Sales' to 'Sales1' and added 'sale_date'
        String sql = "SELECT s.id, p.name, s.quantity, (s.quantity * p.price) as sale_price, s.sale_date " +
                     "FROM Sales1 s JOIN products p ON s.product_id = p.id " +
                     "ORDER BY s.sale_date DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

            while (rs.next()) {
                Object[] row = {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("quantity"),
                        currencyFormat.format(rs.getDouble("sale_price")),
                        dateFormat.format(new Date(rs.getTimestamp("sale_date").getTime()))
                };
                SwingUtilities.invokeLater(() -> transactionHistoryModel.addRow(row));
            }
        }
    }

    private void loadHighlySoldProductProfit() throws SQLException {
        SwingUtilities.invokeLater(() -> highlySoldProfitModel.setRowCount(0));

        // Updated 'Sales' to 'Sales1'. Profit is assumed to be revenue.
        String sql = "SELECT p.name, SUM(s.quantity) as total_quantity_sold, SUM(s.quantity * p.price) as total_revenue " +
                     "FROM Sales1 s JOIN products p ON s.product_id = p.id " +
                     "GROUP BY p.name " +
                     "ORDER BY total_revenue DESC " + // Order by revenue for "profit"
                     "LIMIT 10"; // Top 10 highly sold by profit/revenue

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();

            for (int rank = 1; rs.next(); rank++) {
                Object[] row = {
                        rank,
                        rs.getString("name"),
                        rs.getInt("total_quantity_sold"),
                        currencyFormat.format(rs.getDouble("total_revenue"))
                };
                SwingUtilities.invokeLater(() -> highlySoldProfitModel.addRow(row));
            }
        }
    }
}