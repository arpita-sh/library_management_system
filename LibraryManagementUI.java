import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import java.sql.*;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundSize;


public class LibraryManagementUI extends Application {
    private static final String URL = "jdbc:oracle:thin:@localhost:1521:XE";
    private static final String USERNAME = "ARPITA_2201020010";
    private static final String PASSWORD = "mahtae";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Library Management System");

        Button displayBooksBtn = new Button("Display Books");
        Button displayMembersBtn = new Button("Display Members");
        Button borrowBookBtn = new Button("Borrow Book");
        Button returnBookBtn = new Button("Return Book");
        Button displayTransactionsBtn = new Button("Display Transactions");

        displayBooksBtn.setOnAction(e -> displayBooks());
        displayMembersBtn.setOnAction(e -> displayMembers());
        borrowBookBtn.setOnAction(e -> borrowBook());
        returnBookBtn.setOnAction(e -> returnBook());
        displayTransactionsBtn.setOnAction(e -> displayTransactions());

        // Layout
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        grid.add(displayBooksBtn, 0, 0);
        grid.add(displayMembersBtn, 1, 0);
        grid.add(borrowBookBtn, 0, 1);
        grid.add(returnBookBtn, 1, 1);
        grid.add(displayTransactionsBtn, 0, 2, 2, 1);

      try {
            Image backgroundImage = new Image(getClass().getResource("/pic.jpg").toExternalForm());

            BackgroundImage bgImage = new BackgroundImage(
                    backgroundImage,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER,
                    new BackgroundSize(100, 100, true, true, true, true)
            );
            grid.setBackground(new Background(bgImage));
        } catch (Exception e) {
            System.out.println("Error loading background image: " + e.getMessage());
        }


        // Scene setup
        Scene scene = new Scene(grid, 400, 300);
        scene.getStylesheets().add(getClass().getResource("styles.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void displayBooks() {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Books1")) {

            StringBuilder booksList = new StringBuilder("Books Available:\n");
            while (rs.next()) {
                booksList.append("ID: ").append(rs.getInt("book_id"))
                        .append(", Title: ").append(rs.getString("title"))
                        .append(", Author: ").append(rs.getString("author"))
                        .append(", Genre: ").append(rs.getString("genre"))
                        .append(", Quantity: ").append(rs.getInt("quantity_available"))
                        .append("\n");
            }
            showAlert("Books List", booksList.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void displayMembers() {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Members1")) {

            StringBuilder membersList = new StringBuilder("Library Members:\n");
            while (rs.next()) {
                membersList.append("ID: ").append(rs.getInt("member_id"))
                        .append(", Name: ").append(rs.getString("name"))
                        .append(", Email: ").append(rs.getString("email"))
                        .append(", Address: ").append(rs.getString("address"))
                        .append("\n");
            }
            showAlert("Members List", membersList.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void borrowBook() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Enter Member ID and Book ID (comma-separated)");
        dialog.showAndWait().ifPresent(input -> {
            String[] parts = input.split(",");
            if (parts.length == 2) {
                int memberId = Integer.parseInt(parts[0].trim());
                int bookId = Integer.parseInt(parts[1].trim());

                String updateBooks = "UPDATE Books1 SET quantity_available = quantity_available - 1 WHERE book_id = ? AND quantity_available > 0";
                String insertTransaction = "INSERT INTO Transactions1 (transaction_id, book_id, member_id, transaction_date) VALUES (TRANSACTIONS_SEQ.NEXTVAL, ?, ?, SYSDATE)";

                try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                     PreparedStatement pstmt1 = conn.prepareStatement(updateBooks);
                     PreparedStatement pstmt2 = conn.prepareStatement(insertTransaction)) {

                    pstmt1.setInt(1, bookId);
                    if (pstmt1.executeUpdate() > 0) {
                        pstmt2.setInt(1, bookId);
                        pstmt2.setInt(2, memberId);
                        pstmt2.executeUpdate();
                        showAlert("Success", "Book borrowed successfully.");
                    } else {
                        showAlert("Error", "Book not available.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void returnBook() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setHeaderText("Enter Member ID and Book ID (comma-separated)");
        dialog.showAndWait().ifPresent(input -> {
            String[] parts = input.split(",");
            if (parts.length == 2) {
                int memberId = Integer.parseInt(parts[0].trim());
                int bookId = Integer.parseInt(parts[1].trim());

                String updateTransaction = "UPDATE Transactions1 SET return_date = SYSDATE WHERE book_id = ? AND member_id = ? AND return_date IS NULL";
                String updateBook = "UPDATE Books1 SET quantity_available = quantity_available + 1 WHERE book_id = ?";

                try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
                     PreparedStatement pstmt1 = conn.prepareStatement(updateTransaction);
                     PreparedStatement pstmt2 = conn.prepareStatement(updateBook)) {

                    pstmt1.setInt(1, bookId);
                    pstmt1.setInt(2, memberId);
                    int rowsAffected = pstmt1.executeUpdate();

                    if (rowsAffected > 0) {
                        pstmt2.setInt(1, bookId);
                        pstmt2.executeUpdate();
                        showAlert("Success", "Book returned successfully.");
                    } else {
                        showAlert("Error", "No matching active transaction found.");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void displayTransactions() {
        try (Connection conn = DriverManager.getConnection(URL, USERNAME, PASSWORD);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM Transactions1")) {

            StringBuilder transactionsList = new StringBuilder("Library Transactions:\n");
            while (rs.next()) {
                transactionsList.append("Transaction ID: ").append(rs.getInt("transaction_id"))
                        .append(", Book ID: ").append(rs.getInt("book_id"))
                        .append(", Member ID: ").append(rs.getInt("member_id"))
                        .append(", Transaction Date: ").append(rs.getDate("transaction_date"))
                        .append(", Return Date: ").append(rs.getDate("return_date") == null ? "Not Returned" : rs.getDate("return_date"))
                        .append("\n");
            }
            showAlert("Transactions List", transactionsList.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
