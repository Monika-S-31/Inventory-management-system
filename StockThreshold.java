package Stock;

import java.sql.*;

import DB.DBConnection;

public class StockThreshold {
    public static void main(String[] args) {
        if (checkAndRepairStockThreshold()) {
            System.out.println("Database is ready with stock_threshold column");
        } else {
            System.err.println("Failed to verify/repair stock_threshold column");
        }
    }

    public static boolean checkAndRepairStockThreshold() {
        System.out.println("Checking database structure...");

        try (Connection conn = DBConnection.getConnection()) {
            // First check if table exists
            if (!tableExists(conn, "products")) {
                throw new SQLException("Products table doesn't exist");
            }

            // Check if column exists
            if (!columnExists(conn, "products", "stock_threshold")) {
                System.out.println("Column stock_threshold missing - adding it now");
                addStockThresholdColumn(conn);
            } else {
                System.out.println("Column stock_threshold already exists");
            }

            // Verify the column was properly added
            if (!verifyColumnStructure(conn)) {
                throw new SQLException("Column verification failed");
            }

            return true;

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static void addStockThresholdColumn(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE products ADD COLUMN stock_threshold INT DEFAULT 10");
            System.out.println("Added stock_threshold column with default value 10");
        }
    }

    private static boolean verifyColumnStructure(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE products")) {
             
            while (rs.next()) {
                if (rs.getString("Field").equalsIgnoreCase("stock_threshold")) {
                    String type = rs.getString("Type").toUpperCase();
                    if (type.contains("INT")) {
                        System.out.println("Verified stock_threshold column: " + rs.getString("Type"));
                        return true;
                    }
                }
            }
        }
        return false;
    }
}

