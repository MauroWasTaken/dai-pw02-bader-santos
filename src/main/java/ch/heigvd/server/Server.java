package ch.heigvd.server;

import static ch.heigvd.server.functions.GameFunctions.gameLoop;
import static ch.heigvd.server.functions.Login.login;
import static ch.heigvd.server.functions.Matchmaking.*;

import ch.heigvd.client.Client;
import ch.heigvd.common.Game;
import ch.heigvd.common.Norms;
import ch.heigvd.common.Player;
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

/**
 * Main class of the network game server.
 *
 * This class is used as the command "server" by Picocli. It opens a ServerSocket, accepts
 * incoming connections, and assigns each client to a ClientHandler executed inside a thread pool.
 */
@CommandLine.Command(name = "server", description = "Start the server part of the network game.")
public class Server implements Callable<Integer> {
  /** Number of connected players. AtomicInteger ensures safe concurrent access. */
  static AtomicInteger playerCount = new AtomicInteger(0);
  
  /** List of all connected players (thread-safe). */
  public static CopyOnWriteArrayList<Player> players = new CopyOnWriteArrayList<>();

  /** Messages sent by the server to clients. */
  public enum Message {
    OK,
    ERROR,
    CHALLENGES,
    REFUSE,
    GAMESTART,
    PLAY,
    GAME_OVER;
  }
  /**
   * Number of threads available to handle players. This value can be set using the -t or --threads
   * options.
   */
  @CommandLine.Option(
      names = {"-t", "--threads"},
      description = "Number of threads to use for players (default: ${DEFAULT-VALUE}).",
      defaultValue = "12")
  protected static int playerThreads;

  /** TCP port on which the server listens. This value can be set using the -p or --port options. */
  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port to use (default: ${DEFAULT-VALUE}).",
      defaultValue = "42069")
  protected int port;

    /**
   * Method executed by Picocli when running the command.
   *
   * This method: - Creates a ServerSocket on the specified port - Initializes a thread pool -
   * Accepts incoming connections - Starts a ClientHandler for each new connection
   *
   * @return 0 if everything went well, 1 in case of an IO error.
   */
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
   /**
   * Class responsible for handling a single client.
   *
   * Each instance is created when a new client connection is accepted.
   */
  static class ClientHandler implements Runnable {

    private final Socket socket;
    private boolean inGame = false;
    private Game game = null;
    private Player player = null;
    /**
     * Creates a handler for a given client. If the server is full, the connection is refused.
     *
     * @param socket the client's socket
     */
    public ClientHandler(Socket socket) {
      this.socket = socket;
      if (playerCount.get() >= playerThreads) {
        try {
          BufferedWriter out =
              new BufferedWriter(
                  new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
          out.write(Message.ERROR + Norms.END_OF_LINE);
          out.flush();
          socket.close();
        } catch (IOException e) {
          System.out.println("[Server] Could not close socket: " + e);
        }
      } else {
        playerCount.addAndGet(1);
      }
    }
    /**
     * Main loop of the client handler.
     *
     * This method handles: - Messages received from the client - Login - Matchmaking and lobby
     * actions - Exceptions and cleanup
     *
     * @throws RuntimeException in case of unexpected error
     */
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
        out.write(Message.OK + Norms.END_OF_LINE);
        out.flush();
        // login
        player = login(socket, in, out);
        if (player == null) {
          return;
        }

        while (!socket.isClosed()) {
          if (inGame) {
            // game functions
            gameLoop(socket, in, out, game, player.username);
            inGame = false;
          } else {
            // lobby functions
            String clientResponse = in.readLine();
            String[] clientResponseParts = clientResponse.split(" ", 2);
            Client.Message message = Client.Message.valueOf(clientResponseParts[0]);
            Game result;
            switch (message) {
                // matchmaking functions
              case CHALLENGES:
                getChallenges(out, player);
                break;
              case CHALLENGE:
                game = challengePlayer(socket, out, player, players, clientResponseParts[1]);
                if (game != null) {
                  inGame = true;
                }
                break;
              case ACCEPT:
                game = acceptChallenge(socket, in, out, player, clientResponseParts[1]);
                if (game != null) {
                  inGame = true;
                }
                break;
              case REFUSE:
                refuseChallenge(socket, in, out, player, clientResponseParts[1]);
                break;
              case QUIT:
                socket.close();
                break;
              default:
                break;
            }
          }
        }
        playerCount.addAndGet(-1);
        players.removeIf(p -> Objects.equals(p.username, player.username));
        System.out.println("[Server] closing connection");
      } catch (IOException e) {
        if (playerCount.get() >= playerThreads) {
          System.out.println("[Server] No available slots " + playerCount + "/" + playerThreads);
        } else {
          System.out.println("[Server] exception: " + e);
        }
      }
    }
  }
}
