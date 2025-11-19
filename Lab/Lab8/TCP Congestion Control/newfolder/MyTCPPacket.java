import java.io.Serializable;
import java.net.InetAddress;

public class MyTCPPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public int seqNum;
    public int ackNum;
    public boolean SYN;
    public boolean ACK;
    public boolean FIN;
    public String data;
    
    // Client address and port (used by server to respond)
    public transient InetAddress clientAddr;
    public transient int clientPort;

    public MyTCPPacket(int seqNum, int ackNum, boolean SYN, boolean ACK, boolean FIN, String data) {
        this.seqNum = seqNum;
        this.ackNum = ackNum;
        this.SYN = SYN;
        this.ACK = ACK;
        this.FIN = FIN;
        this.data = data;
    }

    @Override
    public String toString() {
        return "[SEQ=" + seqNum + ", ACK=" + ackNum + ", SYN=" + SYN + ", ACKF=" + ACK +
                ", FIN=" + FIN + ", DATA=" + data + "]";
    }
}
