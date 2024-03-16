package options;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Event {

    public static void createEvent(Scanner scanner, Connection conn) {
        System.out.println("\n--- Create New Event ---");
        
        System.out.println("Insert the number of the corresponding hotel you would like to host your event:");
        System.out.println("1. Cityville Central - 123 Main St, Cityville");
        System.out.println("2. Metropolis Grand - 456 Grand Ave, Metropolis");
        System.out.println("3. Lakeside Retreat - 789 River Rd, Lakeside");
        System.out.println("4. Greenfield Oasis - 1010 Forest Dr, Greenfield");
        System.out.println("5. Seaside Resort  - 1212 Coastal Way, Seaside");
        System.out.print("Enter choice number (enter 'b' to go back): ");
        String choice = scanner.nextLine();

        if(choice.equals("b")){
            return;
        }

        while(!(choice.equals("1") || choice.equals("2") || choice.equals("3") || choice.equals("4") || choice.equals("5"))){
            System.out.println();
            System.out.println("Invalid choice. Please select a number from 1 to 5. Or enter 'b' to go back");
            choice = scanner.nextLine();
            if(choice.equals("b")){
                return;
            }
        }
        int hotel_idx = Integer.parseInt(choice);
        HashMap<Integer, String> hotel_map = new HashMap<>();
        hotel_map.put(1, "123 Main St, Cityville");
        hotel_map.put(2, "456 Grand Ave, Metropolis");
        hotel_map.put(3, "789 River Rd, Lakeside");
        hotel_map.put(4, "1010 Forest Dr, Greenfield");
        hotel_map.put(5, "1212 Coastal Way, Seaside");
        String hotelAddress = hotel_map.get(hotel_idx);

        System.out.print("Enter event type: ");
        String eventType = scanner.nextLine();

        String eventDate;
        LocalDate checkDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Define a formatter that matches your date format
        String dateFormatRegex = "\\d{4}-\\d{2}-\\d{2}"; // Regular expression for YYYY-MM-DD

        // Validate date
        while (true) {
            System.out.println("Insert date of the event (YYYY-MM-DD):");
            eventDate = scanner.nextLine();
            if (eventDate.matches(dateFormatRegex)) {
                try {
                    checkDate = LocalDate.parse(eventDate, formatter);
                    break; // Break the loop if the format is correct and can be parsed
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                }
            } else {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        // Show available conference rooms
        HashMap<Integer, List<Integer>> availableRooms = Room.availableConfRooms(hotelAddress, eventDate, conn);
        if (availableRooms.isEmpty()) {
            System.out.println("No available conference rooms for the specified date. Please choose a different date or hotel.");
            return;
        }

        System.out.println("\nAvailable conference rooms for specified date and location:");
        for (Map.Entry<Integer, List<Integer>> entry : availableRooms.entrySet()) {
            System.out.println("Room Number: " + entry.getKey() + ", Capacity: " + entry.getValue().get(0) + ", Price per Hour: $" + entry.getValue().get(1));
        }

        // select a room number from the available options
        int roomNumber = -1;
        boolean validRoomSelection = false;

        while (!validRoomSelection) {
            System.out.print("Please select a room number from the above list: ");
            String roomNumberInput = scanner.nextLine();
            try {
                roomNumber = Integer.parseInt(roomNumberInput);
                if (availableRooms.containsKey(roomNumber)) {
                    validRoomSelection = true;
                } else {
                    System.out.println("Invalid room selection. Please select a valid room number from the list.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
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

        int numberOfParticipants = 0; 
        int roomCapacity = availableRooms.get(roomNumber).get(0); 
        System.out.print("Enter the number of participants for your event (max " + roomCapacity + "): ");
        while (true) {
            String input = scanner.nextLine();
            try {
                numberOfParticipants = Integer.parseInt(input);
                if (numberOfParticipants > 0 && numberOfParticipants <= roomCapacity) {
                    break; // Valid number of participants entered
                } else {
                    System.out.println("The number of participants must be greater than 0 and no more than " + roomCapacity + ".");
                }
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid integer.");
            }
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