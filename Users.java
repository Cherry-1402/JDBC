package bankingsystem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Users {
    private final Connection conn;

    public Users(Connection conn) {
        this.conn = conn;
    }

    public boolean userExists(String email) throws SQLException {
        String sql = "SELECT 1 FROM user WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean register(String fullName, String email, String password) {
        try {
            if (userExists(email)) {
                System.out.println("User already exists with this email.");
                return false;
            }

            String sql = "INSERT INTO user (full_name, email, password) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, fullName);
                ps.setString(2, email);
                ps.setString(3, password);
                int rows = ps.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.out.println("Error registering user: " + e.getMessage());
            return false;
        }
    }

    public String getFullName(String email) {
        String sql = "SELECT full_name FROM user WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("full_name");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching full name: " + e.getMessage());
        }
        return null;
    }

    public boolean login(String email, String password) {
        String sql = "SELECT password FROM user WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String stored = rs.getString("password");
                    return stored != null && stored.equals(password);
                }
            }
        } catch (SQLException e) {
            System.out.println("Error during login: " + e.getMessage());
        }
        return false;
    }

    public void viewAccountDetails(String email) {
        String sql = "SELECT * FROM accounts WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Account Number: " + rs.getLong("account_number"));
                    System.out.println("Full Name: " + rs.getString("full_name"));
                    System.out.println("Email: " + rs.getString("email"));
                    System.out.println("Balance: " + rs.getBigDecimal("balance"));
                } else {
                    System.out.println("No account found for this email.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching account details: " + e.getMessage());
        }
    }
}
