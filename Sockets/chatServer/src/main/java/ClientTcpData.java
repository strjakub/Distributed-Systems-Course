import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public record ClientTcpData(Socket socket, BufferedReader in, PrintWriter out, int id) {}
