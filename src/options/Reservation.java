package options;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Reservation {
    public static void newReservation(Scanner scanner, Connection conn) {
        System.out.println("\n--- Add New Reservation ---");

        System.out.println("Insert the number of the corresponding hotel you would like to stay at:");
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

        String checkIn, checkOut;
        LocalDate checkInDate, checkOutDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // Define a formatter that matches your date format
        String dateFormatRegex = "\\d{4}-\\d{2}-\\d{2}"; // Regular expression for YYYY-MM-DD

        // Validate check-in date
        while (true) {
            System.out.println("Insert check-in date (YYYY-MM-DD):");
            checkIn = scanner.nextLine();
            if (checkIn.matches(dateFormatRegex)) {
                try {
                    checkInDate = LocalDate.parse(checkIn, formatter);
                    break; // Break the loop if the format is correct and can be parsed
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                }
            } else {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        // Validate check-out date
        while (true) {
            System.out.println("Insert check-out date (YYYY-MM-DD):");
            checkOut = scanner.nextLine();
            if (checkOut.matches(dateFormatRegex)) {
                try {
                    checkOutDate = LocalDate.parse(checkOut, formatter);
                    if (checkOutDate.isAfter(checkInDate)) {
                        break; // Break the loop if the format is correct and check-out is after check-in
                    } else {
                        System.out.println("Check-out date must be after check-in date. Please enter a valid check-out date.");
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("Invalid date format. Please use YYYY-MM-DD.");
                }
            } else {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }
        int lengthOfStay = calculateLengthOfStay(checkIn, checkOut);
        System.out.print("Enter payment information (*card* ending in *last 4 digits* or specify if paypal): ");
        String paymentInfo = scanner.nextLine();
        
        long reservationId = -1;
        // Inserting reservation into database
        String sql = "INSERT INTO reservations (arrivalDate, lengthOfStay, paymentInfo) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setDate(1, Date.valueOf(checkIn));
            pstmt.setInt(2, lengthOfStay);
            pstmt.setString(3, paymentInfo);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        reservationId = rs.getLong(1);
                        System.out.println("Reservation added successfully with ID: " + reservationId);
                    }
                }
            } else {
                System.out.println("Failed to add the reservation.");
                return;
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
            return; 
        }

        if (reservationId != -1) {
            HashMap<Integer, Integer> availableRooms = Room.availableRooms(hotelAddress, checkIn, checkOut, conn);
            
            if (availableRooms.isEmpty()) {
                System.out.println("No available rooms for the specified dates. Please choose different dates or a different hotel.");
                return;
            }
        
            // Display available rooms
            System.out.println("\nAvailable rooms:");
            for (Map.Entry<Integer, Integer> entry : availableRooms.entrySet()) {
                System.out.printf("Room Number: %d, Price per Night: $%d\n", entry.getKey(), entry.getValue());
            }
        
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
        
            // Insert the booking into the 'books' table
            String insertBookingSQL = "INSERT INTO books (roomNumber, hotelAddress, reservationId, checkInDate, checkOutDate) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement bookingStmt = conn.prepareStatement(insertBookingSQL)) {
                bookingStmt.setInt(1, roomNumber);
                bookingStmt.setString(2, hotelAddress);
                bookingStmt.setLong(3, reservationId);
                bookingStmt.setDate(4, Date.valueOf(checkIn));
                bookingStmt.setDate(5, Date.valueOf(checkOut));
        
                int bookingAffectedRows = bookingStmt.executeUpdate();
                if (bookingAffectedRows > 0) {
                    System.out.println("Room successfully booked for your reservation.");
                } else {
                    System.out.println("Failed to book the room. Please try again.");
                }
            } catch (SQLException e) {
                System.out.println("Error booking room: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private static int calculateLengthOfStay(String checkIn, String checkOut) {
        LocalDate arrivalDate = LocalDate.parse(checkIn);
        LocalDate checkoutDate = LocalDate.parse(checkOut);
        return (int) ChronoUnit.DAYS.between(arrivalDate, checkoutDate);
    }
}