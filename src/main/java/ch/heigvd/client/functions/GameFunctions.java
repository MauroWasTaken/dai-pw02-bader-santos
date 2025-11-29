package ch.heigvd.client.functions;

import static ch.heigvd.client.functions.UI.drawGame;

import ch.heigvd.client.Client;
import ch.heigvd.common.Game;
import ch.heigvd.common.Norms;
import ch.heigvd.server.Server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.net.Socket;

public class GameFunctions {
  public static void gameloop(
      Socket socket,
      BufferedReader in,
      BufferedWriter out,
      BufferedReader consoleReader,
      String username) {
    Game game = new Game();
    String playerSymbol = Client.myTurn ? "X" : "O";
    String opponentSymbol = Client.myTurn ? "O" : "X";
    while (!socket.isClosed())
      try {
        drawGame(username, game, Client.myTurn);

        if (Client.myTurn) {
          System.out.print("Enter your move (row and column): ");
          String userInput = consoleReader.readLine();
          String[] userInputParts = userInput.split(" ", 2);
          if (userInputParts[0].equalsIgnoreCase("QUIT")) {
            out.write(Client.Message.QUIT + " " + Norms.END_OF_LINE);
            out.flush();
            System.out.println("You have quit the game.");
            socket.close();
            return;
          }
          int row, column;
          try {
            row = Integer.parseInt(userInputParts[0]);
            column = Integer.parseInt(userInputParts[1]);
          } catch (Exception e) {
            Client.message = "Invalid input format. Please enter row and column numbers.";
            continue;
          }
          out.write(Client.Message.PLAY + " " + row + " " + column + Norms.END_OF_LINE);
          out.flush();

          String serverResponse = in.readLine();
          String[] serverResponseParts = serverResponse.split(" ", 3);
          Server.Message message = Server.Message.valueOf(serverResponseParts[0]);

          if (message == Server.Message.ERROR) {
            Client.message = "Invalid move! That cell is occupied or out of bounds. Try again.";
            continue;
          } else if (message == Server.Message.OK) {
            game.board[row * 3 + column] = playerSymbol;
            Client.myTurn = false;
          }

        } else {
          String serverResponse = in.readLine();
          String[] serverResponseParts = serverResponse.split(" ", 3);
          Server.Message message = Server.Message.valueOf(serverResponseParts[0]);
          switch (message) {
            case PLAY:
              int row = Integer.parseInt(serverResponseParts[1]);
              int column = Integer.parseInt(serverResponseParts[2]);

              game.board[row * 3 + column] = opponentSymbol;

              Client.message = "Opponent played at (" + row + ", " + column + ")";
              Client.myTurn = true;
              break;
            case GAME_OVER:
              String result = serverResponseParts[1];
              if (result.equals("1")) {
                Client.message = "You won the game!";
              } else if (result.equals("2")) {
                Client.message = "You lost the game.";
              } else if (result.equals("3")) {
                Client.message = "Opponent disconnected. You win by default!";
              } else {
                Client.message = "The game ended in a draw.";
              }
              drawGame(username, game, false); // Show final board
              System.out.println("\nPress Enter to return to lobby...");
              consoleReader.readLine();
              Client.inGame = false;
              return;
            default:
              Client.message = "An error occurred try again";
              break;
          }
        }
      } catch (Exception e) {
        Client.message = "An error occurred try again";
      }
  }
}
