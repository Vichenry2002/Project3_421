package options;

import java.sql.*;
import java.util.Scanner;

public class Billing {

    public static void listBillings(Scanner scanner, Connection conn) {
        while (true) {
            System.out.println();
            System.out.println("--- List Billings ---");
            System.out.println("Provide associated Reservation Id (enter 'q' to go back to the main menu):");

            String reservationIdInput = scanner.nextLine();
            System.out.println();
            if(reservationIdInput.equalsIgnoreCase("q")){
                break;
            }

            int reservationId;
            try {
                reservationId = Integer.parseInt(reservationIdInput);
            } catch (NumberFormatException e) {
                System.out.println("Invalid reservation ID format. Please try again.");
                continue;
            }

            String sql = "SELECT billingId, billingDate, receipt, billedAmount FROM billings WHERE reservationId = ?";
            double totalAmount = 0;

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, reservationId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    System.out.printf("%-10s %-12s %-30s %-15s%n", "Billing ID", "Date", "Receipt", "Amount");
                    while (rs.next()) {
                        int billingId = rs.getInt("billingId");
                        Date billingDate = rs.getDate("billingDate");
                        String receipt = rs.getString("receipt");
                        double billedAmount = rs.getDouble("billedAmount");

                        System.out.printf("%-10d %-12s %-30s %-15.2f%n", billingId, billingDate, receipt, billedAmount);

                        totalAmount += billedAmount;
                    }

                    if (totalAmount == 0) {
                        System.out.println("No billings found for the given Reservation ID.");
                    } else {
                        System.out.printf("\nTotal Billed Amount for Reservation ID %d: %.2f%n", reservationId, totalAmount);
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
