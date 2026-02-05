import java.sql.*;
import java.util.Scanner;

public class TransactionFixed {
    // Database credentials
    static final String URL = "jdbc:mysql://localhost:3306/college";
    static final String USER = "root";
    static final String PASS = "root";

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n=== BANKING SYSTEM ===");
            System.out.println("1. Create New Account");
            System.out.println("2. Transfer Money");
            System.out.println("3. View Account Details");
            System.out.println("4. Delete Account");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            int choice = sc.nextInt();

            if (choice == 1) {
                createAccount(sc);
            } else if (choice == 2) {
                performTransfer(sc);
            } else if (choice == 3) {
                viewAccount(sc);
            } else if (choice == 4) {
                deleteAccount(sc);
            } else {
                System.out.println("Exiting...");
                break;
            }
        }
        sc.close();
    }

    private static void createAccount(Scanner sc) {
        System.out.println("\n--- Create Account ---");
        System.out.print("Enter Account Number (ID): ");
        int accNo = sc.nextInt();
        sc.nextLine(); // consume newline
        System.out.print("Enter Holder Name: ");
        String name = sc.nextLine();
        
        System.out.print("Enter Initial Balance: ");
        double balance = sc.nextDouble();

        String insertSQL = "INSERT INTO accounts (acc_no, name, balance) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(insertSQL)) {

            pstmt.setInt(1, accNo);
            pstmt.setString(2, name);
            // pstmt.setString(3, type);
            pstmt.setDouble(3, balance);

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Account created successfully!");
            }
        } catch (SQLException e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    private static void performTransfer(Scanner sc) {
        System.out.println("\n--- Transfer Money ---");
        System.out.print("Enter Sender Account No: ");
        int senderAcc = sc.nextInt();
        System.out.print("Enter Receiver Account No: ");
        int receiverAcc = sc.nextInt();
        System.out.print("Enter Amount: ");
        double amount = sc.nextDouble();

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            conn.setAutoCommit(false); // Start Transaction

            // 1. Validate Sender
            if (!checkAccountExists(conn, senderAcc)) {
                System.out.println("Transaction Failed: Sender account " + senderAcc + " does not exist.");
                conn.rollback();
                return;
            }

            // 2. Validate Receiver
            if (!checkAccountExists(conn, receiverAcc)) {
                System.out.println("Transaction Failed: Receiver account " + receiverAcc + " does not exist.");
                conn.rollback();
                return;
            }

            // 3. Check Balance
            double currentBalance = getBalance(conn, senderAcc);
            if (currentBalance < amount) {
                System.out.println("Transaction Failed: Insufficient balance. Available: " + currentBalance);
                conn.rollback();
                return;
            }

            // 4. Perform Transfer
            String withdrawSQL = "UPDATE accounts SET balance = balance - ? WHERE acc_no = ?";
            String depositSQL = "UPDATE accounts SET balance = balance + ? WHERE acc_no = ?";

            try (PreparedStatement withdrawStmt = conn.prepareStatement(withdrawSQL);
                    PreparedStatement depositStmt = conn.prepareStatement(depositSQL)) {

                // Withdraw
                withdrawStmt.setDouble(1, amount);
                withdrawStmt.setInt(2, senderAcc);
                withdrawStmt.executeUpdate();

                // Deposit
                depositStmt.setDouble(1, amount);
                depositStmt.setInt(2, receiverAcc);
                depositStmt.executeUpdate();

                conn.commit(); // COMMIT TRANSACTION
                System.out.println(
                        "Transaction Successful! Transferred " + amount + " from " + senderAcc + " to " + receiverAcc);

            } catch (SQLException e) {
                conn.rollback();
                System.out.println("SQL Error during transfer: " + e.getMessage());
                e.printStackTrace();
            }

        } catch (SQLException e) {
            System.out.println("Database Connection Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (conn != null)
                    conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private static void viewAccount(Scanner sc) {
        System.out.println("\n--- View Account Details ---");
        System.out.print("Enter Account Number (ID): ");
        int accNo = sc.nextInt();

        String query = "SELECT * FROM accounts WHERE acc_no = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, accNo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Account ID: " + rs.getInt("acc_no"));
                    System.out.println("Holder Name: " + rs.getString("name"));
                    System.out.println("Balance: " + rs.getDouble("balance"));
                } else {
                    System.out.println("Account not found!");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error fetching account: " + e.getMessage());
        }
    }

    private static void deleteAccount(Scanner sc) {
        System.out.println("\n--- Delete Account ---");
        System.out.print("Enter Account Number (ID): ");
        int accNo = sc.nextInt();

        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            conn.setAutoCommit(false); // Start Transaction

            // Check if account exists
            if (!checkAccountExists(conn, accNo)) {
                System.out.println("Delete Failed: Account " + accNo + " does not exist.");
                conn.rollback();
                return;
            }

            String deleteSQL = "DELETE FROM accounts WHERE acc_no = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteSQL)) {
                pstmt.setInt(1, accNo);
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    conn.commit();
                    System.out.println("Account " + accNo + " deleted successfully.");
                } else {
                    conn.rollback();
                    System.out.println("Delete Failed: Unknown error.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Database Error: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // Helper to check if account exists
    private static boolean checkAccountExists(Connection conn, int accNo) throws SQLException {
        String query = "SELECT 1 FROM accounts WHERE acc_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, accNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // Helper to get balance
    private static double getBalance(Connection conn, int accNo) throws SQLException {
        String query = "SELECT balance FROM accounts WHERE acc_no = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, accNo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        return 0.0;
    }
} 
