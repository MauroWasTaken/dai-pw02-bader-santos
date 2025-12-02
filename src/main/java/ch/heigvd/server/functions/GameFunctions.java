package ch.heigvd.server.functions;

import ch.heigvd.client.Client;
import ch.heigvd.common.Game;
import ch.heigvd.common.Norms;
import ch.heigvd.common.Player;
import ch.heigvd.server.Server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;

/**
 * Server-side game loop utilities.
 *
 * <p>This class contains the logic executed by the server to manage a single game session from the
 * server perspective. It parses client protocol messages, applies moves to the game model and
 * updates player statistics when the match ends.
 */
public class GameFunctions {
  /**
   * Main server-side game loop executed for a single client connection.
   *
   * <p>This method blocks while the game is active. It expects the client to send protocol messages
   * defined in {@code ch.heigvd.client.Client.Message} (PLAY, QUIT, ...). On game end the method
   * sends a GAME_OVER response and returns.
   *
   * @param socket client's socket
   * @param in reader attached to the client socket
   * @param out writer attached to the client socket
   * @param game shared Game instance for the two players
   * @param username username of the client associated with this connection
   */
  public static void gameLoop(
      Socket socket, BufferedReader in, BufferedWriter out, Game game, String username) {
    while (!game.isOver.get() && !socket.isClosed()) {
      if (isMyTurn(game, username)) {
        try {
          String clientResponse = in.readLine();
          String[] clientResponseParts = clientResponse.split(" ", 3);
          Client.Message message = Client.Message.valueOf(clientResponseParts[0]);
          switch (message) {
            case PLAY:
              int column, row;
              try {
                row = Integer.parseInt(clientResponseParts[1]);
                column = Integer.parseInt(clientResponseParts[2]);
              } catch (Exception e) {
                out.write(Server.Message.ERROR + " 1" + Norms.END_OF_LINE);
                out.flush();
                break;
              }
              if (!game.makeMove(row * 3 + column, username)) {
                out.write(Server.Message.ERROR + " 1" + Norms.END_OF_LINE);
                out.flush();
              } else {
                out.write(Server.Message.OK + Norms.END_OF_LINE);
                out.flush();
                if (game.isOver.get()) {
                  int code;
                  Player player =
                      game.player1.username.equals(username) ? game.player1 : game.player2;
                  player.status = Player.Status.ONLINE;
                  player.challenges.clear();
                  if (game.winner == null) {
                    code = 0;
                    player.draws++;
                    player.winStreak = 0;
                  } else if (game.winner.username.equals(username)) {
                    code = 1;
                    player.wins++;
                    player.winStreak++;
                  } else {
                    code = 2;
                    player.losses++;
                    player.winStreak = 0;
                  }
                  out.write(Server.Message.GAMEOVER + " " + code + Norms.END_OF_LINE);
                  out.flush();
                  return;
                }
              }

              break;
            case QUIT:
              game.winner = game.player1.username.equals(username) ? game.player1 : game.player2;
              game.hasDisconnect.set(true);
              game.isOver.set(true);
              socket.close();
              break;
            default:
              out.write(Server.Message.ERROR + " 1" + Norms.END_OF_LINE);
              out.flush();
              break;
          }
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        while (!isMyTurn(game, username) && !game.isOver.get()) {
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
        int lastRow = game.lastMove.get() / 3;
        int lastColumn = game.lastMove.get() % 3;
        try {
          if (game.hasDisconnect.get()) {
            Player player = game.player1.username.equals(username) ? game.player1 : game.player2;
            player.challenges.clear();
            player.wins++;
            player.winStreak++;
            player.status = Player.Status.ONLINE;
            out.write(Server.Message.GAMEOVER + " " + 3 + Norms.END_OF_LINE);
            out.flush();
            return;
          }
          if (game.isOver.get()) {
            int code;
            Player player = game.player1.username.equals(username) ? game.player1 : game.player2;
            player.challenges.clear();
            player.status = Player.Status.ONLINE;
            if (game.winner == null) {
              code = 0;
              player.draws++;
              player.winStreak = 0;
            } else if (game.winner.username.equals(username)) {
              code = 1;
              player.wins++;
              player.winStreak++;
            } else {
              code = 2;
              player.losses++;
              player.winStreak = 0;
            }
            out.write(Server.Message.GAMEOVER + " " + code + Norms.END_OF_LINE);
            out.flush();
            return;
          }
          out.write(Server.Message.PLAY + " " + lastRow + " " + lastColumn + Norms.END_OF_LINE);
          out.flush();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  /**
   * Indicates whether it is the specified player's turn in the given game.
   *
   * @param game game model
   * @param username username to check
   * @return true if it is this player's turn, false otherwise
   */
  public static boolean isMyTurn(Game game, String username) {
    return game.player1.username.equals(username) == game.isPlayer1Turn.get();
  }
}
