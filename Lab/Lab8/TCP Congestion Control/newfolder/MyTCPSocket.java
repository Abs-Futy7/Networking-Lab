import java.io.*;
import java.net.*;
import java.util.*;

public class MyTCPSocket {
    private DatagramSocket socket;
    private InetAddress serverAddr;
    private int serverPort;
    private PerformanceMetrics metrics;

    // Congestion control parameters
    private int cwnd = 1;          // Congestion window
    private int ssthresh = 8;      // Slow start threshold
    private int dupACKcount = 0;   // Duplicate ACK counter
    private String mode;           // "TAHOE" or "RENO"

    public MyTCPSocket(String mode, PerformanceMetrics metrics) throws Exception {
        this.mode = mode.toUpperCase();
        this.metrics = metrics;
        socket = new DatagramSocket();
        serverAddr = InetAddress.getByName("127.0.0.1");
        serverPort = 5000;

        handshake();
    }

    // ---------------------------
    // 3-way Handshake
    // ---------------------------
    private void handshake() throws Exception {
        System.out.println("Starting TCP " + mode + " Mode");

        // Step 1: Send SYN
        MyTCPPacket syn = new MyTCPPacket(0, 0, true, false, false, "");
        send(syn);

        // Step 2: Receive SYN-ACK
        MyTCPPacket synAck = receive();

        // Step 3: Send ACK
        MyTCPPacket ack = new MyTCPPacket(1, synAck.seqNum + 1, false, true, false, "");
        send(ack);

        System.out.println("Handshake complete. Starting transmission...\n");
    }

    // ---------------------------
    // Send packet
    // ---------------------------
    private void send(MyTCPPacket packet) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(packet);
        byte[] data = baos.toByteArray();

        DatagramPacket dp = new DatagramPacket(data, data.length, serverAddr, serverPort);
        socket.send(dp);
    }

    // ---------------------------
    // Receive packet
    // ---------------------------
    private MyTCPPacket receive() throws Exception {
        byte[] buf = new byte[4096];
        DatagramPacket dp = new DatagramPacket(buf, buf.length);
        socket.receive(dp);

        ByteArrayInputStream bais = new ByteArrayInputStream(dp.getData());
        ObjectInputStream ois = new ObjectInputStream(bais);
        return (MyTCPPacket) ois.readObject();
    }

    // ---------------------------
    // Read data file for transmission
    // ---------------------------
    private List<String> readDataFile(String filename) throws Exception {
        List<String> data = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) { // Skip empty lines
                    data.add(line.trim());
                }
            }
        }
        return data;
    }

    // ---------------------------
    // Main Transmission Logic
    // ---------------------------
    public void startTransmission(int totalRounds) throws Exception {
        // Read the data file
        List<String> fileData = readDataFile("sample_data.txt");
        int totalDataChunks = fileData.size();
        int currentDataIndex = 0;
        int globalPacketCounter = 1;
        
        System.out.println("Loaded " + totalDataChunks + " lines of data from sample_data.txt");
        System.out.println("Starting data transmission...\n");

        for (int round = 1; round <= totalRounds && currentDataIndex < totalDataChunks; round++) {
            long roundStartTime = System.currentTimeMillis();
            
            System.out.println("== TCP " + mode + " Mode ==");
            System.out.println("Round " + round + ": cwnd = " + cwnd + ", ssthresh = " + ssthresh);
            
            // Create data packets for this round
            List<String> dataPackets = new ArrayList<>();
            StringBuilder sentPacketsStr = new StringBuilder("Sent packets: ");
            
            for (int i = 0; i < cwnd && currentDataIndex < totalDataChunks; i++) {
                String packetId = "pkt" + globalPacketCounter++;
                String actualData = fileData.get(currentDataIndex++);
                String packetData = packetId + ":" + actualData;
                dataPackets.add(packetData);
                
                if (i > 0) sentPacketsStr.append(", ");
                sentPacketsStr.append(packetId + " (\"" + 
                    (actualData.length() > 30 ? actualData.substring(0, 30) + "..." : actualData) + "\")");
            }

            System.out.println(sentPacketsStr.toString());

            // Send all packets as a single message (separator: |||)
            String allPackets = String.join("|||", dataPackets);
            MyTCPPacket dataPacket = new MyTCPPacket(round, 0, false, false, false, allPackets);
            send(dataPacket);

            // Receive ACKs - expect one ACK per packet sent
            boolean lossEvent = false;
            int expectedAcks = cwnd;
            int receivedAcks = 0;
            String lastAckReceived = "";
            
            while (receivedAcks < expectedAcks && !lossEvent) {
                try {
                    socket.setSoTimeout(3000); // 3 second timeout
                    MyTCPPacket ack = receive();
                    receivedAcks++;
                    
                    String currentAck = ack.data; // ACK format: "ACK:pkt1"
                    System.out.println("Received: " + currentAck);

                    // Check for duplicate ACKs
                    if (lastAckReceived.equals(currentAck) && !currentAck.equals("ACK:NA")) {
                        dupACKcount++;
                        
                        if (dupACKcount == 3) {
                            System.out.println("==> 3 Duplicate ACKs: Fast Retransmit triggered.");

                            ssthresh = Math.max(cwnd / 2, 1);

                            if (mode.equals("TAHOE")) {
                                cwnd = 1; // reset
                                System.out.println("TCP TAHOE Reset: cwnd -> " + cwnd);
                            } else if (mode.equals("RENO")) {
                                cwnd = ssthresh; // fast recovery
                                System.out.println("TCP RENO Fast Recovery: cwnd -> " + cwnd);
                            }

                            dupACKcount = 0;
                            lossEvent = true;
                        }
                    } else {
                        // New ACK received
                        dupACKcount = 1; // Reset to 1 as per algorithm
                        lastAckReceived = currentAck;
                    }
                } catch (SocketTimeoutException e) {
                    // Timeout - assume packet loss
                    System.out.println("Timeout occurred - assuming packet loss");
                    ssthresh = Math.max(cwnd / 2, 1);
                    cwnd = 1; // Both TAHOE and RENO reset on timeout
                    System.out.println("Timeout Reset: cwnd -> 1, ssthresh -> " + ssthresh);
                    lossEvent = true;
                    break;
                }
            }

            // Adjust cwnd based on phase (only if no loss event)
            if (!lossEvent) {
                if (cwnd < ssthresh) {
                    // Slow Start (exponential growth)
                    cwnd *= 2;
                    System.out.println("Slow Start: cwnd -> " + cwnd);
                } else {
                    // Congestion Avoidance (linear growth)
                    cwnd += 1;
                    System.out.println("Congestion Avoidance: cwnd -> " + cwnd);
                }
            }

            // Record performance metrics for this round
            long roundEndTime = System.currentTimeMillis();
            long roundRTT = roundEndTime - roundStartTime;
            metrics.recordRound(round, cwnd, ssthresh, roundRTT, lossEvent, cwnd);

            System.out.println();
            Thread.sleep(1000);
        }

        // Send FIN to close connection
        MyTCPPacket fin = new MyTCPPacket(0, 0, false, false, true, "");
        send(fin);
        socket.close();
        
        // Display transmission summary
        System.out.println("\n=== Data Transmission Complete ===");
        System.out.println("Total data chunks transmitted: " + currentDataIndex + " / " + totalDataChunks);
        if (currentDataIndex >= totalDataChunks) {
            System.out.println("All data transmitted successfully!");
        } else {
            System.out.println("Transmission stopped early (" + (totalDataChunks - currentDataIndex) + " chunks remaining)");
        }
        System.out.println("Check 'received_data.txt' on server side for received data.");
        System.out.println("Client disconnected.");
    }
}