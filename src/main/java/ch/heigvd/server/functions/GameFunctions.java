package ch.heigvd.server.functions;

import ch.heigvd.client.Client;
import ch.heigvd.common.Game;
import ch.heigvd.common.Norms;
import ch.heigvd.common.Player;
import ch.heigvd.server.Server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;

public class GameFunctions {
  public static void gameLoop(
      Socket socket, BufferedReader in, BufferedWriter out, Game game, String username) {
    while (!game.isOver.get() & !socket.isClosed()) {
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
                      game.player1.username.equals(username) ? game.player2 : game.player1;
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
                  out.write(Server.Message.GAME_OVER + " " + code + Norms.END_OF_LINE);
                  out.flush();
                  return;
                }
              }

              break;
            case QUIT:
              game.winner = game.player1.username.equals(username) ? game.player2 : game.player1;
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
            Player player = game.player1.username.equals(username) ? game.player2 : game.player1;
            player.wins++;
            player.winStreak++;
            player.status = Player.Status.ONLINE;
            out.write(Server.Message.GAME_OVER + " " + 3 + Norms.END_OF_LINE);
            out.flush();
            return;
          }
          if (game.isOver.get()) {
            int code;
            Player player = game.player1.username.equals(username) ? game.player2 : game.player1;
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
            out.write(Server.Message.GAME_OVER + " " + code + Norms.END_OF_LINE);
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

  public static boolean isMyTurn(Game game, String username) {
    return game.player1.username.equals(username) == game.isPlayer1Turn.get();
  }
}
