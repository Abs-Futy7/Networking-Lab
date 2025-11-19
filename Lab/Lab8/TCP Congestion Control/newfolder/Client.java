
 
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try {
            // Check user input
            Scanner sc = new Scanner(System.in);
            System.out.println("Enter TCP mode (TAHOE/RENO): ");
            System.out.println("Select Mode: ");
            System.out.println("1. TAHOE");
            System.out.println("2. RENO");
            
            int choice = sc.nextInt();
            String mode = "";
            if (choice == 1) {
                mode = "TAHOE";
            } else if (choice == 2) {
                mode = "RENO";
            } else {
                System.out.println("Invalid choice! Use either '1' for TAHOE or '2' for RENO.");
                sc.close();
                return;
            }
           
            if (!mode.equals("TAHOE") && !mode.equals("RENO")) {
                System.out.println("Invalid mode! Use either 'TAHOE' or 'RENO'.");
                sc.close();
                return;
            }

            // Initialize performance metrics
            PerformanceMetrics metrics = new PerformanceMetrics(mode);

            // Create the TCP socket (custom implementation)
            MyTCPSocket socket = new MyTCPSocket(mode, metrics);

            // Start data transmission (15 rounds for better analysis)
            socket.startTransmission(15);

            // Generate performance report and CSV
            metrics.generateReport();
            metrics.generateCSV();
            sc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
