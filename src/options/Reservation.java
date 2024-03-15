package options;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Reservation {
    public static void newReservation(Scanner scanner, Connection conn) {
        System.out.println("\n--- Add New Reservation ---");

        String hotelAddress = chooseHotel(scanner);
        String arrivalDateStr = validateDate(scanner, "check-in");
        String checkoutDateStr = validateDate(scanner, "check-out");
        int lengthOfStay = calculateLengthOfStay(arrivalDateStr, checkoutDateStr);

        // Prompt for payment information
        System.out.print("Enter payment information (*card* ending in last *4 digits*): ");
        String paymentInfo = scanner.nextLine();

        // Initialize reservationId with a default or invalid value
        long reservationId = -1;

        // Inserting reservation into database
        String sql = "INSERT INTO reservations (arrivalDate, lengthOfStay, paymentInfo) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setDate(1, Date.valueOf(arrivalDateStr));
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
                return; // Exit if reservation creation failed
            }
        } catch (SQLException e) {
            System.out.println("SQL Error: " + e.getMessage());
            e.printStackTrace();
            return; // Exit in case of SQL exception
        }

        if (reservationId != -1) {
            HashMap<Integer, Integer> availableRooms = Room.availableRooms(hotelAddress, arrivalDateStr, checkoutDateStr, conn);
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
                bookingStmt.setDate(4, Date.valueOf(arrivalDateStr));
                bookingStmt.setDate(5, Date.valueOf(checkoutDateStr));
        
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

    private static String chooseHotel(Scanner scanner) {
        String hotelAddress = "";
        while (true) {
            System.out.println("Select hotel address:");
            System.out.println("1. Cityville Central - 123 Main St, Cityville");
            System.out.println("2. Metropolis Grand - 456 Grand Ave, Metropolis");
            System.out.println("3. Lakeside Retreat - 789 River Rd, Lakeside");
            System.out.println("4. Greenfield Oasis - 1010 Forest Dr, Greenfield");
            System.out.println("5. Seaside Resort - 1212 Coastal Way, Seaside");
            System.out.print("Enter choice (1-5): ");

            String input = scanner.nextLine();
            switch (input) {
                case "1": return "123 Main St, Cityville";
                case "2": return "456 Grand Ave, Metropolis";
                case "3": return "789 River Rd, Lakeside";
                case "4": return "1010 Forest Dr, Greenfield";
                case "5": return "1212 Coastal Way, Seaside";
                default: System.out.println("Invalid choice. Please select a number from 1 to 5.");
            }
        }
    }

    private static String validateDate(Scanner scanner, String dateType) {
        String dateStr;
        String dateFormat = "\\d{4}-\\d{2}-\\d{2}";
        while (true) {
            System.out.printf("Insert %s date (YYYY-MM-DD):", dateType);
            dateStr = scanner.nextLine();
            if (dateStr.matches(dateFormat)) {
                break; // Break the loop if the format is correct
            } else {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }
        return dateStr;
    }

    private static int calculateLengthOfStay(String arrivalDateStr, String checkoutDateStr) {
        LocalDate arrivalDate = LocalDate.parse(arrivalDateStr);
        LocalDate checkoutDate = LocalDate.parse(checkoutDateStr);
        return (int) ChronoUnit.DAYS.between(arrivalDate, checkoutDate);
    }

}

