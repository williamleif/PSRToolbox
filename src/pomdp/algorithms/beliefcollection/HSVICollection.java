package pomdp.algorithms.beliefcollection;


import java.util.Vector;

import pomdp.algorithms.ValueIteration;
import pomdp.utilities.BeliefState;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.valuefunction.JigSawValueFunction;

public class HSVICollection extends BeliefCollection {
	
	protected JigSawValueFunction m_vfUpperBound;
	protected double m_dMaxWidthForIteration;
	
	Vector<BeliefState> exploredBeliefs;
	Vector<BeliefState> previousBeliefs;
	//parameters
	int defaultNumBeliefPoints = 200;
	//int count;
	
	public HSVICollection(ValueIteration vi, boolean bAllowDuplicates, boolean bUseFIB)
	{	
		super(vi, bAllowDuplicates);	

		POMDP.resetMDPValueFunction();
		POMDP.getMDPValueFunction().persistQValues( true );
		POMDP.getMDPValueFunction().valueIteration( 1000, ExecutionProperties.getEpsilon());
		
		
		m_vfUpperBound = new JigSawValueFunction(POMDP, POMDP.getMDPValueFunction(), bUseFIB);
	}
	
	/* no initial belief */
	public Vector<BeliefState> initialBelief()
	{
		
		
		return new Vector<BeliefState>();
	}

	public Vector<BeliefState> expand(int numNewBeliefs, Vector<BeliefState> beliefPoints)
	{
		previousBeliefs = beliefPoints;
		exploredBeliefs = new Vector<BeliefState>();

		int cUpperBoundPoints = 0;

		BeliefState bsInitial = POMDP.getBeliefStateFactory().getInitialBeliefState();
		double dInitialWidth = width(bsInitial);		
		Logger.getInstance().logln("initial width = " + dInitialWidth);
		Logger.getInstance().logln("upper: "+ m_vfUpperBound.valueAt( bsInitial ));
		Logger.getInstance().logln("lower: "+ valueIteration.valueAt(bsInitial ));

		
		do{
			m_dMaxWidthForIteration = 0.0;
			explore(bsInitial, epsilon, 1, 1.0, numNewBeliefs, numNewBeliefs);
		}while( exploredBeliefs.size() < numNewBeliefs );

		if( ( m_vfUpperBound.getUpperBoundPointCount() > 1000 ) && ( m_vfUpperBound.getUpperBoundPointCount() > cUpperBoundPoints * 1.1 ) ){
			m_vfUpperBound.pruneUpperBound();
			cUpperBoundPoints = m_vfUpperBound.getUpperBoundPointCount();
		}			

		dInitialWidth = width(bsInitial);

		return exploredBeliefs;		
	}

	public Vector<BeliefState> expand(Vector<BeliefState> beliefPoints)
	{
		return expand(defaultNumBeliefPoints, beliefPoints);
	}
		
	
	protected int explore(BeliefState bsCurrent, double dEpsilon, int t, double dDiscount, int maxSteps, int cMaxBeliefs){
		
		double dWidth = width(bsCurrent);
		int iMaxDepth = 0;

		if (POMDP.terminalStatesDefined() && bsCurrent.isDeterministic()){
			int iState = bsCurrent.getDeterministicIndex();
			if(POMDP.isTerminalState(iState))
				return t;
		}
		
		if( m_bAllowDuplicates || !exploredBeliefs.contains(bsCurrent))
			exploredBeliefs.add(bsCurrent);
		if( exploredBeliefs.size() == cMaxBeliefs )
			return t;
		
		if( dWidth > m_dMaxWidthForIteration )
			m_dMaxWidthForIteration = dWidth;
		
		/* stopping condition, stop after maxSteps or if the width is smaller than epsilon*gamma(-t) (line 1 in Alg3 in HSVI2) */
		if((t == maxSteps) || (dWidth < (dEpsilon / dDiscount)))
			return t;
			
		BeliefState bsNext = getNextBeliefState(bsCurrent, dEpsilon, dDiscount * POMDP.getDiscountFactor());

		if ((bsNext != null) && (bsNext != bsCurrent))
			iMaxDepth = explore(bsNext, dEpsilon, t+1, dDiscount * POMDP.getDiscountFactor(), maxSteps, cMaxBeliefs);
		else
			iMaxDepth = t;

		m_vfUpperBound.updateValue(bsCurrent);
					
		return iMaxDepth;
	}
	
	protected BeliefState getNextBeliefState(BeliefState bsCurrent, double dEpsilon, double dDiscount){
		
		
		int action = m_vfUpperBound.getAction2(bsCurrent);
			
		int observation = getExplorationObservation(bsCurrent, action, dEpsilon, dDiscount);
				
		if(observation == -1) {
			//Logger.getInstance().logln("all beliefs under this s,a are added");
			return null;
		}
		return bsCurrent.nextBeliefState(action, observation);		
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
	
	
	protected int getExplorationObservation( BeliefState bsCurrent, int iAction, 
			double dEpsilon, double dDiscount ){
		int iObservation = 0, iMaxObservation = -1;
		double dProb = 0.0, dExcess = 0.0, dValue = 0.0, dMaxValue = 0.0;
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
}
