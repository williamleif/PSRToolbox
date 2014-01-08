package pomdp.algorithms.online;

import java.util.Iterator;
import java.util.Vector;

import pomdp.algorithms.pointbased.IterativeValueIteration;
import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.valuefunction.LinearValueFunctionApproximation;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class SimpleOnlineValueIteration extends IterativeValueIteration {

	protected long m_cImprovements;
		
	public SimpleOnlineValueIteration( POMDP pomdp, 
								double dEpsilon, LinearValueFunctionApproximation vOptimizedValueFunction, Vector vBeliefPoints, boolean bPersistQValues ){
		super( pomdp, dEpsilon, vOptimizedValueFunction, vBeliefPoints, bPersistQValues );
		m_bAlwaysBackup = true;
		m_cImprovements = 0;
	}

	protected double improve( BeliefState bsCurrent, boolean bForceBackup ){
		AlphaVector avMax = getMaxAlpha( bsCurrent );
		double dCurrentValue = 0.0;
		double dNextValue = 0.0;
		AlphaVector avNext = null;
		
		if( avMax != null )
			dCurrentValue = avMax.dotProduct( bsCurrent );
		
		avNext = backup( bsCurrent );
		dNextValue = avNext.dotProduct( bsCurrent );

 		if( dNextValue > dCurrentValue + m_dEpsilon ){
			add( avNext );
			m_cImprovements++;
		}
 		else{
 			BeliefState bsWitness = avMax.getWitness();
 			//AlphaVector avBackup = backup( bsWitness );
 			//double dExpectedValue = avMax.dotProduct( bsWitness );
 			//double dBackupValue = avBackup.dotProduct( bsWitness );
 			double dExpectedValue = avMax.dotProduct( bsWitness );
 			double dBackupValue = computeBackupValue( bsWitness, false );
 			//double dBackupValue = avBackup.dotProduct( bsCurrent );
 			//double d = computeBackupValue( bsWitness );
 			if( dBackupValue + m_dEpsilon < dExpectedValue ){
 				avMax.decay( dExpectedValue - dBackupValue  + m_dEpsilon );
 				//m_vValueFunction.remove( avMax );
 				//if( m_vValueFunction.size() == 0 )
 				//	initValueFunctionToMin();
 				improve( bsWitness, true );
 				//decayValueFunction( 1 );
 				//avMax.decay( dExpectedValue - dBackupValue  + m_dEpsilon );
 				
 				/*
 				Logger.getInstance().logln( "Witness " + bsWitness + " (" + dBackupValue + 
 						") does not support the vector (" + dExpectedValue + ")" );
 					*/	
 			}
 		}
		return dNextValue - dCurrentValue;
	}
	
	protected boolean dominated( AlphaVector av, double dEpsilon ){
		double dNewValue = 0.0, dMaxValue = 0.0;
		boolean bDominated = true;
		
		int iState = 0;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dNewValue = av.valueAt( iState );
			dMaxValue = m_avMaxValues.valueAt( iState );
			if( dNewValue >= dMaxValue + dEpsilon ){ // >= so that we can pass in eps=0
				m_avMaxValues.setValue( iState, dNewValue );
				bDominated = false;				
			}
		}
		return bDominated;
	}
	
	
	protected String getDominance(){
		Iterator it = m_vValueFunction.iterator();
		AlphaVector av = null;
		int iState = 0;
		double dValue = 0.0, dMaxValue = 0.0;
		String sResult = "";
		
		while( it.hasNext() ){
			av = (AlphaVector)it.next();
			sResult += "\n(" + av.getAction() + "[";
			for( iState = 0 ; iState < m_cStates ; iState++ ){
				dValue = av.valueAt( iState );
				dMaxValue = m_avMaxValues.valueAt( iState );
				if( dValue >= dMaxValue ){
					sResult += iState + "=" + round( dValue, 2 ) + ",";
				}
			}
			sResult += "]";
		}
		return sResult;
	}
	
	public void valueIteration( int cMaxSteps, double dEpsilon, double dTargetValue ){
	}

	@Override
	public void valueIteration(int maxSteps, double epsilon,
			double targetValue, int maxRunningTime, int numEvaluations) {
		throw new NotImplementedException();		
	}

}
