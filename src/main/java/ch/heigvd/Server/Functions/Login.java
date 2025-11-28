package ch.heigvd.Server.Functions;

import ch.heigvd.Client.Client;
import ch.heigvd.Common.Norms;
import ch.heigvd.Common.Player;
import ch.heigvd.Server.Server;
import java.io.*;
import java.net.Socket;

/**
 * Classe contenant la logique de login des joueurs.
 *
 * <p>Le login utilise un fichier texte appele logins.txt pour stocker les couples nom d utilisateur
 * mot de passe.
 *
 * <p>Note: pour ce projet, nous avons fais le choix que le login soit un simple fichier txt car le
 * login n'est pas la partie la plus importante à pratiquer ici. Bien évidemment il faudrai un login
 * plus poussé
 *
 * <p>Le fonctionnement general est le suivant : - si l utilisateur existe et que le mot de passe
 * est correct : connexion - si l utilisateur existe mais mauvais mot de passe : erreur - si l
 * utilisateur n existe pas : creation du compte - si l utilisateur est deja connecte : erreur
 */
public class Login {
  public static final String LOGIN_FILE = "logins.txt";

  public static Player login(Socket socket, BufferedReader in, BufferedWriter out) {
    // On vérifie que le fichier existe
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
          // On fait rien
        }
        if (message != Client.Message.LOGIN) {
          socket.close();
          return null;
        }
        String username = clientResponseParts[1];
        String password = clientResponseParts[2];

        // check si le username est dans logins.txt
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
        // returns un message en fonction de la situation
        if (found) {
          if (passwordCorrect) {
            if (Server.players.stream().anyMatch(p -> p.username.equals(username))) {
              out.write(Server.Message.ERROR + " 1" + Norms.END_OF_LINE); // user already logged in
              out.flush();
            } else {
              Player player = new Player(username);
              Server.players.add(player);
              out.write(Server.Message.OK + Norms.END_OF_LINE);
              out.flush();
              loginsReader.close();
              return player;
            }
          } else { // faux mot de passe
            out.write(Server.Message.ERROR + " 2" + Norms.END_OF_LINE);
            out.flush();
          }
        } else {
          // crée un nouveau user
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
