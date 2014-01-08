package pomdp.utilities.concurrent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.environments.POMDP;
import pomdp.utilities.AlphaVector;
import pomdp.utilities.BeliefState;
import pomdp.utilities.BeliefStateFactory;
import pomdp.valuefunction.LinearValueFunctionApproximation;

public class FindMaxAlphas extends Task implements Runnable {

	private int m_iAction;
	private BeliefState m_bsBelief;
	private LinearValueFunctionApproximation m_vValueFunction;
	private AlphaVector[] m_avNextVectors;
	private double m_dValue;
	
	public FindMaxAlphas( POMDP pomdp, int iAction, BeliefState bs, LinearValueFunctionApproximation vValueFunction ){
		m_pPOMDP = pomdp;
		m_iAction = iAction;
		m_bsBelief = bs.copy();
		m_vValueFunction = vValueFunction;
		m_avNextVectors = null;
		m_dValue = 0.0;
	}
		
	public FindMaxAlphas( Element eTask, POMDP pomdp ) throws Exception {
		m_pPOMDP = pomdp;
		setId( Integer.parseInt( eTask.getAttribute( "ID" ) ) );
		m_dValue = Double.parseDouble( eTask.getAttribute( "Value" ) );
		Element eChild = (Element) eTask.getChildNodes().item( 0 );
		m_bsBelief = m_pPOMDP.getBeliefStateFactory().parseDOM( eChild );
		eChild = (Element) eTask.getChildNodes().item( 1 );
		if( eChild.getNodeName().equals( "NextAlphaVectors" ) ){
			int cVectors = Integer.parseInt( eChild.getAttribute( "Count" ) ), iVector = 0;
			m_avNextVectors = new AlphaVector[cVectors];
			for( iVector = 0 ; iVector < cVectors ; iVector++ ){
				m_avNextVectors[iVector] = AlphaVector.parseDOM( (Element)eChild.getChildNodes().item( iVector ), m_pPOMDP );
			}
			m_vValueFunction = null;
		}
		else if( eChild.getNodeName().equals( "ValueFunction" ) ){
			m_avNextVectors = null;
			m_vValueFunction = new LinearValueFunctionApproximation();
			m_vValueFunction.parseDOM( eChild, m_pPOMDP );
		}
	}

	
	public Element getDOM( Document doc ) throws Exception {
		Element eTask = doc.createElement( "FindMaxAlphas" ), eValueFunction = null, eVectors = null, eVector = null;
		eTask.setAttribute( "ID", getId() + "" );
		eTask.setAttribute( "Value", m_dValue +"" );
		eTask.appendChild( m_bsBelief.getDOM( doc ) );
		if( m_vValueFunction != null ){
			eValueFunction = m_vValueFunction.getDOM( doc );
			eTask.appendChild( eValueFunction );
		}
		if( m_avNextVectors != null ){
			eVectors = doc.createElement( "NextAlphaVectors" );
			eVectors.setAttribute( "Count", m_avNextVectors.length + "" );
			for( AlphaVector av : m_avNextVectors ){
				eVector = av.getDOM( doc );
				eVectors.appendChild( eVector );
			}
			eTask.appendChild( eVectors );
		}
		return eTask;
	}

	
	public void run() {
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Started finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
		m_dValue = findMaxAlphas();	
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Done finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
	}
	public void execute() {
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Started finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
		m_dValue = findMaxAlphas();	
		//Logger.getInstance().log( "FindMaxAlpha", 0, "run", Thread.currentThread().getName() + " Done finding alphas, a = " + m_iAction + ", b = " + m_bsBelief.getSerialNumber() );
	}

	private double findMaxAlphas() {
		AlphaVector avAlpha = null;
		int iObservation = 0;
		double dSumValues = 0.0, dValue = 0, dProb = 0.0, dSumProbs = 0.0;
		BeliefState bsSuccessor = null;
		
		//if( m_pPOMDP.getBeliefStateFactory() == null )
		//	BeliefStateFactory.getInstance( m_pPOMDP );
		boolean bCache = m_pPOMDP.getBeliefStateFactory().isCachingBeliefStates();
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( false );

		m_avNextVectors = new AlphaVector[m_pPOMDP.getObservationCount()];	
		
		for( iObservation = 0 ; iObservation < m_pPOMDP.getObservationCount() ; iObservation++ ){
			dProb = m_bsBelief.probabilityOGivenA( m_iAction, iObservation );
			
			dSumProbs += dProb;
			if( dProb > 0.0 ){
				bsSuccessor = m_bsBelief.nextBeliefState( m_iAction, iObservation );
				avAlpha = m_vValueFunction.getMaxAlpha( bsSuccessor );
				dValue = avAlpha.dotProduct( bsSuccessor );
				dSumValues += dValue * dProb;
				/*
				Logger.getInstance().logln( "successor[" + iAction + "," + iObservation + "] = " + bsSuccessor );
				Logger.getInstance().logln( "alpha[" + iAction + "," + iObservation + "] = " + avAlpha );
				Logger.getInstance().logln( "alpha[" + iAction + "," + iObservation + "] = " + avAlpha );
				Logger.getInstance().logln( "alpha(b) = " + dValue );
				*/
			}
			else{
				avAlpha = m_vValueFunction.getLast();
			}
			
			m_avNextVectors[iObservation] = avAlpha;
			
			//Logger.getInstance().logln( "next[" + iAction + "," + iObservation + "] = " + avAlpha );
		}
		
		//if( diff( dSumProbs, 1.0 ) > 0.1 )
		//	Logger.getInstance().log( "VI", 0, "findMaxAlphas", cG + ") Sum of probabilities is " + dSumProbs );
		
		dSumValues /= dSumProbs; //in case due to rounding there is an error and probs do not exactly sum to 1
		dSumValues *= m_pPOMDP.getDiscountFactor();
		dSumValues += m_pPOMDP.immediateReward( m_bsBelief, m_iAction ); 
		
		m_pPOMDP.getBeliefStateFactory().cacheBeliefStates( bCache );
		
		m_vValueFunction = null;
		
		return dSumValues;
	}

	public double getValue(){
		return m_dValue;
	}
	public AlphaVector[] getNextVectors(){
		return m_avNextVectors;
	}
	public void copyResults( Task tProcessed ){
		m_avNextVectors = ((FindMaxAlphas)tProcessed).getNextVectors();		
	}
}
