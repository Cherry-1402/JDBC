package bankingsystem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AccountManager {
    private final Connection conn;
    private final Accounts accounts;

    public AccountManager(Connection conn) {
        this.conn = conn;
        this.accounts = new Accounts(conn);
    }

    private boolean validatePin(Accounts.Account acc, String pin) {
        return acc != null && acc.securityPin != null && acc.securityPin.equals(pin);
    }

    public boolean creditMoney(long accountNumber, BigDecimal amount, String pin) throws SQLException {
        Accounts.Account acc = accounts.getAccountByNumber(accountNumber);
        if (!validatePin(acc, pin)) {
            System.out.println("Invalid security PIN.");
            return false;
        }
        String sql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, accountNumber);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    public boolean debitMoney(long accountNumber, BigDecimal amount, String pin) throws SQLException {
        Accounts.Account acc = accounts.getAccountByNumber(accountNumber);
        if (!validatePin(acc, pin)) {
            System.out.println("Invalid security PIN.");
            return false;
        }
        if (acc.balance.compareTo(amount) < 0) {
            System.out.println("Insufficient funds. Available: " + acc.balance);
            return false;
        }
        String sql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setLong(2, accountNumber);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    public boolean transferMoney(long senderAcc, long receiverAcc, BigDecimal amount, String pin) throws SQLException {
        if (senderAcc == receiverAcc) {
            System.out.println("Cannot transfer money to the same account.");
            return false;
        }

        try {
            conn.setAutoCommit(false);

            Accounts.Account sender = accounts.getAccountByNumber(senderAcc);
            Accounts.Account receiver = accounts.getAccountByNumber(receiverAcc);

            if (sender == null) {
                System.out.println("Sender account not found.");
                conn.rollback();
                return false;
            }
            if (receiver == null) {
                System.out.println("Receiver account not found.");
                conn.rollback();
                return false;
            }
            if (!validatePin(sender, pin)) {
                System.out.println("Invalid security PIN for sender.");
                conn.rollback();
                return false;
            }
            if (sender.balance.compareTo(amount) < 0) {
                System.out.println("Insufficient funds in sender account.");
                conn.rollback();
                return false;
            }

            String withdrawSQL = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
            String depositSQL = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";

            try (PreparedStatement w = conn.prepareStatement(withdrawSQL);
                    PreparedStatement d = conn.prepareStatement(depositSQL)) {

                w.setBigDecimal(1, amount);
                w.setLong(2, senderAcc);
                int rowsAffectedSender = w.executeUpdate();

                d.setBigDecimal(1, amount);
                d.setLong(2, receiverAcc);
                int rowsAffectedReceiver = d.executeUpdate();

                if (rowsAffectedSender > 0 && rowsAffectedReceiver > 0) {
                    conn.commit();
                    return true;
                } else {
                    System.out.println("Transaction failed during update. Rolling back.");
                    conn.rollback();
                    return false;
                }
            } catch (SQLException ex) {
                conn.rollback();
                System.out.println("Error during transfer: " + ex.getMessage());
                return false;
            }
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public BigDecimal checkBalance(long accountNumber, String pin) throws SQLException {
        Accounts.Account acc = accounts.getAccountByNumber(accountNumber);
        if (!validatePin(acc, pin)) {
            System.out.println("Invalid security PIN.");
            return null;
        }
        return acc.balance;
    }

    // Convenience method: get account by email
    public Accounts.Account getAccountByEmail(String email) throws SQLException {
        return accounts.getAccountByEmail(email);
    }

}
