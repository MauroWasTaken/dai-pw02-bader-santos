package ch.heigvd.Server;

import static ch.heigvd.Server.Functions.Login.login;

import ch.heigvd.Common.Player;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Start the server part of the network game.")
public class Server implements Callable<Integer> {
  static AtomicInteger playerCount = new AtomicInteger(0);
  public static CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();
  // End of line character
  public static String END_OF_LINE = "\n";

  public enum Message {
    OK,
    ERROR,
  }

  @CommandLine.Option(
      names = {"-t", "--threads"},
      description = "Number of threads to use for players (default: ${DEFAULT-VALUE}).",
      defaultValue = "12")
  protected static int playerThreads;

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port to use (default: ${DEFAULT-VALUE}).",
      defaultValue = "42069")
  protected int port;

  @Override
  public Integer call() {
    try (ServerSocket serverSocket = new ServerSocket(port);
        ExecutorService executor = Executors.newFixedThreadPool(playerThreads + 1)) {
      System.out.println("[SERVER] Listening on port " + port);

      while (!serverSocket.isClosed()) {
        Socket clientSocket = serverSocket.accept();
        executor.submit(new ClientHandler(clientSocket));
      }
    } catch (IOException e) {
      System.out.println("[Server] IO exception: " + e);
      return 1;
    }

    return 0;
  }

  static class ClientHandler implements Runnable {

    private final Socket socket;

    public ClientHandler(Socket socket) {
      this.socket = socket;
      if (playerCount.get() >= playerThreads) {
        try {
          BufferedWriter out =
              new BufferedWriter(
                  new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
          out.write(Message.ERROR + END_OF_LINE);
          out.flush();
          socket.close();
        } catch (IOException e) {
          System.out.println("[Server] Could not close socket: " + e);
        }
      } else {
        playerCount.addAndGet(1);
      }
    }

    @Override
    public void run() {
      try (socket;
          BufferedReader in =
              new BufferedReader(
                  new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
          BufferedWriter out =
              new BufferedWriter(
                  new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8))) {
        System.out.println(playerCount.get() + " / " + playerThreads + " players connected.");
        out.write(Message.OK + END_OF_LINE);
        out.flush();
        String username = login(socket, in, out);
        if (username == null) {
          return;
        }
        while (!socket.isClosed()) {
          // socket.close();
        }
        playerCount.addAndGet(-1);
        players.removeIf(player -> Objects.equals(player.username, username));
        System.out.println("[Server] closing connection");
      } catch (IOException e) {
        if (playerCount.get() >= playerThreads) {
          System.out.println("[Server] No avaible slots " + playerCount + "/" + playerThreads);
        } else {
          System.out.println("[Server] exception: " + e);
        }
      }
    }
  }
}
