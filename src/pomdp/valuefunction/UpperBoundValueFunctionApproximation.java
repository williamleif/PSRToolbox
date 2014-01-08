package pomdp.valuefunction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import pomdp.algorithms.pointbased.HeuristicSearchValueIteration.ValueFunctionEntry;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateComparator;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.Logger;
import pomdp.utilities.Pair;

public class UpperBoundValueFunctionApproximation {
	protected POMDP m_pPOMDP;
	protected Map<BeliefState, Double> m_mBeliefStateValues;
	protected double[] m_adStateValues;
	protected boolean m_bUseFIB;
	
	public UpperBoundValueFunctionApproximation( POMDP pomdp, double[] adStateValues ){
		m_pPOMDP = pomdp;
		m_mBeliefStateValues = new TreeMap<BeliefState, Double>( BeliefStateComparator.getInstance() );
		m_adStateValues = adStateValues.clone();
	}
	public UpperBoundValueFunctionApproximation( POMDP pomdp, MDPValueFunction vfMDP, boolean bUseFIB ) {
		m_pPOMDP = pomdp;
		m_mBeliefStateValues = new TreeMap<BeliefState, Double>( BeliefStateComparator.getInstance() );
		m_bUseFIB = bUseFIB;
		m_adStateValues = new double[m_pPOMDP.getStateCount()];
		if(m_bUseFIB)
			initializeFIB(vfMDP);
		else
			initializeMDP(vfMDP);
		//initializeFIB(vfMDP);
		
		
//		double TOTAL = 0;
//		for(int s = 0; s < m_pPOMDP.getStateCount(); s++)
//		{
//			TOTAL += m_adStateValues[s];
//
//		}
//		Logger.getInstance().logln("TOTAL = " + TOTAL);
		
	}
	
	public double interpolate( BeliefState bs ){
		double dValue = 0.0;
		Entry<Integer,Double> e = null;
		Iterator<Entry<Integer,Double>> itNonZero = bs.getNonZeroEntries().iterator();
		while( itNonZero.hasNext() ){
			e = itNonZero.next();
			dValue += e.getValue() * m_adStateValues[e.getKey()];
		}
		return dValue;
	}
	
	public double valueAt( BeliefState bs ){
		if( !m_mBeliefStateValues.containsKey( bs ) ){
			return interpolate( bs );
		}
		return m_mBeliefStateValues.get( bs );
	}
	
	public int getAction( BeliefState bs ){
		int iAction = 0, iObservation = 0, iMaxAction = -1;
		double dMaxActionValue = Double.NEGATIVE_INFINITY, dPr = 0.0, dValueSum = 0.0;
		BeliefState bsSuccessor = null;
		
		for( iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
			dValueSum = 0.0;
			for( iObservation = 0 ;iObservation < m_pPOMDP.getObservationCount() ; iObservation++ ){
				bsSuccessor = bs.nextBeliefState( iAction, iObservation );
				dPr = bs.probabilityOGivenA( iAction, iObservation );
				if( dPr > 0.0 ){
					double value = valueAt( bsSuccessor );
					dValueSum += dPr * value;
				}
			}
			if( dValueSum > dMaxActionValue ){
				iMaxAction = iAction;
				dMaxActionValue = dValueSum;
			}
		}
		return iMaxAction;
	}
	public int getAction2( BeliefState bs ){
		return getAction2(bs, null);
	}

	public int getAction2( BeliefState bs, Pair<Integer,Double> pResult ){
		int iAction = 0, iObservation = 0, iMaxAction = -1;
		double dMaxActionValue = Double.NEGATIVE_INFINITY, dPr = 0.0, dValueSum = 0.0;
		BeliefState bsSuccessor = null;

		List<Integer> bestActions = new ArrayList<Integer>();

		for( iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
			dValueSum = m_pPOMDP.immediateReward( bs, iAction );
			for( iObservation = 0 ;iObservation < m_pPOMDP.getObservationCount() ; iObservation++ ){
				bsSuccessor = bs.nextBeliefState( iAction, iObservation );
				dPr = bs.probabilityOGivenA( iAction, iObservation );
				if( dPr > 0.0 ){
					double value = valueAt( bsSuccessor );
					dValueSum += m_pPOMDP.getDiscountFactor() * dPr * value;
				}
			}
			if( dValueSum > dMaxActionValue - .00000001 ){
				bestActions.add(iAction);
				iMaxAction = iAction;
				dMaxActionValue = dValueSum;
				if(pResult != null){
					pResult.setSecond(dMaxActionValue);
				}
			}
		}
		iMaxAction = bestActions.get(m_pPOMDP.getRandomGenerator().nextInt(bestActions.size()));
		if(pResult != null){
			pResult.setFirst(iMaxAction);
		}
		return iMaxAction;
	}

	
	public void setValueAt(BeliefState bs, double dValue){
		m_mBeliefStateValues.put( bs, dValue );
	}

	public void updateValue(BeliefState bs) {

		
		int iAction = 0, iObservation = 0;
		BeliefState bsSuccessor = null;
		double dActionValue = 0.0, dMaxValue = 0.0, dPr = 0.0, dValue = 0.0;
		for( iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
			dActionValue = m_pPOMDP.immediateReward( bs, iAction );
			for( iObservation = 0 ; iObservation < m_pPOMDP.getObservationCount() ; iObservation++ ){
				dPr = bs.probabilityOGivenA( iAction, iObservation );
				if( dPr > 0.0 ){
					bsSuccessor = bs.nextBeliefState( iAction, iObservation );
					dValue = valueAt( bsSuccessor );
					dActionValue += m_pPOMDP.getDiscountFactor() * dValue * dPr;
				}
			}
			if( dActionValue > dMaxValue )
				dMaxValue = dActionValue;
		}
		m_mBeliefStateValues.put( bs, dMaxValue );
	}
	public int getUpperBoundPointCount(){
		return m_mBeliefStateValues.size();
	}
	protected Collection<Entry<BeliefState,Double>> getUpperBoundPoints(){
		return m_mBeliefStateValues.entrySet();
	}
	
	public double getMaxValueUpperBoundPoints()
	{
		double max = Double.MIN_VALUE;
		for (Entry<BeliefState,Double> e : m_mBeliefStateValues.entrySet())
		{
			if (e.getValue() > max)
				max = e.getValue();
		}
		return max;		
	}
	
	public double getMinValueUpperBoundPoints()
	{
		double min = Double.MAX_VALUE;
		for (Entry<BeliefState,Double> e : m_mBeliefStateValues.entrySet())
		{
			if (e.getValue() < min)
				min = e.getValue();
		}
		return min;		
	}
	
	
	
	public void pruneUpperBound(){
		BeliefState bs = null;
		Collection<Entry<BeliefState,Double>> colUpperBound = getUpperBoundPoints();
		double dUpperBound = 0.0, dHValue = 0.0, dNewValue = 0.0;
		int cBeliefPoints = getUpperBoundPointCount();
		double dEpsilon = (1e-10);
		Vector<BeliefState> vToRemove = new Vector<BeliefState>();
		
		for( Entry<BeliefState,Double> e : colUpperBound ){
			dUpperBound = e.getValue();
			
			bs = e.getKey();
			if( !bs.isDeterministic() ){				
				dHValue = interpolate( bs );
				if( dHValue <  dUpperBound - dEpsilon ){
					vToRemove.add( bs );
				}
				
			}
		}

		for( BeliefState bsRemove : vToRemove ){
			m_mBeliefStateValues.remove( bsRemove );
		}
		
		Logger.getInstance().log( "UpperBoundValueIteration", 0, "pruneUpperBound", "Pruned from " + cBeliefPoints + " to " + getUpperBoundPointCount() );
	}


	public void initializeMDP(MDPValueFunction vfMDP)
	{
		int iState = 0;
		for(iState = 0; iState < m_pPOMDP.getStateCount(); iState++)
			m_adStateValues[iState] = vfMDP.getValue(iState);		
	}
	public void initializeFIB(MDPValueFunction vfMDP)
	{
		Logger.getInstance().logln("Started FIB");
		int iStartState = 0, iEndState = 0, iObservation = 0;
		double dMaxR = m_pPOMDP.getMaxR() / (1 - m_pPOMDP.getDiscountFactor());
		double[][] adQFunction = new double[m_pPOMDP.getStateCount()][m_pPOMDP.getActionCount()];
		for(iStartState = 0; iStartState < m_pPOMDP.getStateCount(); iStartState++)
			for(int iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
				//double dValue = vfMDP.getQValue(iStartState, iAction);
				adQFunction[iStartState][iAction] = 0.0;			
				//adQFunction[iStartState][iAction] = dValue;			
			}
				//adQFunction[iStartState][iAction] = 0.0;//dMaxR;
			//m_adStateValues[iStartState] = vfMDP.getValue(iStartState);		
		double dMaxDiff = Double.POSITIVE_INFINITY;
		int iIteration = 0;
		while(dMaxDiff > 0.01){
			dMaxDiff = 0.0;
			for(iStartState = 0; iStartState < m_pPOMDP.getStateCount(); iStartState++){
				for(int iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
					double dActionValue = 0.0;
					for(iObservation = 0; iObservation < m_pPOMDP.getObservationCount(); iObservation++){
						double[] adNextAction = new double[m_pPOMDP.getActionCount()];
						Iterator<Entry<Integer,Double>> it = m_pPOMDP.getNonZeroTransitions(iStartState, iAction);
						while(it.hasNext()){
							Entry<Integer,Double> e = it.next();
							iEndState = e.getKey();
							double dTr = e.getValue();
							double dO = m_pPOMDP.O(iAction, iEndState, iObservation);
							for(int iNextAction = 0 ; iNextAction < m_pPOMDP.getActionCount() ; iNextAction++){
								adNextAction[iNextAction] +=  dO * dTr * adQFunction[iEndState][iNextAction];
							}
						}
					
						double dBestNextAction = Double.NEGATIVE_INFINITY;
						for(int iNextAction = 0 ; iNextAction < m_pPOMDP.getActionCount() ; iNextAction++){
							if(adNextAction[iNextAction] > dBestNextAction)
								dBestNextAction = adNextAction[iNextAction];
						}
						dActionValue += dBestNextAction;
					}
					double dNewActionValue = dActionValue * m_pPOMDP.getDiscountFactor() + m_pPOMDP.R(iStartState, iAction);
					double dDiff = Math.abs(adQFunction[iStartState][iAction] - dNewActionValue);
					if(dDiff > dMaxDiff)
						dMaxDiff = dDiff;
					adQFunction[iStartState][iAction] = dNewActionValue;
				}
			}	
			Logger.getInstance().logln("FIB: " + iIteration + ", " + dMaxDiff);
			iIteration++;
		}
		dMaxDiff = 0.0;
		double dMaxReverseDiff = 0.0;
		for(iStartState = 0; iStartState < m_pPOMDP.getStateCount(); iStartState++){
			m_adStateValues[iStartState] = Double.NEGATIVE_INFINITY;
			for(int iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
				if( adQFunction[iStartState][iAction] > m_adStateValues[iStartState])
					m_adStateValues[iStartState] = adQFunction[iStartState][iAction];
			}
			double dDiff = Math.abs(vfMDP.getValue(iStartState) - m_adStateValues[iStartState]);
			if(m_adStateValues[iStartState] < vfMDP.getValue(iStartState))
			{
				if( dDiff > dMaxDiff)
					dMaxDiff = vfMDP.getValue(iStartState) - m_adStateValues[iStartState];
			}
			else if(dDiff > dMaxReverseDiff)
				dMaxReverseDiff = dDiff;
		}
		Logger.getInstance().logln("Done FIB, max diff between QMDP and FIB is " + dMaxDiff + ", " + dMaxReverseDiff);
	
	}
	public void initializeFIBIII(MDPValueFunction vfMDP)
	{
		Logger.getInstance().logln("Started FIB");
		int iStartState = 0, iEndState = 0, iObservation = 0;
		double dMaxR = m_pPOMDP.getMaxR() / (1 - m_pPOMDP.getDiscountFactor());
		double[][] adQFunction = new double[m_pPOMDP.getStateCount()][m_pPOMDP.getActionCount()];
		for(iStartState = 0; iStartState < m_pPOMDP.getStateCount(); iStartState++)
			for(int iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ )
				adQFunction[iStartState][iAction] = 0.0;//dMaxR;
			//m_adStateValues[iStartState] = vfMDP.getValue(iStartState);		
		double dMaxDiff = Double.POSITIVE_INFINITY;
		int iIteration = 0;
		while(dMaxDiff > 0.01){
			dMaxDiff = 0.0;
			for(iStartState = 0; iStartState < m_pPOMDP.getStateCount(); iStartState++){
				for(int iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
					double dActionValue = 0.0;
					for(iObservation = 0; iObservation < m_pPOMDP.getObservationCount(); iObservation++){
						double dBestNextAction = Double.NEGATIVE_INFINITY;
						for(int iNextAction = 0 ; iNextAction < m_pPOMDP.getActionCount() ; iNextAction++){
							double dNextActionValue = 0.0;
							Iterator<Entry<Integer,Double>> it = m_pPOMDP.getNonZeroTransitions(iStartState, iAction);
							while(it.hasNext()){
								Entry<Integer,Double> e = it.next();
								iEndState = e.getKey();
								double dTr = e.getValue();
								dNextActionValue += m_pPOMDP.O(iAction, iEndState, iObservation) * 
										dTr * adQFunction[iEndState][iNextAction];
							}
							/*
							for(iEndState = 0; iEndState < m_pPOMDP.getStateCount(); iEndState++){
								dNextActionValue += m_pPOMDP.O(iAction, iEndState, iObservation) * 
										m_pPOMDP.tr(iStartState, iAction, iEndState) * adQFunction[iEndState][iNextAction];
							}
							*/
							if(dNextActionValue > dBestNextAction)
								dBestNextAction = dNextActionValue;
						}
						dActionValue += dBestNextAction;
					}
					double dNewActionValue = dActionValue * m_pPOMDP.getDiscountFactor() + m_pPOMDP.R(iStartState, iAction);
					double dDiff = Math.abs(adQFunction[iStartState][iAction] - dNewActionValue);
					if(dDiff > dMaxDiff)
						dMaxDiff = dDiff;
					adQFunction[iStartState][iAction] = dNewActionValue;
				}
			}	
			Logger.getInstance().logln("FIB: " + iIteration + ", " + dMaxDiff);
			iIteration++;
		}
		dMaxDiff = 0.0;
		double dMaxReverseDiff = 0.0;
		for(iStartState = 0; iStartState < m_pPOMDP.getStateCount(); iStartState++){
			m_adStateValues[iStartState] = Double.NEGATIVE_INFINITY;
			for(int iAction = 0 ; iAction < m_pPOMDP.getActionCount() ; iAction++ ){
				if( adQFunction[iStartState][iAction] > m_adStateValues[iStartState])
					m_adStateValues[iStartState] = adQFunction[iStartState][iAction];
			}
			double dDiff = Math.abs(vfMDP.getValue(iStartState) - m_adStateValues[iStartState]);
			if(m_adStateValues[iStartState] < vfMDP.getValue(iStartState))
			{
				if( dDiff > dMaxDiff)
					dMaxDiff = vfMDP.getValue(iStartState) - m_adStateValues[iStartState];
			}
			else if(dDiff > dMaxReverseDiff)
				dMaxReverseDiff = dDiff;
		}
		Logger.getInstance().logln("Done FIB, max diff between QMDP and FIB is " + dMaxDiff + ", " + dMaxReverseDiff);
	
	}
	public void initializeFIBII(MDPValueFunction vfMDP)
	{
		int T = 10;
		double[][][] alphaAs = new double[T][m_pPOMDP.getActionCount()][m_pPOMDP.getStateCount()];
		Iterator<Entry<Integer, Double>> itNonZeroTransitions = null;
		Entry<Integer, Double> nextStateTransition = null;
		/* iteration */
		for (int t = 0; t < T; t++)
		{
			for (int a = 0; a < m_pPOMDP.getActionCount(); a++)	
			{
				for(int s = 0; s < m_pPOMDP.getStateCount(); s++)	
				{
					if (t == 0) {
						/* initialize to MDP VF */			
						alphaAs[t][a][s] = vfMDP.getValue(s);
					}
					else
					{							
						double nextStateValue = 0;
						for(int o = 0; o < m_pPOMDP.getObservationCount(); o++)
						{
							double maxAction = Double.NEGATIVE_INFINITY;
					
							for (int aNext = 0; aNext < m_pPOMDP.getActionCount(); aNext++)
							{
								double innerSum = 0;
								
								
								itNonZeroTransitions = m_pPOMDP.getNonZeroTransitions(s,a);
								while(itNonZeroTransitions.hasNext()){
									nextStateTransition = itNonZeroTransitions.next();
									int sNext = nextStateTransition.getKey();
									double transitionProb = nextStateTransition.getValue();
									
								
									double observationProb = m_pPOMDP.O(a, sNext, o);
									//double transitionProb = m_pPOMDP.tr(s, a, sNext);
									innerSum += observationProb * transitionProb * alphaAs[t-1][aNext][sNext];

								}
								
								if (innerSum > maxAction)
									maxAction = innerSum;

							}
							nextStateValue += maxAction;
						}
						alphaAs[t][a][s] = m_pPOMDP.R(s,a) + m_pPOMDP.getDiscountFactor() * nextStateValue;
					}
				}
			}
		}
		double[][] finalAlphas = alphaAs[T-1];
		

		for(int s = 0; s < m_pPOMDP.getStateCount(); s++)
		{
			double best = Double.NEGATIVE_INFINITY;
		
			for (int a = 0; a < m_pPOMDP.getActionCount(); a++)
			{
				if (finalAlphas[a][s] > best)
					best = finalAlphas[a][s];
				
			}
			m_adStateValues[s] = best;

		}

	}

	public boolean UseFIB(){
		return m_bUseFIB;
	}

}
