package ch.heigvd.Server.Functions;

import ch.heigvd.Client.Client;
import ch.heigvd.Common.Norms;
import ch.heigvd.Common.Player;
import ch.heigvd.Server.Server;
import java.io.*;
import java.net.Socket;

/**
 * Class containing the login logic for players.
 *
 * The login system uses a simple text file named logins.txt to store pairs of
 * username and password.
 *
 * Note: For this project, we intentionally use a basic text file login system
 * because authentication is not the main objective here. In a real application,
 * a more advanced and secure login system would be required.
 *
 * General behavior:
 * - If the user exists and the password is correct: login is successful
 * - If the user exists but the password is incorrect: error
 * - If the user does not exist: the account is created
 * - If the user is already logged in: error
 */
public class Login {
    public static final String LOGIN_FILE = "logins.txt";

    /**
     * Handles a player's login request.
     *
     * The expected format from the client is:
     * LOGIN username password
     *
     * This method verifies if the user exists, checks the correctness of the
     * password, creates a new account if needed, and prevents multiple logins
     * with the same username.
     *
     * @param socket the client's socket
     * @param in the client's input stream
     * @param out the client's output stream
     * @return a Player object if login succeeds, null otherwise
     */
    public static Player login(Socket socket, BufferedReader in, BufferedWriter out) {

        // Ensure the login file exists
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
                    // Ignore unknown messages
                }

                // Only LOGIN messages are accepted here
                if (message != Client.Message.LOGIN) {
                    socket.close();
                    return null;
                }

                String username = clientResponseParts[1];
                String password = clientResponseParts[2];

                // Check if the username exists in logins.txt
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

                // Respond based on the login state
                if (found) {

                    if (passwordCorrect) {

                        // Check if user is already logged in
                        if (Server.players.stream().anyMatch(p -> p.username.equals(username))) {
                            out.write(Server.Message.ERROR + " 1" + Norms.END_OF_LINE);
                            out.flush();
                        } else {
                            Player player = new Player(username);
                            Server.players.add(player);
                            out.write(Server.Message.OK + Norms.END_OF_LINE);
                            out.flush();
                            loginsReader.close();
                            return player;
                        }

                    } else {
                        // Wrong password
                        out.write(Server.Message.ERROR + " 2" + Norms.END_OF_LINE);
                        out.flush();
                    }

                } else {
                    // Create a new user
                    BufferedWriter loginsWriter = new BufferedWriter(new FileWriter(loginsFile, true));
                    loginsWriter.write(username + " " + password + "\n");
                    loginsWriter.close();

                    Player player = new Player(username);
                    Server.players.add(player);

                    out.write(Server.Message.OK + Norms.END_OF_LINE);
                    out.flush();

                    loginsReader.close();
                    return player;
                }
            }

        } catch (IOException e) {
            System.out.println("[Server] exception: " + e);
        }

        return null;
    }
}
