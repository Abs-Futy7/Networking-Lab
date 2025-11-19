import java.util.*;
import java.io.*;

public class PerformanceMetrics {
    private List<Integer> cwndHistory;
    private List<Integer> ssthreshHistory;
    private List<Integer> roundNumbers;
    private List<Long> rttHistory;
    private List<Boolean> packetLossHistory;
    private int totalPacketsSent;
    private int totalPacketsLost;
    private long startTime;
    private String algorithm;
    
    public PerformanceMetrics(String algorithm) {
        this.algorithm = algorithm;
        this.cwndHistory = new ArrayList<>();
        this.ssthreshHistory = new ArrayList<>();
        this.roundNumbers = new ArrayList<>();
        this.rttHistory = new ArrayList<>();
        this.packetLossHistory = new ArrayList<>();
        this.totalPacketsSent = 0;
        this.totalPacketsLost = 0;
        this.startTime = System.currentTimeMillis();
    }
    
    public void recordRound(int round, int cwnd, int ssthresh, long rtt, boolean packetLoss, int packetsSent) {
        roundNumbers.add(round);
        cwndHistory.add(cwnd);
        ssthreshHistory.add(ssthresh);
        rttHistory.add(rtt);
        packetLossHistory.add(packetLoss);
        totalPacketsSent += packetsSent;
        if (packetLoss) {
            totalPacketsLost++;
        }
    }
    
    public double getThroughput() {
        long totalTime = System.currentTimeMillis() - startTime;
        return (double) totalPacketsSent / (totalTime / 1000.0); // packets per second
    }
    
    public double getPacketLossRate() {
        return (double) totalPacketsLost / totalPacketsSent * 100;
    }
    
    public double getAverageRTT() {
        return rttHistory.stream().mapToLong(Long::longValue).average().orElse(0);
    }
    
    public void generateCSV() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(algorithm + "_metrics.csv"));
            writer.println("Round,CWND,SSThresh,RTT,PacketLoss");
            
            for (int i = 0; i < roundNumbers.size(); i++) {
                writer.printf("%d,%d,%d,%d,%s%n",
                    roundNumbers.get(i),
                    cwndHistory.get(i),
                    ssthreshHistory.get(i),
                    rttHistory.get(i),
                    packetLossHistory.get(i) ? "1" : "0"
                );
            }
            writer.close();
            System.out.println("CSV file generated: " + algorithm + "_metrics.csv");
        } catch (IOException e) {
            System.err.println("Error writing CSV: " + e.getMessage());
        }
    }
    
    public void generateReport() {
        System.out.println("\n=== " + algorithm + " Performance Report ===");
        System.out.printf("Total Rounds: %d%n", roundNumbers.size());
        System.out.printf("Total Packets Sent: %d%n", totalPacketsSent);
        System.out.printf("Total Packets Lost: %d%n", totalPacketsLost);
        System.out.printf("Packet Loss Rate: %.2f%%%n", getPacketLossRate());
        System.out.printf("Throughput: %.2f packets/sec%n", getThroughput());
        System.out.printf("Average RTT: %.2f ms%n", getAverageRTT());
        System.out.printf("Max CWND: %d%n", Collections.max(cwndHistory));
        System.out.printf("Min CWND: %d%n", Collections.min(cwndHistory));
    }
    
    public List<Integer> getCwndHistory() { return cwndHistory; }
    public List<Integer> getRoundNumbers() { return roundNumbers; }
    public List<Integer> getSsthreshHistory() { return ssthreshHistory; }
    public List<Long> getRttHistory() { return rttHistory; }
}