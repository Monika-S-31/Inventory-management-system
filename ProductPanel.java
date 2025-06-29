package Product;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import DB.DBConnection;
import Inventory_Dashboard.InventoryDashboard;

import java.awt.*;
import java.awt.event.*;
import java.sql.*;

public class ProductPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtName, txtCategory, txtQty, txtPrice, txtSupplier, txtSearch;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnDashboard;

    public ProductPanel() {
        setLayout(new BorderLayout());

        // Form Panel using GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtName = new JTextField(20);
        txtCategory = new JTextField(20);
        txtQty = new JTextField(20);
        txtPrice = new JTextField(20);
        txtSupplier = new JTextField(20);
        txtSearch = new JTextField(20);

        // Add components with GridBagLayout
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Product Name:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new JLabel("Category:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtCategory, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new JLabel("Quantity:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtQty, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Price:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtPrice, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        formPanel.add(new JLabel("Supplier:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtSupplier, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        formPanel.add(new JLabel("Search:"), gbc);
        gbc.gridx = 1;
        formPanel.add(txtSearch, gbc);

        // Button Panel
        JPanel btnPanel = new JPanel();
        btnAdd = new JButton("Add");
        btnUpdate = new JButton("Update");
        btnDelete = new JButton("Delete");
        btnClear = new JButton("Clear");
        btnDashboard = new JButton("View Dashboard");
        btnPanel.add(btnAdd);
        btnPanel.add(btnUpdate);
        btnPanel.add(btnDelete);
        btnPanel.add(btnClear);
        btnPanel.add(btnDashboard);

        // Table setup
        model = new DefaultTableModel(new String[] { "ID", "Name", "Category", "Qty", "Price", "Supplier" }, 0);
        table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);

        add(formPanel, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        loadProducts();

        // Event listeners
        btnAdd.addActionListener(e -> addProduct());
        btnUpdate.addActionListener(e -> updateProduct());
        btnDelete.addActionListener(e -> deleteProduct());
        btnClear.addActionListener(e -> clearFields());
        btnDashboard.addActionListener(e -> showInventoryDashboard());
        table.getSelectionModel().addListSelectionListener(e -> fillFormFromTable());
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                searchProducts(txtSearch.getText());
            }
        });
    }

    private void showInventoryDashboard() {
        InventoryDashboard dashboard = new InventoryDashboard();
        JFrame frame = new JFrame("Inventory Dashboard");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(dashboard);
        frame.setSize(900, 500);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void recordSale(int productId, int quantity) {
        try (Connection conn = DBConnection.getConnection()) {
            // Changed table name from 'Sales' to 'Sales1'
            String sql = "INSERT INTO Sales1 (product_id, quantity) VALUES (?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, productId);
            pst.setInt(2, quantity);
            pst.executeUpdate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadProducts() {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM products";
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("supplier")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void searchProducts(String keyword) {
        model.setRowCount(0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "SELECT * FROM products WHERE name LIKE ? OR category LIKE ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, "%" + keyword + "%");
            pst.setString(2, "%" + keyword + "%");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[] {
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getString("supplier")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void addProduct() {
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO products (name, category, quantity, price, supplier) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, txtName.getText());
            pst.setString(2, txtCategory.getText());
            pst.setInt(3, Integer.parseInt(txtQty.getText()));
            pst.setDouble(4, Double.parseDouble(txtPrice.getText()));
            pst.setString(5, txtSupplier.getText());
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Product added");
            loadProducts();
            clearFields();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void updateProduct() {
        int row = table.getSelectedRow();
        if (row == -1)
            return;
        int id = (int) model.getValueAt(row, 0);
        try (Connection conn = DBConnection.getConnection()) {
            String sql = "UPDATE products SET name=?, category=?, quantity=?, price=?, supplier=? WHERE id=?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, txtName.getText());
            pst.setString(2, txtCategory.getText());
            pst.setInt(3, Integer.parseInt(txtQty.getText()));
            pst.setDouble(4, Double.parseDouble(txtPrice.getText()));
            pst.setString(5, txtSupplier.getText());
            pst.setInt(6, id);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Product updated");
            loadProducts();
            clearFields();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void deleteProduct() {
        int row = table.getSelectedRow();
        if (row == -1)
            return;
        int id = (int) model.getValueAt(row, 0);
        int currentQty = (int) model.getValueAt(row, 3);

        try {
            // Record remaining stock as sale before deletion
            if (currentQty > 0) {
                recordSale(id, currentQty);
            }

            try (Connection conn = DBConnection.getConnection()) {
                String sql = "DELETE FROM products WHERE id=?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, id);
                pst.executeUpdate();
                JOptionPane.showMessageDialog(this, "Product deleted and sale recorded");
                loadProducts();
                clearFields();
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private void fillFormFromTable() {
        int row = table.getSelectedRow();
        if (row != -1) {
            txtName.setText(model.getValueAt(row, 1).toString());
            txtCategory.setText(model.getValueAt(row, 2).toString());
            txtQty.setText(model.getValueAt(row, 3).toString());
            txtPrice.setText(model.getValueAt(row, 4).toString());
            txtSupplier.setText(model.getValueAt(row, 5).toString());
        }
    }

    private void clearFields() {
        txtName.setText("");
        txtCategory.setText("");
        txtQty.setText("");
        txtPrice.setText("");
        txtSupplier.setText("");
        table.clearSelection();
    }
}

