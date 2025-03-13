import java.sql.*;
import java.util.Scanner;
import java.time.LocalDate;

public class LibraryManagementSystem {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USERNAME = "ARPITA_2201020010";
    private static final String PASSWORD = "mahtae";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Connected to Oracle database successfully.");
            createSequence(conn);
            createTables(conn);
            insertInitialData(conn);
            displayMenu(conn, scanner);

        } catch (SQLException se) {
            System.err.println("Error connecting to Oracle database or executing query.");
            se.printStackTrace();
        }
    }

    static void createSequence(Connection conn) {
        String checkSequence = "SELECT COUNT(*) FROM user_sequences WHERE sequence_name = 'TRANSACTIONS_SEQ'";
        String createSequence = "CREATE SEQUENCE TRANSACTIONS_SEQ START WITH 1 INCREMENT BY 1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkSequence)) {
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.executeUpdate(createSequence);
                System.out.println("Sequence 'TRANSACTIONS_SEQ' created successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void createTables(Connection conn) {
        String createBooksTable = "CREATE TABLE Books1 (" +
                "book_id NUMBER(10) PRIMARY KEY, " +
                "title VARCHAR2(255), " +
                "author VARCHAR2(255), " +
                "genre VARCHAR2(50), " +
                "quantity_available NUMBER(5))";
        executeStatement(conn, createBooksTable);

        String createMembersTable = "CREATE TABLE Members1 (" +
                "member_id NUMBER(10) PRIMARY KEY, " +
                "name VARCHAR2(100), " +
                "email VARCHAR2(100), " +
                "address VARCHAR2(255))";
        executeStatement(conn, createMembersTable);

        String createTransactionsTable = "CREATE TABLE Transactions1 (" +
                "transaction_id NUMBER(10) PRIMARY KEY, " +
                "book_id NUMBER(10), " +
                "member_id NUMBER(10), " +
                "transaction_date DATE, " +
                "return_date DATE, " +
                "FOREIGN KEY (book_id) REFERENCES Books1(book_id), " +
                "FOREIGN KEY (member_id) REFERENCES Members1(member_id))";
        executeStatement(conn, createTransactionsTable);
    }

    static void insertInitialData(Connection conn) {
        executeStatement(conn, "INSERT INTO Books1 VALUES (1, 'Atomic Habits', 'James Clear', 'Self-help', 5)");
        executeStatement(conn, "INSERT INTO Books1 VALUES (2, 'Life of Pi', 'Yann Martel', 'Adventure', 3)");
        executeStatement(conn, "INSERT INTO Books1 VALUES (3, 'Pride and Prejudice', 'Jane Austen', 'Romance', 7)");
        executeStatement(conn, "INSERT INTO Books1 VALUES (4, 'Verity', 'Colleen Hoover', 'Thriller', 8)");

        executeStatement(conn, "INSERT INTO Members1 VALUES (1, 'Smita', 'smita@gmail.com', 'Mumbai')");
        executeStatement(conn, "INSERT INTO Members1 VALUES (2, 'Aryan Raj', 'aryan@gmail.com', 'Delhi')");
        executeStatement(conn, "INSERT INTO Members1 VALUES (3, 'Ananya', 'ananya@gmail.com', 'Pune')");
    }

    static void displayMenu(Connection conn, Scanner scanner) {
        while (true) {
            System.out.println("\nLibrary Management System Menu:");
            System.out.println("1. Display Books");
            System.out.println("2. Display Members");
            System.out.println("3. Borrow Book");
            System.out.println("4. Return Book");
            System.out.println("5. Display Transactions");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();

            switch (choice) {
                case 1:
                    displayBooks(conn);
                    break;
                case 2:
                    displayMembers(conn);
                    break;
                case 3:
                    borrowBook(conn, scanner);
                    break;
                case 4:
                    returnBook(conn, scanner);
                    break;
                case 5:
                    displayTransactions(conn);
                    break;
                case 6:
                    System.out.println("Exiting program...");
                    return;
                default:
                    System.out.println("Invalid choice! Please enter a number between 1 and 6.");
            }
        }
    }

    static void displayBooks(Connection conn) {
        String query = "SELECT * FROM Books1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            System.out.println("\nBooks Available:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("book_id") +
                        ", Title: " + rs.getString("title") +
                        ", Author: " + rs.getString("author") +
                        ", Genre: " + rs.getString("genre") +
                        ", Quantity: " + rs.getInt("quantity_available"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void displayMembers(Connection conn) {
        String query = "SELECT * FROM Members1";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            System.out.println("\nLibrary Members:");
            while (rs.next()) {
                System.out.println("ID: " + rs.getInt("member_id") +
                        ", Name: " + rs.getString("name") +
                        ", Email: " + rs.getString("email") +
                        ", Address: " + rs.getString("address"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void borrowBook(Connection conn, Scanner scanner) {
        System.out.print("Enter Member ID: ");
        int memberId = scanner.nextInt();
        System.out.print("Enter Book ID: ");
        int bookId = scanner.nextInt();

        String updateBooks = "UPDATE Books1 SET quantity_available = quantity_available - 1 WHERE book_id = ? AND quantity_available > 0";
        String insertTransaction = "INSERT INTO Transactions1 (transaction_id, book_id, member_id, transaction_date) VALUES (TRANSACTIONS_SEQ.NEXTVAL, ?, ?, SYSDATE)";

        try (PreparedStatement pstmt1 = conn.prepareStatement(updateBooks);
             PreparedStatement pstmt2 = conn.prepareStatement(insertTransaction)) {

            pstmt1.setInt(1, bookId);
            if (pstmt1.executeUpdate() > 0) {
                pstmt2.setInt(1, bookId);
                pstmt2.setInt(2, memberId);
                pstmt2.executeUpdate();
                System.out.println("Book borrowed successfully.");
            } else {
                System.out.println("Book not available.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void returnBook(Connection conn, Scanner scanner) {
        System.out.print("Enter Member ID: ");
        int memberId = scanner.nextInt();
        System.out.print("Enter Book ID: ");
        int bookId = scanner.nextInt();

        String updateTransaction = "UPDATE Transactions1 SET return_date = ? WHERE book_id = ? AND member_id = ? AND return_date IS NULL";
        String updateBook = "UPDATE Books1 SET quantity_available = quantity_available + 1 WHERE book_id = ?";

        try (PreparedStatement pstmt1 = conn.prepareStatement(updateTransaction);
             PreparedStatement pstmt2 = conn.prepareStatement(updateBook)) {
            pstmt1.setDate(1, Date.valueOf(LocalDate.now()));
            pstmt1.setInt(2, bookId);
            pstmt1.setInt(3, memberId);
            int rowsAffected = pstmt1.executeUpdate();

            if (rowsAffected > 0) {
                pstmt2.setInt(1, bookId);
                pstmt2.executeUpdate();
                System.out.println("Book returned successfully.");
            } else {
                System.out.println("No matching active transaction found.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

     static void displayTransactions(Connection conn) {
        String query = "SELECT t.transaction_id, b.title, m.name, t.transaction_date, NVL(TO_CHAR(t.return_date), 'Not Returned') FROM Transactions1 t JOIN Books1 b ON t.book_id = b.book_id JOIN Members1 m ON t.member_id = m.member_id";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            System.out.println("\nTransactions:");
            while (rs.next()) {
                System.out.println("Transaction ID: " + rs.getInt(1) + ", Book: " + rs.getString(2) + ", Member: " + rs.getString(3) + ", Transaction Date: " + rs.getDate(4) + ", Return Date: " + rs.getString(5));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void executeStatement(Connection conn, String query) {
    try (Statement stmt = conn.createStatement()) {
        stmt.executeUpdate(query);
    } catch (SQLException e) {
        // Ignore "table already exists" errors, print others
        if (!e.getMessage().contains("already exists")) {
            e.printStackTrace();
        }
    }
}

}
