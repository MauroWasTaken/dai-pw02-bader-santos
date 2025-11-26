package ch.heigvd.Server.Functions;

import ch.heigvd.Client.Client;
import ch.heigvd.Common.Player;
import ch.heigvd.Server.Server;
import java.io.*;
import java.net.Socket;

public class Login {
  public static final String LOGIN_FILE = "logins.txt";

  public static String login(Socket socket, BufferedReader in, BufferedWriter out) {
    // making sure the file exists
    File loginsFile = new File(LOGIN_FILE);
    try {
      if (loginsFile.createNewFile()) {
        System.out.println("[Server] Created new login file: " + LOGIN_FILE);
      }
    } catch (IOException e) {
      System.out.println("[Server] Could not create login file: " + e);
    }

    try {
      while (!socket.isClosed()) {
        String clientResponse = in.readLine();
        if (clientResponse == null) {
          socket.close();
          return null;
        }

        String[] clientResponseParts = clientResponse.split(" ", 3);

        Client.Message message = null;
        try {
          message = Client.Message.valueOf(clientResponseParts[0]);
        } catch (IllegalArgumentException e) {
          // Do nothing
        }
        if (message != Client.Message.LOGIN) {
          socket.close();
          return null;
        }
        String username = clientResponseParts[1];
        String password = clientResponseParts[2];

        // check if username is in logins.txt
        loginsFile = new File(LOGIN_FILE);
        BufferedReader loginsReader = new BufferedReader(new FileReader(loginsFile));
        String line;
        boolean found = false;
        boolean passwordCorrect = false;
        while ((line = loginsReader.readLine()) != null) {
          String[] loginParts = line.split(" ", 2);
          if (loginParts[0].equals(username)) {
            found = true;
            if (loginParts[1].equals(password)) {
              passwordCorrect = true;
            }
            break;
          }
        }
        // returns messages according to the situation
        if (found) {
          if (passwordCorrect) {
            if (Server.players.stream().anyMatch(p -> p.username.equals(username))) {
              out.write(Server.Message.ERROR + " 1" + Server.END_OF_LINE); // user already logged in
              out.flush();
            } else {
              Player player = new Player(username);
              Server.players.add(player);
              out.write(Server.Message.OK + Server.END_OF_LINE);
              out.flush();
              loginsReader.close();
              return username;
            }
          } else { // wrong password
            out.write(Server.Message.ERROR + " 2" + Server.END_OF_LINE);
            out.flush();
          }
        } else {
          // creates new user
          BufferedWriter loginsWriter = new BufferedWriter(new FileWriter(loginsFile, true));
          loginsWriter.write(username + " " + password + "\n");
          loginsWriter.close();
          Player player = new Player(username);
          Server.players.add(player);
          out.write(Server.Message.OK + Server.END_OF_LINE);
          out.flush();
          loginsReader.close();
          return username;
        }
      }
    } catch (IOException e) {
      System.out.println("[Server] exception: " + e);
    }
    return null;
  }
}
