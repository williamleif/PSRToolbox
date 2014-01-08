package pomdp.algorithms.beliefcollection;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;

public class PBVILeafCollection extends BeliefCollection{

	public Vector<BeliefState> leafs;
	boolean fullSuccessors;
	public PBVILeafCollection(ValueIteration vi, boolean _fullSuccessors, boolean bAllowDuplicates)
	{	
		super(vi, bAllowDuplicates);	
		leafs =  new Vector<BeliefState>();
		fullSuccessors = _fullSuccessors;
	}
	

	
	/* in PBVI, we start only with the initial belief state of the POMDP */
	public Vector<BeliefState> initialBelief()
	{
		Vector<BeliefState> initial = new Vector<BeliefState>();
		
		/* initialize the list of belief points with the initial belief state */
		initial.add(POMDP.getBeliefStateFactory().getInitialBeliefState());
		leafs.add(POMDP.getBeliefStateFactory().getInitialBeliefState());
		return initial;
	}
	

	
	public Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> beliefPoints)
	{
		Vector<BeliefState> newBeliefs = new Vector<BeliefState>();			
		Vector<BeliefState> combinedBeliefs = new Vector<BeliefState>(beliefPoints);
		BeliefState picked;
		
		while (newBeliefs.size() < numNewBeliefs)
		{

			if ((valueIteration.getPOMDP().getRandomGenerator().nextDouble() < .75) && !leafs.isEmpty())
			{
				int randomBelief = valueIteration.getPOMDP().getRandomGenerator().nextInt(leafs.size());
				picked = leafs.get(randomBelief);
			}
			else
			{
				int randomBelief = valueIteration.getPOMDP().getRandomGenerator().nextInt(combinedBeliefs.size());
				picked = combinedBeliefs.get(randomBelief);

			}

			leafs.remove(picked);
			
			BeliefState bsNext;
			if (fullSuccessors)
				 bsNext = POMDP.getBeliefStateFactory().computeFarthestSuccessorFull(combinedBeliefs, picked);
			else
				 bsNext = POMDP.getBeliefStateFactory().computeFarthestSuccessor(combinedBeliefs, picked);
			
			
			if ((bsNext != null) && (!combinedBeliefs.contains(bsNext))) {
				//Logger.getInstance().logln("new");
				newBeliefs.add(bsNext);
				combinedBeliefs.add(bsNext);
				leafs.add(bsNext);
				
			}		
					
		}
		//Logger.getInstance().logln(leafs.size());
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
