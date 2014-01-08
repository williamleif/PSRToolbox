package pomdp.algorithms.beliefcollection;

import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BinManager;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.valuefunction.JigSawValueFunction;

public class SARSOPCollection extends BeliefCollection {


	protected JigSawValueFunction m_vfUpperBound;
	int defaultNumBeliefPoints = 200;
	Vector<BeliefState> exploredBeliefs;
	Vector<BeliefState> previousBeliefs;
	BinManager bm;


	public SARSOPCollection(ValueIteration vi, boolean bAllowDuplicates, boolean bUseFIB)
	{	
		super(vi, bAllowDuplicates);	
		POMDP.resetMDPValueFunction();
		POMDP.getMDPValueFunction().persistQValues(true);
		POMDP.getMDPValueFunction().valueIteration(1000, ExecutionProperties.getEpsilon());

		m_vfUpperBound = new JigSawValueFunction(POMDP, POMDP.getMDPValueFunction(), bUseFIB);
		bm = new BinManager(this, m_vfUpperBound);
	}

	/* no initial belief */
	public Vector<BeliefState> initialBelief()
	{
		return new Vector<BeliefState>();
	}

	public Vector<BeliefState> expand(Vector<BeliefState> beliefPoints)
	{
		return expand(defaultNumBeliefPoints, beliefPoints);
	}


	public Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> beliefPoints)
	{
		//BUGBUG - check that order is not reversed, check that duplicates are removed only if needed
		previousBeliefs = beliefPoints;
		exploredBeliefs = new Vector<BeliefState>();

		int cUpperBoundPoints = 0;

		BeliefState bsInitial = POMDP.getBeliefStateFactory().getInitialBeliefState();

		//double dInitialWidth = width(bsInitial);		
		//Logger.getInstance().logln("initial width = " + dInitialWidth);
		Logger.getInstance().logln("upper: "+ m_vfUpperBound.valueAt( bsInitial ));
		Logger.getInstance().logln("lower: "+ valueIteration.valueAt(bsInitial ));


		double initialL = valueIteration.valueAt(bsInitial);
		
		// this is as per paper, in SARSOP c++ code, this looks like it doesn't add the epsilon
		double initialU = valueIteration.valueAt(bsInitial) + epsilon;
		
		explore(bsInitial, epsilon, 1, 1.0, numNewBeliefs, initialL, initialU);

		if( ( m_vfUpperBound.getUpperBoundPointCount() > 1000 ) && ( m_vfUpperBound.getUpperBoundPointCount() > cUpperBoundPoints * 1.1 ) ){
			m_vfUpperBound.pruneUpperBound();
			cUpperBoundPoints = m_vfUpperBound.getUpperBoundPointCount();
		}			

		removeDuplicates(exploredBeliefs);
		return exploredBeliefs;		
	}


	protected int explore(BeliefState bsCurrent, double dEpsilon, int t, double dDiscount, int maxSteps, double L, double U){

		double dWidth = width(bsCurrent);
		int iMaxDepth = 0;

		bm.updateNode(bsCurrent);

		/* stopping condition, stop after maxSteps or if the width is smaller than .5* epsilon*gamma(-t) (paragraph under alg3) */
		if((t >= maxSteps) || (dWidth < .5*(dEpsilon / dDiscount)))
			return t;
		
		/* SARSOP specific stopping condition (lines 4->6) furthur down */
		

		
		//8
		double Lprime = Math.max(L, valueIteration.valueAt(bsCurrent));
		//9
		double Uprime = Math.max(U, valueIteration.valueAt(bsCurrent) + (dEpsilon / dDiscount));
		
		//10
		int action = m_vfUpperBound.getAction2(bsCurrent);
		
		//11
		int observation = getExplorationObservation(bsCurrent, action, dEpsilon, dDiscount * POMDP.getDiscountFactor());

		if(observation == -1) {
			Logger.getInstance().logln("all beliefs under this s,a are added");
			return t;
		}
		
		

		//compute "futurevalues", this is \sum_{o != o') p(o|b,a') V(nextbelief), upper or lower
		double lowerFuture = getFutureValue(bsCurrent, action, observation, "lower");
		double upperFuture = getFutureValue(bsCurrent, action, observation, "upper");
		
		double immediateReward = bsCurrent.getActionImmediateReward(action);

		//14
		BeliefState bsNext = bsCurrent.nextBeliefState(action, observation);
		

		/* other SARSOP specific stopping condition */
		/* check lower bound target to prediction */
		double Vpred = bm.getBinValue(bsNext);

		
		//Not super clear in SARSOP paper, but coded as this in SampleBP:CompareIfLowerBoundImprovesAction in SARSOP code
		double Vhat = immediateReward + POMDP.getDiscountFactor() * (lowerFuture + bsCurrent.probabilityOGivenA(action, observation)* Vpred);
		
		double maxValueForLowerBound = Double.MIN_NORMAL;
		for(int iAction = 0 ; iAction < POMDP.getActionCount() ; iAction++){
			
			double fullFuture = getFullFutureValue(bsCurrent, iAction);
			if (fullFuture > maxValueForLowerBound)
			{
				maxValueForLowerBound = fullFuture;
			}
		}
		
		double currentTarget = Math.max(L, maxValueForLowerBound);

		/* lines 4->6 */
		if (Vhat < currentTarget) {

			/* other condition in this line is taken up by first stopping condition */
			if (m_vfUpperBound.valueAt(bsCurrent) <= U)
				return t;
		}
		
		//12		
		double Lnext = ((Lprime - immediateReward) / POMDP.getDiscountFactor() - lowerFuture) / bsCurrent.probabilityOGivenA(action, observation);
		
		//13		
		double Unext = ((Uprime - immediateReward) / POMDP.getDiscountFactor() - upperFuture) / bsCurrent.probabilityOGivenA(action, observation);
		
	
		if ((bsNext != null) && (bsNext != bsCurrent))
			iMaxDepth = explore(bsNext, dEpsilon, t+1, dDiscount * POMDP.getDiscountFactor(), maxSteps, Lnext, Unext);
		else
			iMaxDepth = t;

		
		m_vfUpperBound.updateValue(bsCurrent);
		
		//step 15, unrolled
		if( !exploredBeliefs.contains(bsCurrent))
			exploredBeliefs.add(bsCurrent);


		return iMaxDepth;
	}


	protected int getExplorationObservation( BeliefState bsCurrent, int iAction, double dEpsilon, double dDiscount ){
		int iObservation = 0, iMaxObservation = -1;
		double dProb = 0.0, dExcess = 0.0, dValue = 0.0, dMaxValue = -1000000.0;
		BeliefState bsNext = null;

		for( iObservation = 0 ; iObservation < POMDP.getObservationCount() ; iObservation++ ){
			dProb = bsCurrent.probabilityOGivenA( iAction, iObservation );
			if( dProb > 0 ){
				bsNext = bsCurrent.nextBeliefState( iAction, iObservation );
				dExcess = excess( bsNext, dEpsilon, dDiscount );
				dValue = dProb * dExcess;  
				if( dValue > dMaxValue ){
					dMaxValue = dValue;
					iMaxObservation = iObservation;
				}			
			}
		}
		return iMaxObservation;
	}
	
	
	protected double excess( BeliefState bsCurrent, double dEpsilon, double dDiscount ){
		return width( bsCurrent ) - ( dEpsilon / dDiscount );
	}

	protected double width( BeliefState bsCurrent ){
		double dUpperValue = 0.0, dLowerValue = 0.0, dWidth = 0.0;
				
		dUpperValue = m_vfUpperBound.valueAt( bsCurrent );
		dLowerValue = valueIteration.valueAt( bsCurrent );
		dWidth = dUpperValue - dLowerValue;	
		return dWidth;
	}
	


	private double getFutureValue(BeliefState bsCurrent, int action, int observation, String type)
	{
		int iObservation;
		double dProb = 0.0;
		double total = 0.0;
		double totalProb = 0.0;
		BeliefState bsNext = null;

		for(iObservation = 0 ; iObservation < POMDP.getObservationCount(); iObservation++){

			if (iObservation != observation)
			{
				dProb =  bsCurrent.probabilityOGivenA(action, iObservation);
				if( dProb > 0 ){
					totalProb += dProb;
					bsNext = bsCurrent.nextBeliefState(action, iObservation);
					if (type.equals("upper"))
						total += dProb * m_vfUpperBound.valueAt(bsNext);
					else if (type.equals("lower"))
						total += dProb * valueIteration.valueAt(bsNext);

				}
			}
		}
		return total;
	}
	
	private double getFullFutureValue(BeliefState bsCurrent, int action)
	{
		double immediateReward = POMDP.immediateReward(bsCurrent,action);
		
		int iObservation;
		double dProb = 0.0;
		double total = 0.0;
		double totalProb = 0.0;
		BeliefState bsNext = null;

		for(iObservation = 0 ; iObservation < POMDP.getObservationCount(); iObservation++){

				dProb =  bsCurrent.probabilityOGivenA(action, iObservation);
				if( dProb > 0 ){
					totalProb += dProb;
					bsNext = bsCurrent.nextBeliefState(action, iObservation);
						total += dProb * valueIteration.valueAt(bsNext);

			}
		}
		total = immediateReward + POMDP.getDiscountFactor() * total;
		return total;
	}
}
