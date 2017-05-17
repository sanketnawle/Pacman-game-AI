
/*The implementation of stochastic hill climber is essentially similar to the implementation of DFS; the difference being
  that instead of always opening the first member of the stack, it picks one randomly and then checks if it is safe to move forward.
  Hence, I have kept most of the code similar to that of DFS and just added randomness while choosing the moves
*/


package pacman.entries;

import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import static pacman.game.Constants.DELAY;

import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.Random;

/**
 * Created by Sanket N on 18-10-2016.
 */

public class MyStochasticHillClimber extends Controller<MOVE> {

    private static final int MIN_DISTANCE = 30;
    private static final int MIN_POWERPILL_DISTANCE = 20;
    private static final int MIN_EDIBLE_DISTANCE = 30;
    private final int alpha = 10;
    //static int i;

    public MOVE getMove(Game game, long timeDue) {

        Game copy;


        //If any non-edible ghost is too close (less than MIN_DISTANCE), run away
        for(Constants.GHOST ghost : Constants.GHOST.values())
            if(game.getGhostEdibleTime(ghost)==0 && game.getGhostLairTime(ghost)==0)
                if(game.getShortestPathDistance(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(ghost))<MIN_DISTANCE)
                    return game.getNextMoveAwayFromTarget(game.getPacmanCurrentNodeIndex(),game.getGhostCurrentNodeIndex(ghost), Constants.DM.PATH);


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

            int value = hillClimber(copy,0);

            if (value > best) {
                best = value;
                bestMove = move;
            }
        }

        return bestMove;
    }


    int hillClimber(Game state, int i)
    {
        i++;
        int best = 0;
        Game copy;

        MOVE[] MoveArray = state.getPossibleMoves(state.getPacmanCurrentNodeIndex(), state.getPacmanLastMoveMade());
        Random r = new Random();

        MOVE random_move = MoveArray[r.nextInt(MoveArray.length)];

        copy = state.copy();
        copy.advanceGame(random_move, new StarterGhosts().getMove(copy, System.currentTimeMillis() + DELAY));
        int value = copy.getScore();

        if (copy.wasPacManEaten() || i>alpha )
        {
            if (value > best) {
                best = value;
                --i;
            }
        }

        else
        {
            return hillClimber(copy,i);
        }

        return best;

    }

}


