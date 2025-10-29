# **DAI 2025 - project 02 - Bader Gabriel Santos Mauro**

Our tic tac toe game is a Java-based command-line application designed to allow users to play against each other online. The project will have a client-server structure, where different clients connect to a server and then are able to match each other.

The key features for this project would be:

- Client can join a server with a username (and a password if he wants to keep his stats)
- Client is put into a lobby where he can see the list of online clients and their status (refreshes every x seconds).
- From the lobby, the client is able to challenge another client to a match
    - Client is able to accept or refuse the challenge
    
- In game
    - The server will choose a client to play first at random
    - do until someone wins or board is full
        - The client whos turn it is will be able to play
        - When the client plays the server gives the turn to the other player
    - The players’ stats are updated
    - Both players go back to the lobby
- Score keeping (wins / draws / losses / winstreak)
- An average looking game interface for the client

## Arguments

| **mode** | **argument** | **description** |
| --- | --- | --- |
| Server | server -p <port> -t <nbplayers> | starts application in server mode |
| Client | client -p <port> | start application in client mode and connects to server in <port> |

the defaults are :
- 42069: for the port
- 12: for the nbplayers