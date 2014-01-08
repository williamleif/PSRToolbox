package pomdp.valuefunction;

import java.util.Iterator;
import java.util.Map.Entry;

import pomdp.algorithms.pointbased.HeuristicSearchValueIteration.ValueFunctionEntry;
import pomdp.environments.POMDP;
import pomdp.utilities.BeliefState;

public class JigSawValueFunction extends UpperBoundValueFunctionApproximation {

	public JigSawValueFunction( POMDP pomdp, MDPValueFunction vfMDP, boolean bUseFIB  ) {
		super( pomdp, vfMDP, bUseFIB );
	}

	protected double computeValueGivenCornerPoints( BeliefState bs ){
		double dVb = 0.0;
		int iState = 0;
		Iterator<Entry<Integer, Double>> itNonZero = bs.getNonZeroEntries().iterator();
		Entry<Integer, Double> e = null;
		double dProbB = 0.0;
		
		while( itNonZero.hasNext() ){
			e = itNonZero.next();
			iState = e.getKey();
			dProbB = e.getValue();
			dVb += m_adStateValues[iState] * dProbB;
		}		
		return dVb;
	}
	
	protected double computeNewValue( BeliefState bs ){
		double dVb = 0.0, dVc = 0.0, dV = 0.0;
		int iState = 0;
		Iterator<Entry<Integer, Double>> itNonZero = bs.getNonZeroEntries().iterator();
		Iterator<Entry<BeliefState,Double>> itInnerPoints = m_mBeliefStateValues.entrySet().iterator();
		Entry<Integer, Double> eBelief = null;
		Entry<BeliefState,Double> eValue = null;
		double dProbC = 0.0, dMinR = 100000, dMinValue = 0.0, dProbB = 0.0;
		BeliefState bsInnerPoint = null;
		
		dVb = computeValueGivenCornerPoints( bs );
		dMinValue = dVb;
		
		while( itInnerPoints.hasNext() ){
			eValue = itInnerPoints.next();
			bsInnerPoint = eValue.getKey();
			if( bs != bsInnerPoint ){
				dVc = eValue.getValue();
				itNonZero = bsInnerPoint.getNonZeroEntries().iterator();
				dMinR = 100000;
				while( ( itNonZero.hasNext() ) && ( dMinR > 0.0 ) ){
					eBelief = itNonZero.next();
					iState = eBelief.getKey();
					dProbC = eBelief.getValue();
					dProbB = bs.valueAt( iState );
					if( dProbB / dProbC < dMinR )
						dMinR = dProbB / dProbC;
				}
				if( dMinR > 0.0 ){
					dV = computeValueGivenCornerPoints( bsInnerPoint );
					if( ( dVb + dMinR * ( dVc - dV ) ) < dMinValue ){
						dMinValue = dVb + dMinR * ( dVc - dV );
					}
				}
			}
		}
		
		return dMinValue;
	}
	
	public double interpolate( BeliefState bs ){
		return computeNewValue( bs );
	}
}
