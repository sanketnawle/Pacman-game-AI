package pacman.entries;

import java.util.concurrent.LinkedBlockingQueue;
import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import static pacman.game.Constants.DELAY;
import static pacman.game.Constants.LEVEL_LIMIT;

import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Created by Sanket N on 20-10-2016.
 */


public class MyBFS extends Controller<MOVE>{

    /*Unlike DFS, we cannot use recursion for BFS. We have to store the game current_state since we will have to visit it again. Thus, a data structure
    called Node is used which maintains the current_state of that particular node */

    private static class Node
    {
        MOVE default_move;
        Game state;
        int ply;
        public Node(MOVE m, Game g, int i)
        {
            default_move= m;
            state = g;
            ply= i;
        }
    }

    private static final int MIN_DISTANCE=10;

    public MOVE getMove(Game game,long timeDue)
    {
        int score = game.getScore();
        MOVE bfs_move = null;

        LinkedBlockingQueue<Node> bQueue = new LinkedBlockingQueue<Node>();
        MOVE[] MoveArray = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());
        for(MOVE moves :MoveArray)
        {
            Game copy_game = game.copy();
            copy_game.advanceGame(moves, new StarterGhosts().getMove(copy_game.copy(), System.currentTimeMillis()+DELAY));
            bQueue.offer(new Node(moves, copy_game, 0));
        }

        int current_node=game.getPacmanCurrentNodeIndex();

        //If any non-edible ghost is too close (less than MIN_DISTANCE), run away
        for(Constants.GHOST ghost : Constants.GHOST.values())
            if(game.getGhostEdibleTime(ghost)==0 && game.getGhostLairTime(ghost)==0)
                if(game.getShortestPathDistance(current_node,game.getGhostCurrentNodeIndex(ghost))<MIN_DISTANCE)
                    return game.getNextMoveAwayFromTarget(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(ghost), Constants.DM.PATH);


        //Find the nearest edible ghost and go after them
        int minDistance=Integer.MAX_VALUE;
        Constants.GHOST minGhost=null;

        for(Constants.GHOST ghost : Constants.GHOST.values())
            if(game.getGhostEdibleTime(ghost)>0)
            {
                int distance = game.getShortestPathDistance(current_node,game.getGhostCurrentNodeIndex(ghost));

                if(distance<minDistance)
                {
                    minDistance=distance;
                    minGhost=ghost;
                }
            }

        if(minGhost!=null && minDistance<30)	//we found an edible ghost
            return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(minGhost), Constants.DM.PATH);

        //search for the power pills and run towards them if they are close enough
        int[] powerPills=game.getPowerPillIndices();
        double pill_length;

        for(int i=0; i< powerPills.length; i++)
        {
            if(game.isPowerPillStillAvailable(i))
            {
                pill_length = game.getDistance(game.getPacmanCurrentNodeIndex(), powerPills[i], Constants.DM.PATH);

                if (pill_length < 30 )
                    return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), powerPills[i], Constants.DM.PATH);
            }
        }


        //BFS implementation

        while(bQueue.size() > 1 && ((System.currentTimeMillis() < timeDue - 5)))
        {
            Node head = bQueue.remove();
            MOVE[] choice = head.state.getPossibleMoves(head.state.getPacmanCurrentNodeIndex(), head.state.getPacmanLastMoveMade());

            for(int i=0;i<choice.length;i++)
            {
                boolean dead = false;
                Game current_state = head.state.copy();
                current_state.advanceGame(choice[i], new StarterGhosts().getMove(current_state.copy(), System.currentTimeMillis()+DELAY));

                while(current_state.getPossibleMoves(current_state.getPacmanCurrentNodeIndex(), current_state.getPacmanLastMoveMade()).length > 1 && !dead)
                {
                    current_state.advanceGame(choice[i], new StarterGhosts().getMove(current_state.copy(), System.currentTimeMillis()+DELAY));
                    if(current_state.wasPacManEaten())
                        dead = true;
                }

                while(current_state.getPossibleMoves(current_state.getPacmanCurrentNodeIndex(), current_state.getPacmanLastMoveMade()).length == 1 && !dead)
                {
                    MOVE advance_move = current_state.getPossibleMoves(current_state.getPacmanCurrentNodeIndex(), current_state.getPacmanLastMoveMade())[0];
                    current_state.advanceGame(advance_move , new StarterGhosts().getMove(current_state.copy(), System.currentTimeMillis()+DELAY));

                    if(current_state.wasPacManEaten())
                        dead = true;
                }

                if(!dead)
                {
                    if(current_state.getScore()>score)
                    {
                        score = current_state.getNumberOfActivePills();
                        bfs_move = head.default_move;
                    }

                    bQueue.add(new Node(head.default_move, current_state, head.ply + 1));
                }
            }
        }

        if(bfs_move == null)
             return bQueue.remove().default_move;
        else return bfs_move;

    }
}
