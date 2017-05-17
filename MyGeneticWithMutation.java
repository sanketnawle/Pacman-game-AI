package pacman.entries;

import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import pacman.game.Constants;
import pacman.game.Game;

import java.util.Random;

import static pacman.game.Constants.DELAY;

/**
 * Created by Sanket N on 23-10-2016.
 */
public class MyGeneticWithMutation extends Controller<Constants.MOVE> {
    private static final int MIN_DISTANCE = 10;
    private static final int MIN_POWERPILL_DISTANCE = 20;
    private static final int MIN_EDIBLE_DISTANCE = 42;
    private static final int population_size = 10;
    private static final int action_set = 6;


    public Constants.MOVE getMove(Game game, long timeDue)
    {
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

        //select the best move to be made from the available ones by sending each move to the genetic function and their evaluating best scores

        Constants.MOVE bestMove = null;
        int best = 0;
        Constants.MOVE[] MoveArray = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());

        for (Constants.MOVE move : MoveArray)
        {
            copy = game.copy();
            copy.advanceGame(move, new StarterGhosts().getMove(copy, System.currentTimeMillis() + DELAY));

            int value = genetic(copy);

            if (value > best)
            {
                best = value;
                bestMove = move;
            }
        }

        return bestMove;
    }


    int genetic(Game g)
    {

        Game copy = g.copy();
        int best_score=0,score;
        int best[] = new int[population_size];

        //initialize best scores array
        for(int j=0;j<population_size;j++)
            best[j]=0;

        Constants.MOVE[] pop[]= new Constants.MOVE[population_size][action_set];

        //generate random population based on the next possible moves
        for(int j=0;j<population_size;j++)
        {
            for(int k=0;k<action_set;k++)
            {
                Random r = new Random();
                Constants.MOVE[] population_MoveArray = copy.getPossibleMoves(copy.getPacmanCurrentNodeIndex(), copy.getPacmanLastMoveMade());
               if(population_MoveArray!=null)
               {
                   Constants.MOVE random_move = population_MoveArray[r.nextInt(population_MoveArray.length)];
                   pop[j][k] = random_move;
                   copy.advanceGame(random_move, new StarterGhosts().getMove(copy, System.currentTimeMillis() + DELAY));
               }
               else break;
            }
        }

        //evaluating the individuals & their scores and finding out the best one
        for(int j=0;j<population_size;j++)
        {
            for(int k=0;k<pop[j].length;k++)
            {
                copy.advanceGame(pop[j][k],new StarterGhosts().getMove(copy, System.currentTimeMillis() + DELAY));
                score = copy.getScore();

                if(!copy.wasPacManEaten())
                    if (best[j] <= score)
                        best[j] = score;
            }
        }


        int temp;
        Constants.MOVE temp_move[]=null;

        //sorting the individuals according to their scores in descending order
        for(int j=0;j<population_size-1;j++) {
            for (int k = 0; k < pop[j].length-j-1; k++)
            {
                if(best[j]<best[j+1])
                {
                    temp= best[j];
                    best[j]= best[j+1];
                    best[j+1]= temp;

                    temp_move = pop[j];
                    pop[j]= pop[j+1];
                    pop[j+1]=temp_move;

                }
            }
        }

        Game mutation_copy;
        int mutation;

        //mutating the worst half of the population using uniform mutation
        for(int j= population_size/2 ;j<population_size;j++)
        {
            mutation_copy = g.copy();

            for(int k=0;k<pop[j].length;k++)
            {
                mutation_copy.advanceGame(pop[j][k],new StarterGhosts().getMove(mutation_copy, System.currentTimeMillis() + DELAY));
                Constants.MOVE[] mutation_movearray = mutation_copy.getPossibleMoves(mutation_copy.getPacmanCurrentNodeIndex(),mutation_copy.getPacmanLastMoveMade());

                Random r = new Random();
                mutation = r.nextInt(1);

                if(mutation==1)
                {
                    Constants.MOVE random_move = mutation_movearray[r.nextInt(mutation_movearray.length)];
                    pop[j][k] = random_move;
                    mutation_copy.advanceGame(random_move, new StarterGhosts().getMove(mutation_copy, System.currentTimeMillis() + DELAY));
                }
            }
        }

        //sorting the best score array again
        for(int j=0;j<population_size;j++)
        {
            for(int k=0;k<pop[j].length;k++)
            {
                copy.advanceGame(pop[j][k],new StarterGhosts().getMove(copy, System.currentTimeMillis() + DELAY));
                score = copy.getScore();

                if(!copy.wasPacManEaten())
                    if (best[j] <= score)
                        best[j] = score;
            }
        }

        //finding out the best score
        for(int j=0;j<population_size-1;j++)
            if(best[j+1]>=best[j])
                best_score= best[j+1];

        return best_score;

    }

}
