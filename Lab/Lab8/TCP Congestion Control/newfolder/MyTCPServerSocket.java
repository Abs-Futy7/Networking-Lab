import java.io.*;
import java.net.*;
import java.util.*;

public class MyTCPServerSocket {
    private DatagramSocket socket;
    private int port;
    private Random random;

    private boolean connectionEstablished = false;
    private int expectedSeqNum = 1;

    public MyTCPServerSocket(int port) throws Exception {
        this.port = port;
        this.socket = new DatagramSocket(port);
        this.random = new Random();
        System.out.println("The server started on port " + port);
    }

    // ---------------------------
    // Main listening loop
    // ---------------------------
    public void listen() throws Exception {
        InetAddress clientAddr = null;
        int clientPort = 0;
        
        while (true) {
            MyTCPPacket packet = receive();

            // Store client address and port from first packet
            if (clientAddr == null) {
                clientAddr = packet.clientAddr;
                clientPort = packet.clientPort;
            }

            // FIN received -> close connection
            if (packet.FIN) {
                System.out.println("Received FIN. Closing connection...");
                
                // Send FIN-ACK
                MyTCPPacket finAck = new MyTCPPacket(0, packet.seqNum + 1, false, true, true, "");
                send(finAck, clientAddr, clientPort);
                
                socket.close();
                break;
            }

            // 3-way handshake
            if (packet.SYN && !connectionEstablished) {
                System.out.println("Client connected: /" + packet.clientAddr.getHostAddress());

                MyTCPPacket synAck = new MyTCPPacket(1, packet.seqNum + 1, true, true, false, "");
                send(synAck, packet.clientAddr, packet.clientPort);

                connectionEstablished = true;
                continue;
            }

            // Data transmission phase
            if (connectionEstablished && !packet.SYN && !packet.FIN) {
                handleDataTransmission(packet, clientAddr, clientPort);
            }
        }
    }

    // ---------------------------
    // Handle data transmission with packet loss simulation
    // ---------------------------
    private void handleDataTransmission(MyTCPPacket packet, InetAddress clientAddr, int clientPort) throws Exception {
        String receivedData = packet.data; // Data packets separated by |||
        String[] dataPackets = receivedData.split("\\|\\|\\|");
        
        // Parse and save received data
        List<String> packetIds = new ArrayList<>();
        List<String> actualData = new ArrayList<>();
        
        System.out.println("Received " + dataPackets.length + " data packets:");
        for (String dataPacket : dataPackets) {
            String[] parts = dataPacket.split(":", 2); // Split on first colon only
            if (parts.length == 2) {
                String packetId = parts[0];
                String data = parts[1];
                packetIds.add(packetId);
                actualData.add(data);
                
                // Display received data (truncate if too long)
                String displayData = data.length() > 50 ? data.substring(0, 50) + "..." : data;
                System.out.println("  " + packetId + ": \"" + displayData + "\"");
                
                // Save data to file
                saveReceivedData(packetId, data);
            }
        }
        
        // Randomly choose one packet to simulate as lost (or no loss)
        int lossIndex = random.nextInt(packetIds.size() + 1); // +1 for no loss option
        
        if (lossIndex == packetIds.size()) {
            // No packet loss - send normal ACKs
            for (String packetId : packetIds) {
                MyTCPPacket ack = new MyTCPPacket(0, 0, false, true, false, "ACK:" + packetId);
                send(ack, clientAddr, clientPort);
            }
        } else {
            // Packet loss simulation
            String lostPacketId = packetIds.get(lossIndex);
            System.out.println("Simulating loss of: " + lostPacketId);
            
            for (int i = 0; i < packetIds.size(); i++) {
                if (i == lossIndex) {
                    // Lost packet - send 3 duplicate ACKs
                    if (i == 0) {
                        // First packet lost - send "ACK:NA" three times
                        for (int j = 0; j < 3; j++) {
                            MyTCPPacket ack = new MyTCPPacket(0, 0, false, true, false, "ACK:NA");
                            send(ack, clientAddr, clientPort);
                        }
                    } else {
                        // Send 3 duplicate ACKs for previous packet
                        String prevPacketId = packetIds.get(i - 1);
                        for (int j = 0; j < 3; j++) {
                            MyTCPPacket ack = new MyTCPPacket(0, 0, false, true, false, "ACK:" + prevPacketId);
                            send(ack, clientAddr, clientPort);
                        }
                    }
                } else if (i < lossIndex) {
                    // Packets before lost packet - send normal ACK
                    MyTCPPacket ack = new MyTCPPacket(0, 0, false, true, false, "ACK:" + packetIds.get(i));
                    send(ack, clientAddr, clientPort);
                }
                // Packets after lost packet are not ACKed (TCP behavior)
            }
        }
    }
    
    // ---------------------------
    // Save received data to file
    // ---------------------------
    private void saveReceivedData(String packetId, String data) {
        try (PrintWriter writer = new PrintWriter(new FileWriter("received_data.txt", true))) {
            writer.println("[" + packetId + "] " + data);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
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
        MyTCPPacket packet = (MyTCPPacket) ois.readObject();

        packet.clientAddr = dp.getAddress();
        packet.clientPort = dp.getPort();
        return packet;
    }

    // ---------------------------
    // Send packet
    // ---------------------------
    private void send(MyTCPPacket packet, InetAddress clientAddr, int clientPort) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(packet);
        byte[] data = baos.toByteArray();

        DatagramPacket dp = new DatagramPacket(data, data.length, clientAddr, clientPort);
        socket.send(dp);
    }
}
