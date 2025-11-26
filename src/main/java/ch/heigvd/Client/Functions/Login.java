package ch.heigvd.Client.Functions;

import ch.heigvd.Client.Client;
import ch.heigvd.Server.Server;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Login {
  public static String login(Socket socket, BufferedReader in, BufferedWriter out) {
    final String ERROR_MESSAGE = "Something went wrong please try again";
    String username;
    String request;
    while (!socket.isClosed()) {
      try {
        // getting username and password from user
        System.out.print("Username: ");
        BufferedReader bir =
            new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        username = bir.readLine();
        System.out.print("Password: ");
        String password = bir.readLine();
        // send login info to server
        request = Client.Message.LOGIN + " " + username + " " + password + Client.END_OF_LINE;
        out.write(request);
        out.flush();
        // read server response
        String serverResponse = in.readLine();
        if (serverResponse == null) {
          System.out.println(ERROR_MESSAGE);
          socket.close();
          continue;
        }

        String[] serverResponseParts = serverResponse.split(" ", 2);
        Server.Message message = null;
        try {
          message = Server.Message.valueOf(serverResponseParts[0]);
        } catch (IllegalArgumentException e) {
          System.out.println(ERROR_MESSAGE);
          continue;
        }

        switch (message) {
          case OK:
            System.out.println("Login successful. Welcome " + username + "!");
            return username;
          case ERROR:
            int errorCode = Integer.parseInt(serverResponseParts[1]);
            switch (errorCode) {
              case 1:
                System.out.println("Error: User already logged in. Please try again.");
                break;
              case 2:
                System.out.println("Error: Invalid username or password. Please try again.");
                break;
              default:
                System.out.println(ERROR_MESSAGE);
                break;
            }
            break;
          default:
            System.out.println(ERROR_MESSAGE);
            break;
        }

      } catch (IOException e) {
        System.out.println("\n" + ERROR_MESSAGE);
      }
    }
    return null;
  }
}
