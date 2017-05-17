package pacman.entries;

import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import static pacman.game.Constants.DELAY;

import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;

/**
 * Created by Sanket N on 18-10-2016.
 */

public class MyDFS extends Controller<MOVE> {

    private static final int MIN_DISTANCE = 10;
    private static final int MIN_POWERPILL_DISTANCE = 10;
    private static final int MIN_EDIBLE_DISTANCE = 40;
    private final int alpha = Integer.MAX_VALUE;
    //static int i;

    public MOVE getMove(Game game, long timeDue) {

        Game copy;


        //If any non-edible ghost is too close (less than MIN_DISTANCE), run away
        for(Constants.GHOST ghost : Constants.GHOST.values())
            if(game.getGhostEdibleTime(ghost)==0 && game.getGhostLairTime(ghost)==0)
                if(game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(ghost))<MIN_DISTANCE)
                    return game.getNextMoveAwayFromTarget(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(ghost),Constants.DM.EUCLID);




        //Find the nearest edible ghost and go after them
        int minDistance=Integer.MAX_VALUE;
        Constants.GHOST minGhost=null;

        for(Constants.GHOST ghost : Constants.GHOST.values())
            if(game.getGhostEdibleTime(ghost)>0)
            {
                int distance=game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(ghost));

                if(distance<minDistance)
                {
                    minDistance=distance;
                    minGhost=ghost;
                }
            }

        if(minGhost!=null &&minDistance< MIN_EDIBLE_DISTANCE)	//we found an edible ghost
            return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(minGhost), Constants.DM.PATH);

        //search for the power pills and run towards them if they are close enough
        int[] powerPills=game.getPowerPillIndices();
        double pill_length;

        for(int i=0; i< powerPills.length; i++)
        {
            if(game.isPowerPillStillAvailable(i))
            {
                pill_length = game.getDistance(game.getPacmanCurrentNodeIndex(), powerPills[i], Constants.DM.PATH);


                if (pill_length < MIN_POWERPILL_DISTANCE )
                    return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), powerPills[i], Constants.DM.PATH);
            }

        }



        //DFS working
        MOVE bestMove = null;
        int best = 0;

        MOVE[] MoveArray = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());

        for (MOVE move : MoveArray) {
                copy = game.copy();

                copy.advanceGame(move, new StarterGhosts().getMove(copy, System.currentTimeMillis() + DELAY));

                int value = dfsRecursive(copy, 0);

                if (value > best) {
                    best = value;
                    bestMove = move;
                }
        }

        return bestMove;
    }


    int dfsRecursive(Game state,int i)
    {
        i++;
        int best = 0;
        Game copy;

        MOVE[] MoveArray = state.getPossibleMoves(state.getPacmanCurrentNodeIndex(), state.getPacmanLastMoveMade());

        for (MOVE move : MoveArray) {
            copy = state.copy();
            copy.advanceGame(move, new StarterGhosts().getMove(copy, System.currentTimeMillis() + DELAY));

            int value = copy.getScore();
            if (copy.wasPacManEaten() || i == alpha)
            {
                if (value > best)
                {
                    best = value;
                    --i;
                }
            }
            else
            {
                return dfsRecursive(copy, i);
            }
        }
        return best;

    }

}
