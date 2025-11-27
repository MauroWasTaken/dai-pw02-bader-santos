package ch.heigvd.Client.Functions;

import ch.heigvd.Client.Client;
import ch.heigvd.Common.Player;

public class UI {

  public static void help() {
    String help =
        "Usage:\n"
            + "  "
            + Client.Message.GUESS
            + " <number> - Submit the number you want to guess.\n"
            + "  "
            + Client.Message.RESTART
            + " - Restart the game.\n"
            + "  "
            + Client.Message.QUIT
            + " - Close the connection to the server.\n"
            + "  "
            + Client.Message.HELP
            + " - Display this help message.\n";
    Client.message += help;
  }

  public static void drawLobby(String username) {
    // Source - https://stackoverflow.com/a
    // Posted by Bhuvanesh Waran, modified by community. See post 'Timeline' for change history
    // Retrieved 2025-11-27, License - CC BY-SA 3.0
    System.out.print("\033\143");
    if (!Client.message.isEmpty()) System.out.print(Client.message + "\n\n");

    System.out.println("Welcome to the Lobby " + username + " !");
    Client.message = "";
    System.out.println("=== Lobby ===");
    for (Player p : Client.players) {
      System.out.println(
          p.username
              + " - Wins: "
              + p.wins
              + " Losses: "
              + p.losses
              + " Draws: "
              + p.draws
              + " Win Streak: "
              + p.winStreak
              + " Status: "
              + p.status);
    }
    if (!Client.challenges.isEmpty()) {
      System.out.println("== Challenges ==:");
      for (Player p : Client.challenges) {
        System.out.println(p.username);
      }
    }
    System.out.println("== Commands ==:");
    for (String option : Client.lobbyOptions) {
      System.out.println(option + " ");
    }
    System.out.print("Please enter a command: ");
  }
}
