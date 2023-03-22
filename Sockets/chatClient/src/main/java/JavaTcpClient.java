import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class JavaTcpClient {

    private static final int multicastPort = 4446;
    private static final int portNumber = 12345;
    private static final String asciiART = """
                                          .
                                          A       ;
                                |   ,--,-/ \\---,-/|  ,
                               _|\\,'. /|      /|   `/|-.
                           \\`.'    /|      ,            `;.
                          ,'\\   A     A         A   A _ /| `.;
                        ,/  _              A       _  / _   /|  ;
                       /\\  / \\   ,  ,           A  /    /     `/|
                      /_| | _ \\         ,     ,             ,/  \\
                     // | |/ `.\\  ,-      ,       ,   ,/ ,/      \\/
                     / @| |@  / /'   \\  \\      ,              >  /|    ,--.
                    |\\_/   \\_/ /      |  |           ,  ,/        \\  ./' __:..
                    |  __ __  |       |  | .--.  ,         >  >   |-'   /     `
                  ,/| /  '  \\ |       |  |     \\      ,           |    /
                 /  |<--.__,->|       |  | .    `.        >  >    /   (
                /_,' \\\\  ^  /  \\     /  /   `.    >--            /^\\   |
                      \\\\___/    \\   /  /      \\__'     \\   \\   \\/   \\  |
                       `.   |/          ,  ,                  /`\\    \\  )
                         \\  '  |/    ,       V    \\          /        `-\\
                          `|/  '  V      V           \\    \\.'            \\_
                           '`-.       V       V        \\./'\\
                               `|/-.      \\ /   \\ /,---`\\
                                /   `._____V_____V'
                                           '     '""";

    public static void main(String[] args) throws UnknownHostException {
        System.out.println("JAVA CLIENT");
        String hostName = "127.0.0.1";
        InetAddress group = InetAddress.getByName("224.0.0.21");
        InetAddress unicast = InetAddress.getByName(hostName);
        byte[] receiveBuffer = new byte[1200];
        byte[] receiveBufferMulticast = new byte[1200];
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        try {
            Socket socket = new Socket(hostName, portNumber);
            DatagramSocket socketUdp = new DatagramSocket();
            DatagramSocket socketMulticast = new DatagramSocket();

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            startListeningThreads(in, socketUdp, group, receiveBuffer, receiveBufferMulticast);

            UdpSend(socketUdp, unicast, "new UDP user connected");
            while (true) {
                String msg = reader.readLine();
                if (msg == null) continue;
                switch (msg) {
                    case "/exit/" -> {
                        out.println("{disconnected}");
                        socket.close();
                        socketUdp.close();
                        socketMulticast.close();
                        System.exit(0);
                    }
                    case "M" -> multicast(socketMulticast, group);
                    case "U" -> UdpSend(socketUdp, unicast, asciiART);
                    default -> out.println(msg);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void UdpSend(DatagramSocket socket, InetAddress address, String msg) throws IOException {
        byte[] sendBuffer = msg.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, portNumber);
        socket.send(sendPacket);
    }

    public static String UdpReceive(DatagramSocket socket, byte[] receiveBuffer) throws IOException {
        Arrays.fill(receiveBuffer, (byte)0);
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);
        return new String(receivePacket.getData(), 0, receivePacket.getLength());
    }

    public static void multicast(DatagramSocket socket, InetAddress group) throws IOException {
        byte[] sendBuffer = asciiART.getBytes();
        DatagramPacket packet = new DatagramPacket(sendBuffer, sendBuffer.length, group, multicastPort);
        socket.send(packet);
    }

    public static void listenForMulticast(byte[] buf, InetAddress group) throws IOException {
        MulticastSocket multicastSocket = new MulticastSocket(multicastPort);
        multicastSocket.joinGroup(new InetSocketAddress(group, multicastPort), NetworkInterface.getByInetAddress(group));
        while (true) {
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            multicastSocket.receive(packet);
            System.out.println(new String(packet.getData(), 0, packet.getLength()));
        }
    }

    public static void startListeningThreads(BufferedReader in, DatagramSocket socketUdp, InetAddress group, byte[] receiveBuffer, byte[] receiveBufferMulticast){
        new Thread(() -> {
            try {
                while (true) {
                    if (in.ready()) {
                        System.out.println(in.readLine());
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                while (true) {
                    System.out.println(UdpReceive(socketUdp, receiveBuffer));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                listenForMulticast(receiveBufferMulticast, group);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

}
