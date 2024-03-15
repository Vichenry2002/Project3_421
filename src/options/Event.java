package options;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Event {

    public static void createEvent(Scanner scanner, Connection conn) {
        System.out.println("\n--- Create New Event ---");

        System.out.print("Enter event type: ");
        String eventType = scanner.nextLine();
        System.out.print("Enter event date (YYYY-MM-DD): ");
        String eventDate = scanner.nextLine();
        String hotelAddress = Reservation.chooseHotel(scanner); 

        // Show available conference rooms
        HashMap<Integer, List<Integer>> availableRooms = Room.availableConfRooms(hotelAddress, eventDate, conn);
        if (availableRooms.isEmpty()) {
            System.out.println("No available conference rooms for the specified date. Please choose a different date or hotel.");
            return;
        }

        System.out.println("\nAvailable conference rooms:");
        for (Map.Entry<Integer, List<Integer>> entry : availableRooms.entrySet()) {
            System.out.println("Room Number: " + entry.getKey() + ", Capacity: " + entry.getValue().get(0) + ", Price per Hour: $" + entry.getValue().get(1));
        }

        // select a room number from the available options
        System.out.print("Select a room number from the above list: ");
        int roomNumber = Integer.parseInt(scanner.nextLine());
        if (!availableRooms.containsKey(roomNumber)) {
            System.out.println("Invalid room selection. Please try again.");
            return;
        }

        // Insert the event into the database
        String sql = "INSERT INTO hotelEvents (eventType, eventDate, hotelAddress, roomNumber) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, eventType);
            pstmt.setDate(2, Date.valueOf(eventDate));
            pstmt.setString(3, hotelAddress);
            pstmt.setInt(4, roomNumber);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Event created successfully.");
            } else {
                System.out.println("Failed to create the event.");
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}