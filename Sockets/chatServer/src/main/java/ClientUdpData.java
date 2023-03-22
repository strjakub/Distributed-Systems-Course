import java.net.DatagramSocket;
import java.net.InetAddress;

public record ClientUdpData(int id, InetAddress address, int port) {}
