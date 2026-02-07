package bankingsystem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class Accounts {
    private final Connection conn;
    private final Random rnd = new Random();

    public Accounts(Connection conn) {
        this.conn = conn;
    }

    public static class Account {
        public long accountNumber;
        public String fullName;
        public String email;
        public BigDecimal balance;
        public String securityPin;
    }

    public long generateAccountNumber() throws SQLException {
        long acc;
        do {
            acc = 1_000_000_000L + (long) (rnd.nextDouble() * 8_999_999_999L);
        } while (accountNumberExists(acc));
        return acc;
    }

    private boolean accountNumberExists(long accNo) throws SQLException {
        String sql = "SELECT 1 FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean accountExistsByEmail(String email) throws SQLException {
        String sql = "SELECT 1 FROM accounts WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public boolean openAccount(long accountNumber, String fullName, String email, BigDecimal balance, String pin)
            throws SQLException {
        String sql = "INSERT INTO accounts (account_number, full_name, email, balance, security_pin) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accountNumber);
            ps.setString(2, fullName);
            ps.setString(3, email);
            ps.setBigDecimal(4, balance);
            ps.setString(5, pin);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    public Account getAccountByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Account a = new Account();
                    a.accountNumber = rs.getLong("account_number");
                    a.fullName = rs.getString("full_name");
                    a.email = rs.getString("email");
                    a.balance = rs.getBigDecimal("balance");
                    a.securityPin = rs.getString("security_pin");
                    return a;
                }
            }
        }
        return null;
    }

    public Account getAccountByNumber(long accNo) throws SQLException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, accNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Account a = new Account();
                    a.accountNumber = rs.getLong("account_number");
                    a.fullName = rs.getString("full_name");
                    a.email = rs.getString("email");
                    a.balance = rs.getBigDecimal("balance");
                    a.securityPin = rs.getString("security_pin");
                    return a;
                }
            }
        }
        return null;
    }

}
