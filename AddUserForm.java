package User_Form;

import javax.swing.*;

import DB.DBConnection;

import java.awt.*;
import java.sql.*;

public class AddUserForm extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    public AddUserForm() {
        setTitle("Add New User");
        setSize(350, 250);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel userLabel = new JLabel("Username:");
        JLabel passLabel = new JLabel("Password:");
        JLabel roleLabel = new JLabel("Role:");

        usernameField = new JTextField(15);
        passwordField = new JPasswordField(15);
        roleCombo = new JComboBox<>(new String[]{"admin", "staff"});

        JButton addButton = new JButton("Add User");

       
        gbc.gridx = 0; gbc.gridy = 0;
        add(userLabel, gbc);
        gbc.gridx = 1;
        add(usernameField, gbc);

      
        gbc.gridx = 0; gbc.gridy = 1;
        add(passLabel, gbc);
        gbc.gridx = 1;
        add(passwordField, gbc);

       
        gbc.gridx = 0; gbc.gridy = 2;
        add(roleLabel, gbc);
        gbc.gridx = 1;
        add(roleCombo, gbc);

        
        gbc.gridx = 1; gbc.gridy = 3;
        gbc.anchor = GridBagConstraints.EAST;
        add(addButton, gbc);

        addButton.addActionListener(e -> addUser());

        setVisible(true);
    }

    private void addUser() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String role = (String) roleCombo.getSelectedItem();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "❗ Username and password cannot be empty.");
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, username);
            pst.setString(2, password); 
            pst.setString(3, role);

            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "✅ User added successfully!");

            
            usernameField.setText("");
            passwordField.setText("");
            roleCombo.setSelectedIndex(0);

        } catch (SQLIntegrityConstraintViolationException e) {
            JOptionPane.showMessageDialog(this, "❌ Username already exists.");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "❌ Error: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AddUserForm::new);
    }
}
