import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class JavaTcpServer {

    public static int portNumber = 12345;

    public static void main(String[] args){

        System.out.println("JAVA SERVER");
        int counterTCP = 1;
        byte[] receiveBuffer = new byte[1200];
        List<ClientTcpData> clients = Collections.synchronizedList(new ArrayList<>());
        List<ClientUdpData> clientsUdp = Collections.synchronizedList(new ArrayList<>());

        try {
            ServerSocket serverSocket = new ServerSocket(portNumber);
            DatagramSocket socketUdp = new DatagramSocket(portNumber);
            createUdpClientThread(receiveBuffer, socketUdp, clientsUdp).start();
            while (true) {
                Socket socket = serverSocket.accept();
                ClientTcpData clientSocket = new ClientTcpData(
                        socket,
                        new BufferedReader(new InputStreamReader(socket.getInputStream())),
                        new PrintWriter(socket.getOutputStream(), true),
                        counterTCP
                );
                clients.add(clientSocket);
                counterTCP++;
                System.out.println("client tcp connected");
                clientSocket.out().println("You are TCP user " + clientSocket.id());
                createTcpClientThread(clientSocket, clients).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Thread createTcpClientThread(ClientTcpData clientSocket, List<ClientTcpData> clients) {
        return new Thread(() -> {
            try {
                while (true) {
                    String msg = clientSocket.in().readLine();
                    if(msg == null) break;
                    if(!msg.isEmpty()) {
                        System.out.println("MSG from " + clientSocket.id() + ": " + msg);
                        for(PrintWriter socketOut : clients.stream().filter(x -> x.id() != clientSocket.id()).map(ClientTcpData::out).toList()){
                            socketOut.println("C" + clientSocket.id() + ": " + msg);
                        }
                    }
                }
                clients.remove(clientSocket);
            } catch (IOException e) {
                clients.remove(clientSocket);
            }
        });
    }

    public static Thread createUdpClientThread(byte[] receiveBuffer, DatagramSocket datagramSocket, List<ClientUdpData> clientsUdpData) {
        return new Thread(() -> {
            try {
                int counterUDP = 1;
                while (true){
                    DatagramPacket packet = UdpReceive(datagramSocket, receiveBuffer);
                    String msg = new String(packet.getData(), 0, packet.getLength());
                    System.out.println(msg);
                    if(clientsUdpData.stream().filter(x -> x.port() == packet.getPort() && x.address().equals(packet.getAddress())).toList().size() == 0) {
                        clientsUdpData.add(new ClientUdpData(counterUDP, packet.getAddress(), packet.getPort()));
                        UdpSend("You are UDP user " + counterUDP, datagramSocket, packet.getAddress(), packet.getPort());
                        counterUDP++;
                    }
                    Optional<Integer> senderID = clientsUdpData.stream().filter(x -> x.port() == packet.getPort() && x.address().equals(packet.getAddress())).map(ClientUdpData::id).findFirst();
                    for (ClientUdpData data: clientsUdpData.stream().filter(x -> x.port() != packet.getPort()).toList()) {
                        UdpSend("Udp from " + senderID.get() + ": " + msg, datagramSocket, data.address(), data.port());
                    }
                    System.out.println(clientsUdpData);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void UdpSend(String msg, DatagramSocket socket, InetAddress address, int port) throws IOException {
        byte[] sendBuffer = msg.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuffer, sendBuffer.length, address, port);
        socket.send(sendPacket);
    }

    public static DatagramPacket UdpReceive(DatagramSocket socket, byte[] receiveBuffer) throws IOException {
        Arrays.fill(receiveBuffer, (byte)0);
        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
        socket.receive(receivePacket);
        return receivePacket;
    }

}