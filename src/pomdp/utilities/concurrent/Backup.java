package pomdp.utilities.concurrent;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.utilities.ExecutionProperties;
import pomdp.utilities.Logger;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class Backup extends Task implements Runnable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Vector<BeliefState> m_vBeliefs;
	private LinearValueFunctionApproximation m_vValueFunction;
	private Vector<AlphaVector> m_vBackups;
	private int m_cObservations, m_cActions;
	private POMDP m_pPOMDP;
	
	public Backup( POMDP pomdp, LinearValueFunctionApproximation vValueFunction ){
		m_vBackups = new Vector<AlphaVector>();
		m_vValueFunction = vValueFunction;
		m_pPOMDP = pomdp;
		m_cObservations = m_pPOMDP.getObservationCount();
		m_cActions = m_pPOMDP.getActionCount();
		m_vBeliefs = new Vector<BeliefState>();
	}

	public void addBelief( BeliefState bs ){
		m_vBeliefs.add( bs );
	}
	
	protected AlphaVector backupGBased( BeliefState bs ){
		AlphaVector avMax = null, avCurrent = null;
		double dValue = 0.0, dMaxValue = Double.MAX_VALUE * -1;
						
		for( int iAction : m_pPOMDP.getRelevantActions( bs ) ){
			avCurrent = G( iAction, bs, m_vValueFunction );
			dValue = avCurrent.dotProduct( bs );
	
			//Logger.getInstance().logln( m_cBackups + ") backup: Action value, a = " + iAction + " v = " + dValue + " " + avCurrent );
			
			if( dValue >= dMaxValue ){
				dMaxValue = dValue;
				if( avMax != null )
					avMax.release();
				avMax = avCurrent;
			}
			else{
				avCurrent.release();
			}
		}

		avMax.setWitness( bs );
		bs.addBackup();
		
		return avMax;
	}

	protected AlphaVector G( int iAction, BeliefState bs, LinearValueFunctionApproximation vValueFunction ){
		AlphaVector avMax = null, avG = null, avSum = null, avMaxOriginal = null;;
		int iObservation = 0, iState = 0;
		Vector<AlphaVector> vVectors = new Vector<AlphaVector>( vValueFunction.getVectors() );
		double dMaxValue = Double.NEGATIVE_INFINITY, dValue = 0, dProb = 0.0, dSumProbs = 0.0;

		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = bs.probabilityOGivenA( iAction, iObservation );
			dSumProbs += dProb;
			if( dProb > 0.0 ){
				dMaxValue = Double.NEGATIVE_INFINITY;
				//argmax_i g^i_a,o \cdot b
				for( AlphaVector avAlpha : vVectors ){
					avG = avAlpha.G( iAction, iObservation );
					
					dValue = avG.dotProduct( bs );
					if( ( avMax == null ) || ( dValue > dMaxValue ) ){
						dMaxValue = dValue;
						avMax = avG;
						avMaxOriginal = avAlpha;
					}
				}
			}
			else{
				dMaxValue = 0.0;
				avMaxOriginal = vValueFunction.getLast();
				avMax = avMaxOriginal.G( iAction, iObservation );
			}

			if( avSum == null ){
				avSum = avMax.copy(); 
			}
			else if( avMax != null ){
				avSum.accumulate( avMax ); 
			}
			avMax = null;
		}
		
		
		AlphaVector avResult = avSum.addReward( iAction );
		avResult.setAction( iAction );
		avSum.release();
		return avResult;
	}
	
	protected AlphaVector backupTauBased( BeliefState bsCurrent ){
		AlphaVector avMax = null;
		double dValue = 0.0, dMaxValue = Double.NEGATIVE_INFINITY;
		int iAction = 0, iMaxAction = -1;
		
		AlphaVector[] aNext = null, aBest = null;
		
		Vector<AlphaVector[]> vWinners = new Vector<AlphaVector[]>();
		Vector<Integer> vWinnersActions = new Vector<Integer>();
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			aNext = new AlphaVector[m_cObservations];
			dValue = findMaxAlphas( iAction, bsCurrent, m_vValueFunction, aNext );
			if( dValue > dMaxValue ){
				dMaxValue = dValue;
				vWinners.clear();
				vWinnersActions.clear();
			}
			if( dValue == dMaxValue ){
				iMaxAction = iAction;
				vWinners.add( aNext );
				vWinnersActions.add( iMaxAction );
			}
		}
		
		int idx = 0;//RandomGenerator.nextInt( vWinners.size() );
		aBest = vWinners.elementAt( idx );
		iMaxAction = vWinnersActions.elementAt( idx );
		
		avMax = G( iMaxAction, m_vValueFunction, aBest );

		avMax.setWitness( bsCurrent );
		bsCurrent.addBackup();
		
		return avMax;
	}

	private double findMaxAlphas( int iAction, BeliefState bs, LinearValueFunctionApproximation vValueFunction, AlphaVector[] aNext ) {
		AlphaVector avAlpha = null;
		int iObservation = 0;
		double dSumValues = 0.0, dValue = 0, dProb = 0.0, dSumProbs = 0.0;
		BeliefState bsSuccessor = null;
		
		boolean bCache = m_pPOMDP.getBeliefStateFactory().isCachingBeliefStates();
		//m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );
		
		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			dProb = bs.probabilityOGivenA( iAction, iObservation );
			
			dSumProbs += dProb;
			if( dProb > 0.0 ){
				bsSuccessor = bs.nextBeliefState( iAction, iObservation );
				avAlpha = vValueFunction.getMaxAlpha( bsSuccessor );
				dValue = avAlpha.dotProduct( bsSuccessor );
				dSumValues += dValue * dProb;
			}
			else{
				avAlpha = vValueFunction.getLast();
			}
			
			aNext[iObservation] = avAlpha;
		}
		
		dSumValues /= dSumProbs; //in case due to rounding there is an error and probs do not exactly sum to 1
		dSumValues *= m_pPOMDP.getDiscountFactor();
		dSumValues += m_pPOMDP.immediateReward( bs, iAction ); 
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bCache );
		
		return dSumValues;
	}
	
	private AlphaVector G( int iAction, LinearValueFunctionApproximation vValueFunction, AlphaVector[] aNext ) {
		AlphaVector avAlpha = null, avG = null, avSum = null, avResult = null;
		int iObservation = 0;
		
		for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
			avAlpha = aNext[iObservation];
			avG = avAlpha.G( iAction, iObservation );
			
			if( avSum == null )
				avSum = avG.copy();
			else
				avSum.accumulate( avG );
		}
		
		avResult = avSum.addReward( iAction );
		avResult.setAction( iAction );

		avSum.release();
		return avResult;
	}


	
	public void run() {
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Started finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
		for( BeliefState bs: m_vBeliefs ){
			m_vBackups.add( backupGBased( bs ) );
		}
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Done finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
	}

	public void execute() {
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Started finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
		for( BeliefState bs: m_vBeliefs ){
			m_vBackups.add( backupGBased( bs ) );
		}
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Done finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
	}

	public int getResultsCount(){
		return m_vBackups.size();
	}
	public AlphaVector getResult( int iVector ){
		return m_vBackups.elementAt( iVector );
	}

	public BeliefState getBeliefState( int iBelief ){
		return m_vBeliefs.elementAt( iBelief );
	}

	@Override
	public void copyResults(Task processed) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Element getDOM(Document doc) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
}
