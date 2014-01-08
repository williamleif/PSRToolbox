package pomdp.algorithms.beliefcollection;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;

public class PBVICollection extends BeliefCollection{

	boolean fullSuccessors;
	
	public PBVICollection(ValueIteration vi, boolean _fullSuccessors, boolean bAllowDuplicates)
	{	
		super(vi, bAllowDuplicates);	
		fullSuccessors = _fullSuccessors;
	}
	
	
	/* in PBVI, we start only with the initial belief state of the POMDP */
	public Vector<BeliefState> initialBelief()
	{
		Vector<BeliefState> initial = new Vector<BeliefState>();
		
		/* initialize the list of belief points with the initial belief state */
		initial.add(POMDP.getBeliefStateFactory().getInitialBeliefState());
		return initial;
	}
	
	
	
	
	public Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> beliefPoints)
	{
		Vector<BeliefState> newBeliefs = new Vector<BeliefState>();			
		Vector<BeliefState> combinedBeliefs = new Vector<BeliefState>(beliefPoints);
		BeliefState picked;
		
		while (newBeliefs.size() < numNewBeliefs && combinedBeliefs.size() > 0 )
		{
			int randomBelief = valueIteration.getPOMDP().getRandomGenerator().nextInt(combinedBeliefs.size());
			picked = combinedBeliefs.get(randomBelief);
			
			BeliefState bsNext;
			if (fullSuccessors)
				 bsNext = POMDP.getBeliefStateFactory().computeFarthestSuccessorFull(combinedBeliefs, picked);
			else
				 bsNext = POMDP.getBeliefStateFactory().computeFarthestSuccessor(combinedBeliefs, picked);
			
			if( bsNext == null )//do not choose again a belief who has all its successors already in B
				combinedBeliefs.remove( picked );
			
			if ((bsNext != null) && (!combinedBeliefs.contains(bsNext))) {
				newBeliefs.add(bsNext);
				combinedBeliefs.add(bsNext);
				
			}		
					
		}
		return newBeliefs;	
	}
	
	
	
	public Vector<BeliefState> expand(Vector<BeliefState> beliefPoints){
			
		Vector<BeliefState> newBeliefs = new Vector<BeliefState>();			
		BeliefState bsNext;
		
		for (BeliefState bsCurrent : beliefPoints)
		{
			bsNext = POMDP.getBeliefStateFactory().computeFarthestSuccessor(beliefPoints, bsCurrent);
			if( (bsNext != null) && (!newBeliefs.contains(bsNext)) && (!beliefPoints.contains(bsNext))){
				newBeliefs.add(bsNext);
			}		
		}	
		return newBeliefs;
	}
	
}
