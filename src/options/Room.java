package options;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Room {

    public static void handleRooms(Scanner scanner, Connection conn){
        String choice;
        do {
            System.out.println();
            System.out.println("-- Room Availability --");
            System.out.println("1. Check Room Availability for Bedrooms");
            System.out.println("2. Check Room Availability for Events");
            System.out.print("Enter choice number (enter 'q' to go to main menu): ");
            choice = scanner.nextLine();
            System.out.println();

            switch (choice) {
                case "1":
                    // Call method to check bedroom availability
                    listBedroomAvailability(scanner, conn);
                    break;
                case "2":
                    // Call method to check event availability
                    listConferenceRoomAvailability(scanner, conn);
                    break;
                default:
                    System.out.println("Invalid choice. Please select a number from 1 to 2.");
                    break;
            }

        } while (!choice.equals("q"));
        

    }
    
    public static void listBedroomAvailability(Scanner scanner, Connection conn){
        System.out.println("Insert the number of the corresponding hotel you would like to search for availability:");
        System.out.println("1. Cityville Central - 123 Main St, Cityville");
        System.out.println("2. Metropolis Grand - 456 Grand Ave, Metropolis");
        System.out.println("3. Lakeside Retreat - 789 River Rd, Lakeside");
        System.out.println("4. Greenfield Oasis - 1010 Forest Dr, Greenfield");
        System.out.println("5. Seaside Resort  - 1212 Coastal Way, Seaside");
        System.out.print("Enter choice number (enter 'b' to go back): ");
        String choice = scanner.nextLine();
        System.out.println();

        if(choice.equals("b")){
            return;
        }

        while(!(choice.equals("1") || choice.equals("2") || choice.equals("3") || choice.equals("4") || choice.equals("5"))){
            System.out.println("Invalid choice. Please select a number from 1 to 5. Or enter 'b' to go back");
            choice = scanner.nextLine();
        }

        int hotel_idx = Integer.parseInt(choice);
        HashMap<Integer, String> hotel_map = new HashMap<>();
        hotel_map.put(1, "123 Main St, Cityville");
        hotel_map.put(2, "456 Grand Ave, Metropolis");
        hotel_map.put(3, "789 River Rd, Lakeside");
        hotel_map.put(4, "1010 Forest Dr, Greenfield");
        hotel_map.put(5, "1212 Coastal Way, Seaside");

        String hotel_address = hotel_map.get(hotel_idx);

        String checkIn, checkOut;
        String dateFormat = "\\d{4}-\\d{2}-\\d{2}"; // Regular expression for YYYY-MM-DD

        // Validate check-in date
        while (true) {
            System.out.println("Insert check-in date (YYYY-MM-DD):");
            checkIn = scanner.nextLine();
            if (checkIn.matches(dateFormat)) {
                break; // Break the loop if the format is correct
            } else {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        // Validate check-out date
        while (true) {
            System.out.println("Insert check-out date (YYYY-MM-DD):");
            checkOut = scanner.nextLine();
            if (checkOut.matches(dateFormat)) {
                break; // Break the loop if the format is correct
            } else {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        HashMap<Integer, Integer> available_rooms = availableRooms(hotel_address, checkIn, checkOut, conn);
        System.out.println();
        System.out.print("Available rooms for specified dates and location: ");
        for (Map.Entry<Integer, Integer> entry : available_rooms.entrySet()) {
            System.out.println("Room Number: " + entry.getKey() + ", Price per Night: $" + entry.getValue());
        }


    }

    public static void listConferenceRoomAvailability(Scanner scanner, Connection conn){

    }

    public static HashMap<Integer, Integer> availableRooms(String hotelAddress, String checkInDate, String checkOutDate, Connection conn) {
        HashMap<Integer, Integer> availableRooms = new HashMap<>();

        try {
            // Get all the rooms for the hotel address
            String sqlBedrooms = "SELECT roomnumber, pricepernight FROM bedrooms WHERE hoteladdress = ?";
            PreparedStatement psBedrooms = conn.prepareStatement(sqlBedrooms);
            psBedrooms.setString(1, hotelAddress);
            ResultSet rsBedrooms = psBedrooms.executeQuery();

            // Store all rooms in a temporary HashMap
            HashMap<Integer, Integer> allRooms = new HashMap<>();
            while (rsBedrooms.next()) {
                allRooms.put(rsBedrooms.getInt("roomnumber"), rsBedrooms.getInt("pricepernight"));
            }

            // Find all booked rooms within the date range
            String sqlBooks = "SELECT roomnumber FROM books WHERE hoteladdress = ? AND ((checkindate BETWEEN ? AND ?) OR (checkoutdate BETWEEN ? AND ?) OR (checkindate < ? AND checkoutdate > ?))";
            PreparedStatement psBooks = conn.prepareStatement(sqlBooks);
            psBooks.setString(1, hotelAddress);
            psBooks.setString(2, checkInDate);
            psBooks.setString(3, checkOutDate);
            psBooks.setString(4, checkInDate);
            psBooks.setString(5, checkOutDate);
            psBooks.setString(6, checkInDate);
            psBooks.setString(7, checkOutDate);
            ResultSet rsBooks = psBooks.executeQuery();

            // Remove the booked rooms from the allRooms HashMap
            while (rsBooks.next()) {
                allRooms.remove(rsBooks.getInt("roomnumber"));
            }

            // The remaining rooms in allRooms are available
            availableRooms.putAll(allRooms);

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return availableRooms;
    }


}
