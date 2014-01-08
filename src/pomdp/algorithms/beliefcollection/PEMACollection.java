package pomdp.algorithms.beliefcollection;

import java.util.Collection;
import java.util.Vector;
import java.util.Vector;
import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;

public class PEMACollection extends BeliefCollection{

	public PEMACollection(ValueIteration vi, boolean bAllowDuplicates)
	{	
		super(vi, bAllowDuplicates);	
	}
	
	
	/* in PEMA, we start only with the initial belief state of the POMDP */
	public Vector<BeliefState> initialBelief()
	{
		Vector<BeliefState> initial = new Vector<BeliefState>();
		
		/* initialize the list of belief points with the initial belief state */
		initial.add(POMDP.getBeliefStateFactory().getInitialBeliefState());
		
		return initial;
	}
	
	
	public Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> vBeliefPoints)
	{

		Vector<BeliefState> newBeliefs = new Vector<BeliefState>();
		Vector<BeliefState> combinedBeliefs = new Vector<BeliefState>(vBeliefPoints);
		Vector<BeliefState> fullyExploredBeliefs = new Vector<BeliefState>();
		
		int oldSize = -1;
		BeliefState bestBelief = null;
		while (newBeliefs.size() < numNewBeliefs)
		{
			
			/* there is a possibility that a belief has all descentants already explored. 
			 * If this is the case, this should not be checked for having the largest error, as
			 * the descendant belief will already be there.
			 */
			
			/* nothing was added */
			if (newBeliefs.size() == oldSize)
			{
				fullyExploredBeliefs.add(bestBelief);
				
			}

			oldSize = newBeliefs.size();
			double maxError = 0.0;
			bestBelief = null;

			/* iterate through the previous set of belief points */			
			for (BeliefState bsCurrent : combinedBeliefs)
			{
				if (!fullyExploredBeliefs.contains(bsCurrent)) {
					
					//Logger.getInstance().logln(combinedBeliefs.size());
					double error = PEMAWeightedErrorBound(combinedBeliefs, bsCurrent);
					if (error > maxError) {
						maxError = error;
						bestBelief = bsCurrent;
					}
				}

			}
			if(bestBelief == null)
				return newBeliefs;
			
			BeliefState bsNext = getPEMADescendant(combinedBeliefs, bestBelief);
				
			if((bsNext != null) && (!combinedBeliefs.contains(bsNext))){
				newBeliefs.add(bsNext);
				combinedBeliefs.add(bsNext);
			}
			
			
		}
		return newBeliefs;
	}
	
	
	public Vector<BeliefState> expand(Vector<BeliefState> vBeliefPoints){
		
		return expand(1, vBeliefPoints);
	}


	
	
	/***
	 * Compute the error bound, Eq. (7) in PEMA paper
	 */
	private double PEMAWeightedErrorBound(Collection <BeliefState> vBeliefPoints, BeliefState bs)
	{
		double maxError = Double.NEGATIVE_INFINITY;
		int action;		
		int observation;
		
		for (action = 0; action < POMDP.getActionCount(); action++)
		{
			double actionObservationError = 0;
			for (observation = 0; observation < POMDP.getObservationCount(); observation++)
			{
				double observationProb = bs.probabilityOGivenA( action, observation );
				
				if (observationProb > 0)
				{
					BeliefState nextBelief = bs.nextBeliefState(action, observation);
					double error = PEMAErrorBound(vBeliefPoints, nextBelief);
					actionObservationError += observationProb * error;		
				}
			}
			
			if (actionObservationError > maxError)
			{
				maxError = actionObservationError;		
			}			
		}
		return maxError;
	}
	
	/***
	 * get the descendant belief that has the largest impact on the error of the current belief.
	 * @param vBeliefPoints is the current set of belief points
	 * @param bs is the current belief
	 */
	private BeliefState getPEMADescendant(Collection<BeliefState> vBeliefPoints, BeliefState bs)
	{
		int action;		
		int observation;
		BeliefState bestBelief = null;
		
		double maxWeightedError = Double.NEGATIVE_INFINITY;
		for (action = 0; action < POMDP.getActionCount(); action++)
		{
			for (observation = 0; observation < POMDP.getObservationCount(); observation++)
			{
				double observationProb = bs.probabilityOGivenA(action, observation);

				if (observationProb > 0)
				{
					BeliefState nextBelief = bs.nextBeliefState(action, observation);

					if (!vBeliefPoints.contains(nextBelief))
					{					
						double error = PEMAErrorBound(vBeliefPoints, nextBelief);
						/* the amount the descendant impacts on total error */
						double weightedError = observationProb * error;
						if (weightedError > maxWeightedError)
						{
							maxWeightedError = weightedError;
							bestBelief = nextBelief;
						}
					}		
				}
			}


		}
		return bestBelief;	
	}
	
	/***
	 * The error for a belief point, as in PEMA, the last equation in 2.4 
	 * @param vBeliefPoints current set of belief points
	 * @param bprime the belief we are looking at
	 * @return the error
	 */
	private double PEMAErrorBound(Collection<BeliefState> vBeliefPoints, BeliefState bprime)
	{
		double RMAX = POMDP.getMaxR();
		double RMIN = POMDP.getMinR();
		double totalError = 0;
		/* find the closest (1-norm) sampled belief to b' */

		BeliefState b = POMDP.getBeliefStateFactory().getNearestL1Belief(vBeliefPoints, bprime);
		AlphaVector a = b.getMaxAlpha();

		if (a != null) {
			int state;
			for (state = 0; state < POMDP.getStateCount(); state++)
			{
				double beliefDifference = bprime.valueAt(state) - b.valueAt(state);
				if (bprime.valueAt(state) >= b.valueAt(state))
				{
					totalError += ((RMAX / (1 - POMDP.getDiscountFactor())) - a.valueAt(state)) * beliefDifference;				
				}
				else
				{
					totalError += ((RMIN / (1 - POMDP.getDiscountFactor())) - a.valueAt(state)) * beliefDifference;
				}

			}

		}
		return totalError;
		
	}
	
}
