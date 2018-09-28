package ai;

import ai.Global;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently
 * it only makes a random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */
public class AIClient implements Runnable
{
    private int player;
    private JTextArea text;
    
    private PrintWriter out;
    private BufferedReader in;
    private Thread thr;
    private Socket socket;
    private boolean running;
    private boolean connected;
    
    // Constants for MiniMax
    static final int LOSS_BIAS = -1000;
    static final int WIN_BIAS = +1000;
    static final int TIME_LIMIT_MS = 5000;
    	
    /**
     * Creates a new client.
     */
    public AIClient()
    {
	player = -1;
        connected = false;
        
        //This is some necessary client stuff. You don't need
        //to change anything here.
        initGUI();
	
        try
        {
            addText("Connecting to localhost:" + KalahaMain.port);
            socket = new Socket("localhost", KalahaMain.port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            addText("Done");
            connected = true;
        }
        catch (Exception ex)
        {
            addText("Unable to connect to server");
            return;
        }
    }
    
    /**
     * Starts the client thread.
     */
    public void start()
    {
        //Don't change this
        if (connected)
        {
            thr = new Thread(this);
            thr.start();
        }
    }
    
    /**
     * Creates the GUI.
     */
    private void initGUI()
    {
        //Client GUI stuff. You don't need to change this.
        JFrame frame = new JFrame("My AI Client");
        frame.setLocation(Global.getClientXpos(), 445);
        frame.setSize(new Dimension(420,250));
        frame.getContentPane().setLayout(new FlowLayout());
        
        text = new JTextArea();
        JScrollPane pane = new JScrollPane(text);
        pane.setPreferredSize(new Dimension(400, 210));
        
        frame.getContentPane().add(pane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        frame.setVisible(true);
    }
    
    /**
     * Adds a text string to the GUI textarea.
     * 
     * @param txt The text to add
     */
    public void addText(String txt)
    {
        //Don't change this
        text.append(txt + "\n");
        text.setCaretPosition(text.getDocument().getLength());
    }
    
    /**
     * Thread for server communication. Checks when it is this
     * client's turn to make a move.
     */
    public void run()
    {
        String reply;
        running = true;
        
        try
        {
            while (running)
            {
                //Checks which player you are. No need to change this.
                if (player == -1)
                {
                    out.println(Commands.HELLO);
                    reply = in.readLine();

                    String tokens[] = reply.split(" ");
                    player = Integer.parseInt(tokens[1]);
                    
                    addText("I am player " + player);
                }
                
                //Check if game has ended. No need to change this.
                out.println(Commands.WINNER);
                reply = in.readLine();
                if(reply.equals("1") || reply.equals("2") )
                {
                    int w = Integer.parseInt(reply);
                    if (w == player)
                    {
                        addText("I won!");
                    }
                    else
                    {
                        addText("I lost...");
                    }
                    running = false;
                }
                if(reply.equals("0"))
                {
                    addText("Even game!");
                    running = false;
                }

                //Check if it is my turn. If so, do a move
                out.println(Commands.NEXT_PLAYER);
                reply = in.readLine();
                if (!reply.equals(Errors.GAME_NOT_FULL) && running)
                {
                    int nextPlayer = Integer.parseInt(reply);

                    if(nextPlayer == player)
                    {
                        out.println(Commands.BOARD);
                        String currentBoardStr = in.readLine();
                        boolean validMove = false;
                        while (!validMove)
                        {
                            long startT = System.currentTimeMillis();
                            //This is the call to the function for making a move.
                            //You only need to change the contents in the getMove()
                            //function.
                            GameState currentBoard = new GameState(currentBoardStr);
                            int cMove = getMove(currentBoard);
                            
                            //Timer stuff
                            long tot = System.currentTimeMillis() - startT;
                            double e = (double)tot / (double)1000;
                            
                            out.println(Commands.MOVE + " " + cMove + " " + player);
                            reply = in.readLine();
                            if (!reply.startsWith("ERROR"))
                            {
                                validMove = true;
                                addText("Made move " + cMove + " in " + e + " secs");
                            }
                        }
                    }
                }
                
                //Wait
                Thread.sleep(100);
            }
	}
        catch (Exception ex)
        {
            running = false;
        }
        
        try
        {
            socket.close();
            addText("Disconnected from server");
        }
        catch (Exception ex)
        {
            addText("Error closing connection: " + ex.getMessage());
        }
    }
    
    /**
     * This is the method that makes a move each time it is your turn.
     * Here you need to change the call to the random method to your
     * Minimax search.
     * 
     * @param currentBoard The current board state
     * @return Move to make (1-6)
     */
    public int getMove(GameState currentBoard)
    {
        int myMove = 0;

 	// Thomas AI vs Alex AI
        if (player==1)
            myMove =iddfsMiniMaxMove(currentBoard); // Thomas
        else
            myMove = getNextMoveAlex_v1(currentBoard); // Alex
 
        return myMove;
    }
    
    public int iddfsMiniMaxMove(GameState state)
    {
        int maxDepthIter = 0;
        int chosenMove = 1;
        int chosenMoveScoreDiff = 0;
        long deadline = System.currentTimeMillis() + TIME_LIMIT_MS;
        
        while(true) { // Iterate max-depth from 1, 2, 3, ..., N
            maxDepthIter++;
            
            // Evaluate choices through recursive miniMaxAlphaBeta
            int[] result = miniMaxAlphaBeta(state, maxDepthIter, 
                        Integer.MIN_VALUE, Integer.MAX_VALUE, true, deadline);
            
            int resultMove = result[0];
            int resultScore = result[1];
            int timeBreak = result[2];
            
            // If we encounter time-break in this max-depth iter, 
            // then we will use previous max-depth iteration results
            if (timeBreak == 1) {
                break;
            }
            
            chosenMove = resultMove;
            chosenMoveScoreDiff = resultScore;
            
            // Check for winning/losing to avoid max-depth iteration spam
            if (chosenMoveScoreDiff > WIN_BIAS) {
                break;
            }
            
            // Overflow condition
            if (maxDepthIter == Integer.MAX_VALUE) {
                break;
            }
        }
        
        addText("P" + this.player + "> MOVE: " + chosenMove + ", IDDFS DEPTH: " 
                + maxDepthIter + ", SCORE DIFF: " + chosenMoveScoreDiff);
        return chosenMove;
    }
    
    public int[] miniMaxAlphaBeta(GameState state, int remainingDepth, int alpha, int beta, boolean isMax, long deadline)
    {
        long currentTime = System.currentTimeMillis();
        
        // Initialize return result
        // For Min/Max node:
        // [0]: Chosen move for Min/Max
        // [1]: Score for chosen move
        // [2]: Flag for time break (0 - no time break happened, 1 - time break happened)
        // For Leaf node (nodes at max depth, e.g. remainingDepth == 0):
        // [0]: -1
        // [1]: Game score difference
        // [2]: Flag for time break (0 - no time break happened, 1 - time break happened)
        int[] result = new int[3];
        result[0] = -1;
        result[1] = 0;
        result[2] = 0;
        
        // Time-break condition
        if (currentTime > deadline) {
            result[2] = 1; // Turn on time-break bit
            return result;
        }
        
        // End-game condition
        if (state.gameEnded()) {
            int endScoreDiff = evalGameScore(state);
            
            if (endScoreDiff < 0) { // AI looses (bias away from this)
                result[0] = -1;
                result[1] = endScoreDiff + LOSS_BIAS;
                return result;
            } else if (endScoreDiff > 0) { // AI wins (bias towards this)
                result[0] = -1;
                result[1] = endScoreDiff + WIN_BIAS;
                return result;
            } else { // Game draw (no bias added), diff always 0
                return result;
            }
        }
        
        // Remaining depth condition (we reached end of depth, so this is a leaf node)
        // Leaf nodes at the bottom simply return their score, which will be propogated upwards
        if (remainingDepth == 0) {
            result[0] = -1;
            result[1] = evalGameScore(state);
            return result;
        }
        
        // Current score & Current move
        int currentMove = -1;
        int currentScore = 0;
        
        if (isMax) {
            currentScore = Integer.MIN_VALUE;
        } else {
            currentScore = Integer.MAX_VALUE;
        }

        // Begin DFS loop for visiting next move nodes
        for (int i = 1; i < 7; i++) {
            // Skip child node if move to it isn't legal
            if (!state.moveIsPossible(i)) {
                continue;
            }
            
            GameState copiedState = state.clone();
            copiedState.makeMove(i);
            int[] subResult = miniMaxAlphaBeta(copiedState, remainingDepth - 1, 
                    alpha, beta, !isMax, deadline);
            int score = subResult[1];
            
            // === ALPHA-BETA LOGIC ===
            // Branch dropping pattern of Alpha-Beta is fairly simple:
            //   Max nodes need to satisfy (score <= beta) equation
            //   Min nodes need to satisfy (score >= alpha) equation
            //   If they don't, they will stop further iteration and return
            // = A/B MAXIMIZER PART =
            if (isMax) {
                // Parent node is a Minimizer and if it this Maximizer's score
                // is higher than parent node's beta, then there is no point
                // evaluating this branch further
                if (score > beta) {
                    result[0] = i;
                    result[1] = score;
                    return result;
                }
                
                alpha = Math.max(alpha, score);
                
                if (currentScore < score) {
                    currentScore = score;
                    currentMove = i;
                }                
            }
            
            // = A/B MINIMIZER PART =
            if (!isMax) {
                // Parent node is a Maximizer and if this Minimizer's score
                // is less than parent node's alpha, then there is no point
                // evaluating this branch further
                if (score < alpha) {
                    result[0] = i;
                    result[1] = score;
                    return result;
                }
                
                // Update current values
                beta = Math.min(beta, score);
                
                if (currentScore > score) {
                    currentScore = score;
                    currentMove = i;
                }
            }
        }
        
        result[0] = currentMove;
        result[1] = currentScore;
        return result;
    }
    
    /**
     * Evaluate game score difference between AI and Opponent
     * @param state Game state
     * @return score difference
     */
    private int evalGameScore(GameState state)
    {
        if (this.player == 1) {
            return state.getScore(1) - state.getScore(2);
        } else {
            return state.getScore(2) - state.getScore(1);
        }
    }
    
    /**
     * Returns a random ambo number (1-6) used when making
     * a random move.
     * 
     * @return Random ambo number
     */
    public int getRandom()
    {
        return 1 + (int)(Math.random() * 6);
    }

   //***********************************************************************
    // Alex part
    static final int MAX_LEVEL = 5;
    
    int cL=0; // current Depth
    int lastBestMovie=0;
    int lastScoreDiff=0;
    
    public boolean getRandomBoolean()
    {
        if ( Math.random() > 0.5)
            return true;
        else
            return false;
    }
    
    
    private int ABprune(GameState state, int alpha, int betta)
    {
        int m=0, t=0;
        
        cL+=1;
        if (cL>MAX_LEVEL) // stop at this level
        {
            cL-=1; // level down
            lastScoreDiff = evalGameScore(state);;
            return lastScoreDiff;
        }
        
        // End-game condition
        if (state.gameEnded()) {
            int endScoreDiff = evalGameScore(state);
            lastScoreDiff = endScoreDiff;
           
            return endScoreDiff;
        }
       
        m = alpha;        
        
        // canonic F2 from 
        for (int i = 1; i < 7; i++) 
        {
             // Skip child node if move to it isn't legal
            if (!state.moveIsPossible(i)) {
                continue;
            }
            if (cL==0)
                lastBestMovie=i;
            
            GameState copiedState = state.clone();
            copiedState.makeMove(i);
            
            
            // this is the same like ABprune(copiedState, betta, m); 
            t = -ABprune(copiedState, -betta, -m); 
            if (t > m)
                m = t;     
            
            if (m >= betta) break;
            
        }
        cL-=1;    // level UP
        return m; //
    }
    
    /*
    MiniMax with ABpruning
    
    */
    int cnt=0;
    private int getNextMoveAlex_v1(GameState state)
    {
        int m=-200, t=0;
        cnt++;
        addText("getNextMoveAlex_v1");
                 
         cL=-1; // -1 level
         t = -ABprune(state, -200, 200);
          
          addText("P" + this.player + "> MOVE: " + lastBestMovie + ", IDDFS DEPTH: " 
                + cL + ", SCORE DIFF: " + lastScoreDiff + " step:" + cnt);
       
        return lastBestMovie;
    }
    
 

}