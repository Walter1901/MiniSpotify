package debug;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class PortScanner {
    // Common ports to test
    private static final int[] COMMON_PORTS = {
            8080, 8000, 9090, 9000, 5000, 3000, 3306,
            1234, 4321, 12345, 12346, 9876, 7777, 6666
    };

    public static void main(String[] args) {
        System.out.println("==== CHECKING COMMON PORTS ====");
        List<Integer> availablePorts = new ArrayList<>();

        // Check status of common ports
        for (int port : COMMON_PORTS) {
            boolean isAvailable = isPortAvailable(port);
            System.out.println("Port " + port + ": " + (isAvailable ? "✅ AVAILABLE" : "❌ IN USE"));

            if (isAvailable) {
                availablePorts.add(port);
            }
        }

        // Find some random available ports
        System.out.println("\n==== FINDING AVAILABLE PORTS ====");
        for (int i = 0; i < 5; i++) {
            int port = findAvailablePort();
            System.out.println("Found available port: " + port);
            availablePorts.add(port);
        }

        // Recommendation
        System.out.println("\n==== RECOMMENDATION ====");
        if (!availablePorts.isEmpty()) {
            System.out.println("Try using one of these available ports in both your client and server:");
            for (int port : availablePorts) {
                System.out.println("- " + port);
            }
        } else {
            System.out.println("Unable to find available ports. Please check your firewall settings.");
        }
    }

    // Check if a specific port is available
    private static boolean isPortAvailable(int port) {
        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;

        try {
            // Check TCP port
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);

            // Check UDP port
            datagramSocket = new DatagramSocket(port);
            datagramSocket.setReuseAddress(true);

            return true;
        } catch (IOException e) {
            return false;
        } finally {
            if (datagramSocket != null) {
                datagramSocket.close();
            }

            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    // Can't do much here
                }
            }
        }
    }

    // Find a random available port
    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("No free port found", e);
        }
    }
}