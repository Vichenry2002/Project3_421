package options;
import java.sql.*;
import java.util.Scanner;

public class Guest {
    public static void addGuest(Scanner scanner, Connection conn) {
        System.out.println("\n--- Add New Guest ---");

        System.out.print("Enter guest name: ");
        String guestName = scanner.nextLine();
        System.out.print("Enter guest address: ");
        String guestAddress = scanner.nextLine();

        int reservationId = -1;
        int roomNumber = -1;
        boolean validRoomFound = false;
        // Loop until a valid room is found for a given reservationId
        while (!validRoomFound) {
            System.out.print("Enter reservation ID: ");
            try {
                reservationId = Integer.parseInt(scanner.nextLine()); 

                String findRoomSql = "SELECT roomNumber FROM books WHERE reservationId = ?";
                try (PreparedStatement pstmtFindRoom = conn.prepareStatement(findRoomSql)) {
                    pstmtFindRoom.setInt(1, reservationId);
                    try (ResultSet rs = pstmtFindRoom.executeQuery()) {
                        if (rs.next()) {
                            roomNumber = rs.getInt("roomNumber");
                            System.out.println("Room number " + roomNumber + " found for reservation ID " + reservationId);
                            validRoomFound = true;
                        } else {
                            System.out.println("No room found for the given reservation ID. Please try again.");
                        }
                    }
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid format. Please enter a valid reservation ID.");
            } catch (SQLException e) {
                System.out.println("SQL Error when finding room number: " + e.getMessage());
                e.printStackTrace();
                return; 
            }
        }

        // SQL command to insert the new guest
        String sql = "INSERT INTO guests (guestName, guestAddress, roomNumber, reservationId) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Setting the parameters for the prepared statement
            pstmt.setString(1, guestName);
            pstmt.setString(2, guestAddress);
            pstmt.setInt(3, roomNumber);
            pstmt.setInt(4, reservationId);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Guest added successfully.");
            } else {
                System.out.println("Failed to add the guest.");
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}