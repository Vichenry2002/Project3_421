package options;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
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

        long eventId = -1;
        String sql = "INSERT INTO hotelEvents (eventType, eventDate, hotelAddress, roomNumber) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, eventType);
            pstmt.setDate(2, Date.valueOf(eventDate));
            pstmt.setString(3, hotelAddress);
            pstmt.setInt(4, roomNumber);
        
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        eventId = generatedKeys.getLong(1); // Retrieve the eventId
                        System.out.println("Event created successfully with Event ID: " + eventId);
                    } else {
                        throw new SQLException("Creating event failed, no ID obtained.");
                    }
                }
            } else {
                System.out.println("Failed to create the event.");
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.print("Enter the number of participants (max 60): ");
        int numberOfParticipants = Integer.parseInt(scanner.nextLine());
        if (numberOfParticipants > 60) {
            System.out.println("The number of participants cannot exceed 60.");
            return;
        }

        int numberStaffPerEvent = (int) Math.ceil(numberOfParticipants / 5.0);

        List<Integer> availableStaffIds = new ArrayList<>();
        String findAvailableStaffSql = "SELECT s.staffId FROM staff s WHERE s.staffId NOT IN (SELECT o.staffId FROM organizes o JOIN hotelEvents e ON o.eventId = e.eventId WHERE e.eventDate = ?)";

        try (PreparedStatement pstmtAvailableStaff = conn.prepareStatement(findAvailableStaffSql)) {
            pstmtAvailableStaff.setDate(1, Date.valueOf(eventDate));
            try (ResultSet rsAvailableStaff = pstmtAvailableStaff.executeQuery()) {
                while (rsAvailableStaff.next()) {
                    availableStaffIds.add(rsAvailableStaff.getInt("staffId"));
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Error when finding available staff: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        Collections.shuffle(availableStaffIds); 
        String assignStaffSql = "INSERT INTO organizes (eventId, staffId) VALUES (?, ?)";
        try (PreparedStatement pstmtAssignStaff = conn.prepareStatement(assignStaffSql)) {
            for (int i = 0; i < numberStaffPerEvent && i < availableStaffIds.size(); i++) {
                pstmtAssignStaff.setLong(1, eventId);
                pstmtAssignStaff.setInt(2, availableStaffIds.get(i));
                pstmtAssignStaff.executeUpdate();
            }
            System.out.println("Staff assigned successfully to the event.");
        } catch (SQLException e) {
            System.out.println("SQL Error when assigning staff to the event: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }
}