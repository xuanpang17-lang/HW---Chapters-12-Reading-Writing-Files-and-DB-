import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class Main {

    // Database connection details (using SQLite for portable local demo)
    private static final String DB_URL = "jdbc:sqlite:test_database.db";

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("========== PART 1: FILE I/O OPERATIONS ==========");
        handleFileOperations(scanner);

        System.out.println("\n========== PART 2: DATABASE OPERATIONS ==========");
        handleDatabaseOperations();

        scanner.close();
    }

    /**
     * Part 1: File Operations
     * 1. Read "my_test_file.txt" and print its content.
     * 2. Take terminal input and overwrite "my_test_file.txt".
     */
    private static void handleFileOperations(Scanner scanner) {
        String fileName = "my_test_file.txt";
        File file = new File(fileName);

        // Step 1 & 2: Check and read my_test_file.txt
        System.out.println("[Step 1] Reading content from " + fileName + "...");
        if (!file.exists()) {
            System.out.println("Warning: " + fileName + " not found! " +
                    "Please make sure you created it with Notepad containing: \"This is my Java read test\"");
            System.out.println("Creating a dummy " + fileName + " for demonstration...");
            createInitialFile(fileName, "This is my Java read test");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            System.out.println("--- Content read from file ---");
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("------------------------------");
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }

        // Step 3: Enter phrase from terminal
        System.out.println("\n[Step 2] Enter phrase to write into file (e.g., \"Java write test\"):");
        System.out.print("> ");
        String userInput = scanner.nextLine();

        // Step 4: Save this phrase into the same file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(userInput);
            System.out.println("[Step 3] Successfully written to " + fileName);
            System.out.println("-> You can now open \"" + fileName + "\" with Notepad to verify the update.");
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    /**
     * Helper method to create initial file if missing
     */
    private static void createInitialFile(String fileName, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            writer.write(content);
        } catch (IOException e) {
            System.err.println("Could not create initial file: " + e.getMessage());
        }
    }

    /**
     * Part 2: Database Operations with JDBC
     * Demonstrates read, modify, update, rollback, and commit actions.
     */
    private static void handleDatabaseOperations() {
        // Setup initial database table & demo data
        setupDatabase();

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            System.out.println("\nConnected to database successfully.");

            // -------------------------------------------------------------
            // SECTION A: Read -> Modify -> Update -> Rollback (Restore)
            // -------------------------------------------------------------
            System.out.println("\n--- Section A: Transaction with Rollback ---");

            // Turn off auto-commit to enable manual transaction management and rollback
            conn.setAutoCommit(false);

            // 1. Read record (ID = 1)
            String originalName = readRecordName(conn, 1);
            System.out.println("Original Record (ID=1) Name: " + originalName);

            // 2. Modify record in Java program
            String modifiedNameTemp = originalName + " [TEMP_MODIFIED]";
            System.out.println("Modified in Java program to: " + modifiedNameTemp);

            // 3. Update record in database
            updateRecordName(conn, 1, modifiedNameTemp);
            System.out.println("Updated record in database (Pending Commit).");
            System.out.println("Current Name in DB: " + readRecordName(conn, 1));

            // 4. Restore / Rollback the previous information
            System.out.println("Rolling back transaction to restore original data...");
            conn.rollback();
            System.out.println("Rollback complete.");

            // Verify restoration
            String restoredName = readRecordName(conn, 1);
            System.out.println("Restored Record (ID=1) Name: " + restoredName);

            // Reset auto-commit back to default
            conn.setAutoCommit(true);

            // -------------------------------------------------------------
            // SECTION B: Read -> Modify -> Update (Final Commit)
            // -------------------------------------------------------------
            System.out.println("\n--- Section B: Permanent Update ---");

            // 1. Read record (ID = 1)
            String currentName = readRecordName(conn, 1);
            System.out.println("Current Record (ID=1) Name: " + currentName);

            // 2. Modify record in Java
            String updatedNameFinal = "Java Database Updated Record";
            System.out.println("Modifying in Java program to: " + updatedNameFinal);

            // 3. Update database
            updateRecordName(conn, 1, updatedNameFinal);
            System.out.println("Updated database record permanently.");

            // Verify final update
            String finalName = readRecordName(conn, 1);
            System.out.println("Final Record (ID=1) Name in DB: " + finalName);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    /**
     * Reads record name for a given ID
     */
    private static String readRecordName(Connection conn, int id) throws SQLException {
        String query = "SELECT name FROM students WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        return null;
    }

    /**
     * Updates record name for a given ID
     */
    private static void updateRecordName(Connection conn, int id, String newName) throws SQLException {
        String updateSql = "UPDATE students SET name = ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setString(1, newName);
            pstmt.setInt(2, id);
            pstmt.executeUpdate();
        }
    }

    /**
     * Helper to set up test table and initial data
     */
    private static void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {

            // Create table
            String createTableSql = "CREATE TABLE IF NOT EXISTS students (" +
                    "id INTEGER PRIMARY KEY, " +
                    "name TEXT NOT NULL)";
            stmt.execute(createTableSql);

            // Insert initial record if not present
            String checkSql = "SELECT COUNT(*) FROM students WHERE id = 1";
            ResultSet rs = stmt.executeQuery(checkSql);
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO students (id, name) VALUES (1, 'John Doe')");
            }

        } catch (SQLException e) {
            System.err.println("Error setting up database: " + e.getMessage());
        }
    }
}