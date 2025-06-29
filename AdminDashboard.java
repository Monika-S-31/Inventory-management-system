package AdminForm;

import javax.swing.*;

import Inventory_Dashboard.InventoryDashboard;
import Product.ProductPanel;
import Transaction.TransactionPanel;

import java.awt.*;

public class AdminDashboard extends JFrame {
    public AdminDashboard(String username) {
        setTitle("Admin Dashboard - Welcome " + username);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Product Management", new ProductPanel());
        tabbedPane.addTab("Inventory Dashboard", new InventoryDashboard());
        tabbedPane.addTab("Transactions & Profit", new TransactionPanel());

        add(tabbedPane, BorderLayout.CENTER);

        setVisible(true);
    }
}