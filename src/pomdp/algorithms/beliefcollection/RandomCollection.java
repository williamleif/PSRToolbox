package pomdp.algorithms.beliefcollection;

import java.util.Collections;
import java.util.Vector;
import pomdp.CreateBeliefSpaces;
import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;

public class RandomCollection extends BeliefCollection {

	public RandomCollection(ValueIteration vi, boolean bAllowDuplicates)
	{	
		super(vi, bAllowDuplicates);	
	}

	/* Initial random belief set */
	public Vector<BeliefState> initialBelief()
	{
		int defaultNumBeliefs = 0;
		Vector<BeliefState> initial = new Vector<BeliefState>(CreateBeliefSpaces.createRandomSpace(POMDP, defaultNumBeliefs));	
		return initial;
	}
	
	/* does no expansion */
	public Vector<BeliefState> expand(Vector<BeliefState> beliefPoints){
		return null;	
	}
	
	/* expand to get N new random beliefs */
	public Vector<BeliefState> expand(int numNewBelief, Vector<BeliefState> beliefPoints){
		
		Vector<BeliefState> newBeliefs = new Vector<BeliefState>();
		while (newBeliefs.size() < numNewBelief)
		{
			Vector<BeliefState> randomBeliefs = new Vector<BeliefState>(CreateBeliefSpaces.createRandomSpace(POMDP, numNewBelief));

			/* make sure the beliefs are new */
			if( !m_bAllowDuplicates )
				randomBeliefs.removeAll(beliefPoints);		
			newBeliefs.addAll(randomBeliefs);
			if( !m_bAllowDuplicates )
				removeDuplicates(newBeliefs);
		}

		/* fix the size */
		newBeliefs.setSize(numNewBelief);
		//Collections.reverse(newBeliefs); - not needed, backup is done in reversed order		
		
		return newBeliefs;
	}	
}