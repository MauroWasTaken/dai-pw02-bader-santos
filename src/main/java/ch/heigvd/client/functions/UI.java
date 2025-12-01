package ch.heigvd.client.functions;

import ch.heigvd.client.Client;
import ch.heigvd.common.Game;
import ch.heigvd.common.Player;

/**
 * Simple console UI helpers used by the client to render lobby and game state.
 *
 * <p>All methods are static and operate on the shared Client state. This class intentionally
 * contains only minimal presentation logic for the terminal UI.
 */
public class UI {

  /** Append a short help message to the client message buffer (displayed on next render). */
  public static void help() {
    String help =
        "\n=== Help Menu ===\n"
            + "Here are the commands you can use in the lobby:\n\n"
            + "  HELP\n"
            + "      Show this help menu.\n\n"
            + "  PLAYERS\n"
            + "      Display the list of connected players.\n\n"
            + "  CHALLENGE <username>\n"
            + "      Send a challenge request to the specified player.\n"
            + "      Example: CHALLENGE alice\n\n"
            + "  ACCEPT <username>\n"
            + "      Accept a challenge received from the given player.\n"
            + "      Example: ACCEPT bob\n\n"
            + "  REFUSE <username>\n"
            + "      Refuse a challenge from the given player.\n"
            + "      Example: REFUSE charlie\n\n"
            + "  QUIT\n"
            + "      Disconnect from the server and exit the game.\n\n"
            + "====================================\n";

    Client.message += help;
  }

  /**
   * Render the lobby view for the provided username.
   *
   * @param username the username of the local player (displayed in the header)
   */
  public static void drawLobby(String username) {
    // Source - https://stackoverflow.com/a
    // Posted by Bhuvanesh Waran, modified by community. See post 'Timeline' for change history
    // Retrieved 2025-11-27, License - CC BY-SA 3.0
    System.out.print("\033\143");
    if (!Client.message.isEmpty()) System.out.print(Client.message + "\n\n");

    System.out.println("Welcome to the Lobby " + username + " !");
    Client.message = "";

    System.out.println("\n=== Connected Players ===");

    for (Player p : Client.players) {

      System.out.println(
          "- "
              + p.username
              + " | Wins: "
              + p.wins
              + " | Losses: "
              + p.losses
              + " | Draws: "
              + p.draws
              + " | WinStreak: "
              + p.winStreak);
    }

    if (!Client.challenges.isEmpty()) {
      System.out.println("\n== Challenges ==:");
      for (Player p : Client.challenges) {
        System.out.println(p.username);
      }
    }
    System.out.println("\n== Commands ==:");
    for (String option : Client.lobbyOptions) {
      System.out.println(option + " ");
    }
    System.out.print("Please enter a command: ");
  }

  /**
   * Render the game view showing the current board and prompt state.
   *
   * @param username the username of the local player (displayed in the header)
   * @param game the current game model containing the board
   * @param yourTurn whether it's the local player's turn
   */
  public static void drawGame(String username, Game game, boolean yourTurn) {
    System.out.print("\033\143");
    if (!Client.message.isEmpty()) System.out.print(Client.message + "\n\n");

    System.out.println("Welcome to the Game " + username + " !");
    Client.message = "";
    System.out.println("\n== Game State ==");
    printBoard(game.board);
    if (yourTurn) {
      System.out.println("It's your turn to play!");
      System.out.println("\n== Commands ==");
      System.out.println("<row> <col>");
      System.out.println("QUIT\n");
      System.out.print("Please enter a command: ");
    } else {
      System.out.println("Waiting for opponent to play...");
    }
  }

  /** Print the tic-tac-toe board to the console. */
  private static void printBoard(String[] gameState) {
    System.out.println("Current Board:");
    System.out.println("  0 1 2");
    for (int i = 0; i < 3; i++) {
      System.out.print(i + " ");
      for (int j = 0; j < 3; j++) {
        System.out.print(gameState[i * 3 + j]);
        if (j < 2) System.out.print("|");
      }
      System.out.println();
      if (i < 2) System.out.println("  -----");
    }
  }
}
