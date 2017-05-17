package pacman.entries;

import java.util.Comparator;
import java.util.PriorityQueue;

import pacman.controllers.Controller;
import pacman.controllers.examples.StarterGhosts;
import static pacman.game.Constants.DELAY;
import static pacman.game.Constants.LEVEL_LIMIT;

import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/**
 * Created by Sanket N on 23-10-2016.
 */

public class MyAStar extends Controller<MOVE>{

    private static class Node {
        MOVE default_move;
        Game state;
        int ply;
        double old_heuristic;
        int old_value;
        public Node(MOVE m, Game g, int i, double heuristic, int value) {
            default_move = m;
            state = g;
            ply = i;
            old_heuristic = heuristic;
            old_value = value;
        }

        public double evaluate_heuristic()
        {
            return (state.getNumberOfPills() - state.getNumberOfActivePills());
        }
    }

    private class HeuristicEvaluator <H> implements Comparator<H>
    {
        @Override
        public int compare(H h1, H h2)
        {
            Node a = (Node) h1;
            Node b = (Node) h2;

            if (a.state.getPly() > b.state.getPly())
                 return -1;
            else if (a.state.getPly() < b.state.getPly())
                 return 1;

            // same ply
            if (a.evaluate_heuristic() > b.evaluate_heuristic())
                 return -1;
            else return 1;

        }
    }

    private static final int MIN_DISTANCE = 10;
    private static final int MIN_POWERPILL_DISTANCE = 20;
    private static final int MIN_EDIBLE_DISTANCE = 30;

    public MOVE getMove(Game game,long timeDue)
    {
        Comparator comparator = new HeuristicEvaluator();
        PriorityQueue<Node> AStarQueue = new PriorityQueue<>(100,comparator);
        MOVE[] MoveArray = game.getPossibleMoves(game.getPacmanCurrentNodeIndex(), game.getPacmanLastMoveMade());


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

        //A star implementation

        for(MOVE move :MoveArray)
        {
            Game copy_game = game.copy();
            copy_game.advanceGame(move, new StarterGhosts().getMove(copy_game.copy(), System.currentTimeMillis()+DELAY));
            AStarQueue.offer(new Node(move, copy_game, 1, 1, copy_game.getNumberOfActivePills()));
        }

        while(AStarQueue.size() > 1)
        {
            Node current = AStarQueue.remove();

            MOVE[] options = current.state.getPossibleMoves(current.state.getPacmanCurrentNodeIndex(), current.state.getPacmanLastMoveMade());


            for(MOVE move:options)
            {
                boolean dead = false;
                Game current_state = current.state.copy();
                current_state.advanceGame(move, new StarterGhosts().getMove(current_state.copy(), System.currentTimeMillis()+DELAY));

                while(current_state.getPossibleMoves(current_state.getPacmanCurrentNodeIndex(), current_state.getPacmanLastMoveMade()).length > 1 && !dead)
                {
                    current_state.advanceGame(move, new StarterGhosts().getMove(current_state.copy(), System.currentTimeMillis()+DELAY));
                    if(current_state.wasPacManEaten())
                        dead = true;
                }

                while(current_state.getPossibleMoves(current_state.getPacmanCurrentNodeIndex(), current_state.getPacmanLastMoveMade()).length == 1 && !dead)
                {
                    MOVE advance_move = current_state.getPossibleMoves(current_state.getPacmanCurrentNodeIndex(), current_state.getPacmanLastMoveMade())[0];
                    current_state.advanceGame(advance_move, new StarterGhosts().getMove(current_state.copy(), System.currentTimeMillis()+DELAY));
                    if(current_state.wasPacManEaten())
                        dead = true;
                }

                if(!dead && (current_state.getNumberOfActivePills() == 0 || current_state.getCurrentLevelTime() >= LEVEL_LIMIT) || current_state.getPly() != game.getPly())
                    return current.default_move;

                else if(current_state.getPly() == game.getPly() && !dead)
                {
                    Node new_node = new Node(current.default_move, current_state, current.ply + 1, current.evaluate_heuristic(), current.state.getNumberOfActivePills());
                    AStarQueue.add(new_node);
                }
            }
        }

        return AStarQueue.remove().default_move;
    }
}
