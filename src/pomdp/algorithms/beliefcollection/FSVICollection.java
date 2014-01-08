package pomdp.algorithms.beliefcollection;

import java.util.Collections;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;

/* based off optimal MDP solution */
public class FSVICollection extends BeliefCollection {

	int depth = 0;
	final int defaultMaxDepth = 200;
	double PICK_MDP_ACTION  = 0.9;
	Vector<BeliefState> exploredBeliefs;
		
	
	public FSVICollection(ValueIteration vi, boolean bAllowDuplicates)
	{	
		super(vi, bAllowDuplicates);	
	}
	
	/* no initial belief */
	public Vector<BeliefState> initialBelief()
	{
		/* initialize the MDP Heuristic*/
		POMDP.getMDPValueFunction().valueIteration(1000,ExecutionProperties.getEpsilon());
		
		return new Vector<BeliefState>();
	}
	
	public Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> vBeliefPoints)
	{

		exploredBeliefs = new Vector<BeliefState>();
		
		BeliefState bsInitial = POMDP.getBeliefStateFactory().getInitialBeliefState();
		
		do{
			/* pick an initial starting state */
			int iInitialState = -1;
			do{
				iInitialState = POMDP.chooseStartState();
			} while (POMDP.isTerminalState(iInitialState));
			
			double dDelta = forwardSearch(iInitialState, bsInitial, 1, numNewBeliefs, numNewBeliefs);
		}while( exploredBeliefs.size() < numNewBeliefs );
		//Logger.getInstance().logln("maxdelta = "+  dDelta);
		
		/* reverse the order of beliefs, so they will be updated at the end of the tree first */
		//Collections.reverse(exploredBeliefs); - no need, reversing at backup time		
		
		
		return exploredBeliefs;
		
	}
	
	
	
	public Vector<BeliefState> expand(Vector<BeliefState> vBeliefPoints)
	{
		return expand(defaultMaxDepth, vBeliefPoints);		
	}
	

	
	protected double forwardSearch(int iState, BeliefState bsCurrent, int iDepth, int maxDepth, int cMaxBeliefs){
		double dDelta = 0.0, dNextDelta = 0.0;
		int iNextState = 0, iHeuristicAction = 0, iObservation = 0;
		BeliefState bsNext = null;

		if( m_bAllowDuplicates || !exploredBeliefs.contains( bsCurrent ) )
			exploredBeliefs.add(bsCurrent);	
		if( exploredBeliefs.size() == cMaxBeliefs )
			return 0.0;

		
		if ((POMDP.terminalStatesDefined() && POMDP.isTerminalState(iState)) || (iDepth == maxDepth)){			
			depth = iDepth;
			Logger.getInstance().logln( "Ended at depth " + iDepth + ". isTerminalState(" + iState + ")=" + POMDP.isTerminalState( iState ) );
		}
		else
		{
			iHeuristicAction = getAction( iState, bsCurrent );
			iNextState = selectNextState( iState, iHeuristicAction );
			iObservation = getObservation( iState, iHeuristicAction, iNextState );
			bsNext = bsCurrent.nextBeliefState( iHeuristicAction, iObservation );
			

			if (bsNext == null || bsNext.equals((bsCurrent))){
				//Logger.getInstance().logln( "Ended at depth " + iDepth + " due to an error" );
				depth = iDepth;
			}
			else
			{
				dNextDelta = forwardSearch(iNextState, bsNext, iDepth + 1, maxDepth, cMaxBeliefs);
			}
		}
		return Math.max(dDelta, dNextDelta);
	}

	

	private int getAction( int iState, BeliefState bs ){

		if(valueIteration.getRandomGenerator().nextDouble() < PICK_MDP_ACTION)
			return POMDP.getMDPValueFunction().getAction( iState );
		/* return a random action */
		else
			return valueIteration.getRandomGenerator().nextInt(POMDP.getActionCount());
	}
	private int getObservation(int iStartState, int iAction, int iEndState){
		return POMDP.observe(iAction, iEndState);
	}
	private int selectNextState(int iState, int iAction) {
		return POMDP.execute( iAction, iState);
	}
}
