package bankingsystem;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class BankingApp {
    // Update these constants to match your MySQL setup
    private static final String URL = "jdbc:mysql://localhost:3306/college";
    private static final String USER = "root";
    private static final String PASS = "root";

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS);
                Scanner sc = new Scanner(System.in)) {

            Users users = new Users(conn);
            Accounts accounts = new Accounts(conn);
            AccountManager manager = new AccountManager(conn);

            String currentUser = null;

            while (true) {
                System.out.println("\n=== BANKING APP ===");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Open Account");
                System.out.println("4. Credit Money");
                System.out.println("5. Debit Money");
                System.out.println("6. Transfer Money");
                System.out.println("7. Check Balance");
                System.out.println("8. View Account Details");
                System.out.println("9. Exit");
                System.out.print("Enter choice: ");

                int choice;
                try {
                    choice = Integer.parseInt(sc.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid choice.");
                    continue;
                }

                try {
                    switch (choice) {
                        case 1: // Register
                            System.out.print("Full name: ");
                            String name = sc.nextLine().trim();
                            System.out.print("Email: ");
                            String email = sc.nextLine().trim();
                            System.out.print("Password: ");
                            String pwd = sc.nextLine().trim();
                            boolean reg = users.register(name, email, pwd);
                            System.out.println(reg ? "Registered successfully." : "Registration failed.");
                            break;
                        case 2: // Login
                            System.out.print("Email: ");
                            String le = sc.nextLine().trim();
                            System.out.print("Password: ");
                            String lp = sc.nextLine().trim();
                            if (users.login(le, lp)) {
                                currentUser = le;
                                System.out.println("Login successful.");
                            } else {
                                System.out.println("Login failed.");
                            }
                            break;
                        case 3: // Open Account
                            if (currentUser == null) {
                                System.out.println("Please login first.");
                                break;
                            }
                            if (accounts.accountExistsByEmail(currentUser)) {
                                System.out.println("An account already exists for this user.");
                                break;
                            }
                            System.out.print("Enter initial deposit (e.g., 100.00): ");
                            BigDecimal init = new BigDecimal(sc.nextLine().trim());
                            String pin;
                            while (true) {
                                System.out.print("Set 4-digit security PIN: ");
                                pin = sc.nextLine().trim();
                                if (pin.matches("\\d{4}"))
                                    break;
                                System.out.println("PIN must be exactly 4 digits.");
                            }
                            long accNo = accounts.generateAccountNumber();
                            String fullName = users.getFullName(currentUser);
                            boolean opened = accounts.openAccount(accNo, fullName, currentUser, init, pin);
                            System.out.println(opened ? ("Account opened. Account Number: " + accNo)
                                    : "Failed to open account.");
                            break;
                        case 4: // Credit
                            System.out.print("Enter account number: ");
                            long cAcc = Long.parseLong(sc.nextLine().trim());
                            System.out.print("Enter amount: ");
                            BigDecimal camt = new BigDecimal(sc.nextLine().trim());
                            System.out.print("Enter security PIN: ");
                            String cpin = sc.nextLine().trim();
                            if (manager.creditMoney(cAcc, camt, cpin)) {
                                System.out.println("Amount credited successfully.");
                            } else {
                                System.out.println("Credit failed.");
                            }
                            break;
                        case 5: // Debit
                            System.out.print("Enter account number: ");
                            long dAcc = Long.parseLong(sc.nextLine().trim());
                            System.out.print("Enter amount: ");
                            BigDecimal damt = new BigDecimal(sc.nextLine().trim());
                            System.out.print("Enter security PIN: ");
                            String dpin = sc.nextLine().trim();
                            if (manager.debitMoney(dAcc, damt, dpin)) {
                                System.out.println("Amount debited successfully.");
                            } else {
                                System.out.println("Debit failed.");
                            }
                            break;
                        case 6: // Transfer
                            System.out.print("Sender account number: ");
                            long sAcc = Long.parseLong(sc.nextLine().trim());
                            System.out.print("Receiver account number: ");
                            long rAcc = Long.parseLong(sc.nextLine().trim());
                            System.out.print("Amount: ");
                            BigDecimal tAmt = new BigDecimal(sc.nextLine().trim());
                            System.out.print("Sender security PIN: ");
                            String tpin = sc.nextLine().trim();
                            if (manager.transferMoney(sAcc, rAcc, tAmt, tpin)) {
                                System.out.println("Transfer successful.");
                            } else {
                                System.out.println("Transfer failed.");
                            }
                            break;
                        case 7: // Check Balance
                            System.out.print("Enter account number: ");
                            long bAcc = Long.parseLong(sc.nextLine().trim());
                            System.out.print("Enter security PIN: ");
                            String bpin = sc.nextLine().trim();
                            BigDecimal bal = manager.checkBalance(bAcc, bpin);
                            if (bal != null) {
                                System.out.println("Available balance: " + bal);
                            } else {
                                System.out.println("Could not fetch balance.");
                            }
                            break;
                        case 8: // View Account Details
                            if (currentUser == null) {
                                System.out.println("Please login first.");
                                break;
                            }
                            users.viewAccountDetails(currentUser);
                            break;
                        case 9:
                            System.out.println("Exiting...");
                            return;
                        default:
                            System.out.println("Unknown option.");
                    }
                } catch (SQLException ex) {
                    System.out.println("Database error: " + ex.getMessage());
                } catch (NumberFormatException nf) {
                    System.out.println("Invalid numeric input.");
                }
            }

        } catch (SQLException e) {
            System.out.println("Unable to connect to database: " + e.getMessage());
        }
    }
}
