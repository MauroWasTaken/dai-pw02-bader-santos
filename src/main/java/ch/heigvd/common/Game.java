package ch.heigvd.common;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple model of a Tic-Tac-Toe game used by the server and client.
 *
 * <p>The Game object stores references to the two participating players, the board state and
 * concurrency-safe flags used by the server to coordinate turns and game termination.
 */
public class Game {
  public String[] board = new String[9];
  public Player player1;
  public Player player2;

  /** True when it is player1's turn. */
  public AtomicBoolean isPlayer1Turn = new AtomicBoolean(true);

  /** True when the game has finished (winner set or draw). */
  public AtomicBoolean isOver = new AtomicBoolean(false);

  /** Winning player instance when isOver is true and the game didn't draw; null for draws. */
  public Player winner = null;

  /** Preferred symbol for player1. */
  public final String player1Symbol = "X";

  /** Preferred symbol for player2. */
  public final String player2Symbol = "O";

  /** Index of the last move played (-1 if none). */
  public AtomicInteger lastMove = new AtomicInteger(-1);

  /** True if a player disconnected and the other player should be credited with the win. */
  public AtomicBoolean hasDisconnect = new AtomicBoolean(false);

  /** Create an empty game with an empty board. Players must be set later. */
  public Game() {
    for (int i = 0; i < 9; i++) {
      board[i] = " ";
    }
  }

  /**
   * Create a game with two players. The starting player is chosen randomly.
   *
   * @param player1 first player (will use the X symbol)
   * @param player2 second player (will use the O symbol)
   */
  public Game(Player player1, Player player2) {
    this.player1 = player1;
    this.player2 = player2;
    this.isPlayer1Turn.set(Math.random() < 0.5);
    for (int i = 0; i < 9; i++) {
      board[i] = " ";
    }
  }

  /**
   * Attempt to make a move on behalf of the player identified by username.
   *
   * @param position board position (0..8)
   * @param username username of the player making the move
   * @return true if the move has been accepted and applied, false otherwise
   */
  public boolean makeMove(int position, String username) {
    Player player = username.equals(player1.username) ? player1 : player2;
    if (isOver.get() || position < 0 || position > 8 || !board[position].equals(" ")) {
      return false;
    }
    if ((isPlayer1Turn.get() && player.equals(player1))
        || (!isPlayer1Turn.get() && player.equals(player2))) {
      board[position] = isPlayer1Turn.get() ? player1Symbol : player2Symbol;
      checkWin();
      lastMove.set(position);
      isPlayer1Turn.set(!isPlayer1Turn.get());
      return true;
    }
    return false;
  }

  /** Check current board for a winning condition or a draw and update isOver/winner accordingly. */
  private void checkWin() {
    String[][] winConditions = {
      {board[0], board[1], board[2]},
      {board[3], board[4], board[5]},
      {board[6], board[7], board[8]},
      {board[0], board[3], board[6]},
      {board[1], board[4], board[7]},
      {board[2], board[5], board[8]},
      {board[0], board[4], board[8]},
      {board[2], board[4], board[6]}
    };
    for (String[] condition : winConditions) {
      if (condition[0].equals(player1Symbol)
          && condition[1].equals(player1Symbol)
          && condition[2].equals(player1Symbol)) {
        isOver.set(true);
        winner = player1;
        return;
      }
      if (condition[0].equals(player2Symbol)
          && condition[1].equals(player2Symbol)
          && condition[2].equals(player2Symbol)) {
        isOver.set(true);
        winner = player2;
        return;
      }
    }
    if (isBoardFull()) {
      isOver.set(true);
    }
  }

  /**
   * Returns whether the board is full (no empty cells left).
   *
   * @return true when no cell equals " "
   */
  private boolean isBoardFull() {
    for (String cell : board) {
      if (cell.equals(" ")) {
        return false;
      }
    }
    return true;
  }
}
