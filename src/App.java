import java.sql.*;
import java.util.Scanner;
import options.Billing;

public class App {

    static Connection connection;

    private static Connection connect() {
        try { DriverManager.registerDriver ( new com.ibm.db2.jcc.DB2Driver() ) ; }
        catch (Exception cnfe){ System.out.println("Class not found"); }

        // This is the url you must use for DB2.
        // hello
        //Note: This url may not valid now ! Check for the correct year and semester and server name.
        String url = "jdbc:db2://winter2024-comp421.cs.mcgill.ca:50000/comp421";

        //REMEMBER to remove your user id and password before submitting your code!!
        String your_userid = "cs421g52";
        String your_password = "Team52#421";

        try {
            Connection connection = DriverManager.getConnection(url, your_userid, your_password);
            return connection;
        } catch (SQLException e) {
            System.out.println("Error connecting to the database");
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        
        Connection conn = connect();
        if (conn == null) {
            System.out.println("Exiting program due to database connection failure.");
            return;
        }
        connection = conn;
        invokeMenu();

        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            System.out.println("Error closing the connection");
            e.printStackTrace();
        }

    }

    public static void invokeMenu(){
        Scanner scanner = new Scanner(System.in);
        int choice;
        do {
            System.out.println("\nMain Menu:");
            System.out.println("1. Check Room Availability");
            System.out.println("2. Check Guest Billings");
            System.out.println("3. Add A New Guest");
            System.out.println("4. Create A New Reservation");
            System.out.println("5. Create A New Event");
            System.out.println("6. Quit");
            System.out.print("Enter choice: ");
    
            choice = scanner.nextInt();
            scanner.nextLine();
    
            switch (choice) {
                case 1:
                    // Call method to check room availability
                    break;
                case 2:
                    Billing.listBillings(scanner, connection);
                    break;
                case 3:
                    // Call method to add a new guest
                    break;
                case 4:
                    // Call method to create a new reservation
                    break;
                case 5:
                    // Call method to create a new event
                    break;
                case 6:
                    System.out.println("Exiting the program...");
                    break;
                default:
                    System.out.println("Invalid choice. Please select a number from 1 to 6.");
                    break;
            }
        } while (choice != 6);
    
        // Close the scanner after the loop, not inside it.
        scanner.close();
    }
}
