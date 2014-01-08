/*
 * Created on May 6, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package pomdp.utilities;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.environments.FactoredPOMDP;
import pomdp.environments.POMDP;
import pomdp.utilities.factored.FactoredAlphaVector;
import pomdp.valuefunction.LinearValueFunctionApproximation;

/**
 * @author Guy Shani
 *
 * This class represents an Alpha vector, assigning a value for each state.
 */


public abstract class AlphaVector implements Serializable{
	protected BeliefState m_bsWitness;
	protected int m_cStates;
	protected int m_cActions;
	protected int m_cObservations;
	protected int m_iAction;
	protected double m_dMaxValue;
	protected double m_dAvgValue;
	protected AlphaVector[][] m_aCachedG;
	protected int m_iAge;
	protected long m_iID;
	protected double m_dOffset;
	protected Map m_mDotProductCache;
	protected int m_iValueFunctionInsertionTime;
	protected int m_cReferenceCounter;
	protected int m_cHitCount;
	//protected LinearValueFunctionApproximation m_vfContainer;
	protected POMDP m_pPOMDP;
	protected long[] m_aiSumIds;
	public boolean m_bMaintainWitness = true;
	
	protected static long s_cGComputations = 0;
	protected static long s_cDotProducts = 0;
	protected static long s_cCurrentDotProducts = 0;
	protected static long s_cApproximateDotProduct = 0;
	protected static long s_cAlphaVectors = 0;
	
	protected static long s_cTotalTimeInG = 0;
	protected static long s_cCurrentTimeInG = 0;
	protected static long s_cTotalTimeInDotProduct = 0;
	protected static long s_cCurrentTimeInDotProduct = 0;
	protected static long TIME_INTERVAL = 100;
	
	protected static boolean s_bAllowCaching = false;
	private static boolean s_bCountDotProduct = ExecutionProperties.getReportOperationTime();

	private boolean m_bDominated;

	/**
	 * @author Guy Shani
	 * 
	 * Constructor
	 * @param bsWitness - a witness belief state for the alpha vector: b is a witness for a if for each a'=/=a b*a<=b*a'
	 * @param iAction - the action associated with this belief state
	 * @param pomdp - the pomdp for which this alpha vector is designed
	 *
	 */
	public AlphaVector( BeliefState bsWitness, int iAction, POMDP pomdp ){
		m_bDominated = false;
		m_pPOMDP = pomdp;
		m_bsWitness = bsWitness;
		m_cStates = m_pPOMDP.getStateCount();
		m_cActions = m_pPOMDP.getActionCount();
		m_cObservations = m_pPOMDP.getObservationCount();
		m_iAction = iAction;
		m_aCachedG = new AlphaVector[m_cActions][m_cObservations];
		m_iAge = 0;
		m_iID = s_cAlphaVectors++;
		m_dOffset = 0.0;
		m_mDotProductCache = new TreeMap();
		m_iValueFunctionInsertionTime = 0;
		m_cReferenceCounter = 0;
		//m_vfContainer = null;
		m_dMaxValue = 0.0;
		m_dAvgValue = 0.0;
		m_aiSumIds = new long[2];
		setSumIds( -1, -1 );
	}
	
	/**
	 * @author Guy Shani
	 * 
	 * valueAt returns the value the alpha vector assigns to a specific state
	 * @param iState - index of the state
	 *
	 */
	public abstract double valueAt( int iState );
	/**
	 * @author Guy Shani
	 * 
	 * sets the value the alpha vector assigns to a specific state
	 * @param iState - index of the state
	 * @param dValue - value of state iState
	 *
	 */
	public abstract void setValue( int iState, double dValue );
	
	/**
	 * @author Guy Shani
	 * 
	 * reduces the values of all states by delta
	 * @param dDelta - change in values
	 */
	public void decay( double dDelta ){
		m_dOffset += dDelta;
	}
	
	/**
	 * @author Guy Shani
	 * 
	 * returns the action assigned to this alpha vector
	 */
	public int getAction(){
		return m_iAction;
	}
	/**
	 * @author Guy Shani
	 * 
	 * sets the action assigned to this alpha vector
	 * @param iAction - valid action index
	 */
	public void setAction( int iAction ){
		m_iAction = iAction;
	}
	
	
	/**
	 * @author Guy Shani
	 * 
	 * Computes the inner product of an alpha vector and a belief state. 
	 * \sum_s b(s)alpha(s)
	 * Computation is efficiently by iterating only over the non-zero entries. 
	 * @param bs - belief state, must have the same dimension as the alpha vector (unchecked)
	 */
	public double dotProduct( BeliefState bs ){
		/* 
		 * \sum_s b(s)alpha(s)
		 */
		int iState = 0;
		double dValue = 0.0, dProb = 0.0, dSum = 0.0;
				
		if( bs == null )
			return 0.0;
		
		long lTimeBefore = 0, lTimeAfter = 0;
		if( ExecutionProperties.getReportOperationTime() )
			lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
		
		int cBeliefNonZeroEntries = bs.getNonZeroEntriesCount();
		int cAlphaNonZeroEntries = getNonZeroEntriesCount();
		Iterator it = bs.getNonZeroEntries().iterator();
		Entry e = null;
		
		if( cBeliefNonZeroEntries < cAlphaNonZeroEntries ){
			it = bs.getNonZeroEntries().iterator();
			if( it != null ){
				while( it.hasNext() ){
					e = (Entry) it.next();
					iState = ((Integer) e.getKey()).intValue();
					dProb = ((Double) e.getValue()).doubleValue();
					dValue = valueAt( iState );
					dSum += dValue * dProb;
				}
			}
		}
		else{
			it = getNonZeroEntries();
			if( it != null ){
				while( it.hasNext() ){
					e = (Entry) it.next();
					iState = ((Integer) e.getKey()).intValue();
					dValue = ((Double) e.getValue()).doubleValue();
					dProb = bs.valueAt( iState );
					dSum += dValue * dProb;
				}
			}			
		}
		
		if( it == null ){
			for( iState = 0 ; iState < m_cStates ; iState++ ){
				dProb = bs.valueAt( iState );
				dValue = valueAt( iState );
				dSum += dValue * dProb;
			}
		}

		if( s_bCountDotProduct ){
			s_cDotProducts++;
			s_cCurrentDotProducts++;
			if( ExecutionProperties.getReportOperationTime() ){
				lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
				s_cCurrentTimeInDotProduct += ( lTimeAfter - lTimeBefore ) / 1000;
				s_cTotalTimeInDotProduct += ( lTimeAfter - lTimeBefore ) / 1000;
				/*
				if( s_cCurrentDotProducts == ( TIME_INTERVAL * 100 ) ){
					String sMsg = "After " + s_cDotProducts + " dot product - avg time = " + s_cCurrentTimeInDotProduct / ( TIME_INTERVAL * 100 );
					s_cCurrentTimeInDotProduct = 0;
					s_cCurrentDotProducts = 0;
					Logger.getInstance().log( "AlphaVector", 0, "dotProduct", sMsg );
				}
				*/
			}
		}


		return dSum;
	}
	
	protected double round( double d, int cDigits ){
		int power = (int)Math.pow( 10, cDigits );
		int num = (int)Math.round( d * power );
		return ( 1.0 * num ) / power;
	}


	/**
	 * @author Guy Shani
	 * 
	 * computes an approximation of the inner product of an alpha vector and a belief state.
	 * Approximation is by using only the dominating subset of the belief state entries. 
	 * Computation is efficiently by iterating only over the non-zero entries. 
	 * @param bs - belief state, must have the same dimension as the alpha vector (unchecked)
	 */
	public double approximateDotProduct( BeliefState bs ){
		int iState = 0;
		double dValue = 0.0, dProb = 0.0, dSum = 0.0;
		
		if( bs == null )
			return 0.0;
		
		if( s_bCountDotProduct )
			s_cApproximateDotProduct++;
		Iterator it = bs.getDominatingNonZeroEntries();
		Entry e = null;
		
		while( it.hasNext() ){
			e = (Entry) it.next();
			iState = ((Integer) e.getKey()).intValue();
			dProb = ((Double) e.getValue()).doubleValue();
			dValue = valueAt( iState );
			dSum += dValue * dProb;
		}
		return dSum;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Returns the witness of this alpha vector. 
	 */
	public BeliefState getWitness(){
		return m_bsWitness;
	}
	/**
	 * @author Guy Shani
	 * 
	 * Sets the witness for this alpha vector. 
	 * @param bsWitness - a new witness.
	 */
	public void setWitness( BeliefState bsWitness ){
		if( m_bMaintainWitness )
			m_bsWitness = bsWitness;
	}
	
	
	/**
	 * @author Guy Shani
	 * 
	 * Moves the alpha vector values by dOffset. 
	 * Similar to decay, but less efficient, because it changes the values of all states.
	 * @param dOffset - change in values.
	 */
	public void translate( double dOffset ){
		double dValue = 0.0;
		
		for( int iState : m_pPOMDP.getValidStates() ){
			dValue = valueAt( iState );
			setValue( iState, dValue + dOffset );
		}
	}
	
	/**
	 * @author Guy Shani
	 * 
	 * Changes the alpha vector values by multiplying them by dScale. 
	 * @param dScale - change in values.
	 */
	public void scale( double dScale ){
		double dValue = 0.0;
		
		for( int iState : m_pPOMDP.getValidStates() ){
			dValue = valueAt( iState );
			setValue( iState, dValue * dScale );
		}
	}

	/**
	 * @author Guy Shani
	 * 
	 * Checks whether this alpha vector pointwise dominates avOther 
	 * @param avOther - another alpha vector.
	 */
	public boolean dominates( AlphaVector avOther ){
		double dValue = 0.0, dOtherValue = 0.0;
		
		if( getMaxValue() < avOther.getMaxValue() )
			return false;
		
		if( getAvgValue() < avOther.getAvgValue() )
			return false;
		
		for( int iState : m_pPOMDP.getValidStates() ){
			dValue = valueAt( iState );
			dOtherValue = avOther.valueAt( iState );
			if( dOtherValue > dValue )
				return false;
		}
		return true;
	}
	
	private static int g_cGs = 0;
	private static int g_cTouchedVertexes = 0;
	//g(s) = \sum_s' O(a,s',o) tr( s,a,s') \alpha(s')
	protected synchronized AlphaVector computeG( int iAction, int iObservation ){
		int iStartState = 0, iEndState = 0, cNonZeroEntries = 0;
		double dObservation = 0.0, dTr = 0.0, dValue = 0.0, dSum = 0.0;

		AlphaVector avResult = newAlphaVector();
		avResult.setAction( iAction );

		Iterator<Entry<Integer,Double>> itNonZeroEntries = null;
		Entry<Integer,Double> eValue = null;
		
		g_cGs++;
		
		for( iStartState = 0 ; iStartState < m_cStates ; iStartState++ ){
			dSum = 0.0;
			//itNonZeroEntries = getNonZeroEntries();
			itNonZeroEntries = m_pPOMDP.getNonZeroTransitions( iStartState, iAction );
			
			while( itNonZeroEntries.hasNext() ){
				eValue = itNonZeroEntries.next();
				iEndState = eValue.getKey();
				//dValue = (Double)eValue.getValue();
				//dTr = m_pPOMDP.tr( iStartState, iAction, iEndState );
				dValue = valueAt( iEndState );
				dTr = eValue.getValue();
				g_cTouchedVertexes++;
				if( dValue != 0 ){
					dObservation = m_pPOMDP.O( iAction, iEndState, iObservation );
					dSum += dObservation * dTr * dValue;
				}
			}
			
			if( dSum != 0 ){
				avResult.setValue( iStartState, dSum );
				cNonZeroEntries++;
			}
		}
		avResult.finalizeValues();
		return avResult;
	}
	
	/* 
	 * g_{a,o}(s) = \sum_s' O(a,s',o)tr(s,a,s')alpha(s')
	 */
	/**
	 * @author Guy Shani
	 * 
	 * Computes the a new alpha vector by the G(a,o) operation of the point-based backup (following the Perseus convention).
	 * g(s) = \sum_s' O(a,s',o) tr( s,a,s') \alpha(s')
	 * Caching results to avoid recomputation. Caching may cause memory problems in larger POMDPs. 
	 * @param n - a valid action index.
	 * @param iObservation - a valid observation index.
	 */
	public AlphaVector G( int iAction, int iObservation ){
		if( s_bAllowCaching && ( m_aCachedG[iAction][iObservation] != null ) )
			return m_aCachedG[iAction][iObservation];
		
		long lTimeBefore = 0, lTimeAfter = 0;
		if( ExecutionProperties.getReportOperationTime() )
			lTimeBefore = JProf.getCurrentThreadCpuTimeSafe();
	
		AlphaVector avResult = computeG( iAction, iObservation );
		if( s_bAllowCaching )
			m_aCachedG[iAction][iObservation] = avResult;
		
		
		s_cGComputations++;
		
		if( ExecutionProperties.getReportOperationTime() ){
			lTimeAfter = JProf.getCurrentThreadCpuTimeSafe();
			
			s_cCurrentTimeInG += ( lTimeAfter - lTimeBefore ) / 1000000;
			s_cTotalTimeInG += ( lTimeAfter - lTimeBefore ) / 1000000;
			/*
			if( s_cGComputations % TIME_INTERVAL  == 0 ){
				Logger.getInstance().log( "AlphaVector", 0, "G", "avg time for computing G " + s_cCurrentTimeInG / ( TIME_INTERVAL ) );
				s_cCurrentTimeInG = 0;
			}
			*/			
		}
		return avResult;
	}
	
	/**
	 * @author Guy Shani
	 * 
	 * Adds the values of another alpha vector to the current alpha vector (pointwise). 
	 * @param av - alpha vector whose values are added to this alpha vector.
	 */
	public abstract void accumulate( AlphaVector av );

	/**
	 * @author Guy Shani
	 * 
	 * Creates a copy of the current alpha vector. 
	 */
	public AlphaVector copy(){
		AlphaVector avCopy = newAlphaVector();
			
		int iState = 0;
		double dValue = 0.0;
		
		Iterator itNonZero = getNonZeroEntries();
		if( itNonZero != null ){

			Pair p = null;
			Map.Entry e = null;
			Object oElement = null;
			
			while( itNonZero.hasNext() ){
				oElement = itNonZero.next();
				if( oElement instanceof Pair ){
					p = (Pair) oElement;
					iState = ((Number)p.m_first).intValue();
					dValue = ((Number)p.m_second).doubleValue();
					avCopy.setValue( iState, dValue );
				}
				else if( oElement instanceof Map.Entry ){
					e = (Map.Entry) oElement;
					iState = ((Number)e.getKey()).intValue();
					dValue = ((Number)e.getValue()).doubleValue();
					avCopy.setValue( iState, dValue );
				}
			}
		}
		else{
			for( int iValidState : m_pPOMDP.getValidStates() ){
				dValue = valueAt( iValidState );
				avCopy.setValue( iValidState, dValue );
			}
		}
		
		return avCopy;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Adds a value to a specific state. 
	 * @param iState - state index.
	 * @param dDeltaValue - change in value.
	 */
	public void addValue( int iState, double dDeltaValue ){
		setValue( iState, valueAt( iState ) + dDeltaValue );		
	}

	/**
	 * @author Guy Shani
	 * 
	 * Sum of all the values in the alpha vector. 
	 */
	public double sumValues() {
		double dSum = 0.0;
		for( int iState : m_pPOMDP.getValidStates() ){
			dSum += valueAt( iState );
		}
		return dSum;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Increases the age of the alpha vector.
	 * Age is used to remove alpha vectors that were created over some time threshold, and might be over the maximal possible values.
	 */
	public void age() {
		m_iAge++;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Returns the age of the alpha vector.
	 * Age is used to remove alpha vectors that were created over some time threshold, and might be over the maximal possible values.
	 */
	public int getAge() {
		return m_iAge;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Sets the counting of dot products. 
	 * @param bCount - true: count dot products, false: do not count dot products.
	 * 
	 */
	public static void countDotProduct( boolean bCount ){
		s_bCountDotProduct  = bCount;
	}
	
	/**
	 * @author Guy Shani
	 * 
	 * Checks whether currently counting dot products.. 
	 * 
	 */
	public static long dotProductCount(){
		return s_cDotProducts;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Returns the number of approximated dot products. 
	 * 
	 */
	public static long dotApproximateProductCount(){
		return s_cApproximateDotProduct;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Clears the dot products counter. 
	 * 
	 */
	public static void clearDotProductCount(){
		s_cDotProducts = 0;
		s_cApproximateDotProduct = 0;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Checks whether G operations caching is permitted.
	 * 
	 */
	public static boolean allowCaching() {
		return s_bAllowCaching;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Sets the caching of G operation results on and off.
	 * @param bAllowCaching - true: caching is on, false: caching is off.
	 * 
	 */
	public static void setAllowCaching( boolean bAllowCaching ){
		s_bAllowCaching = bAllowCaching;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Returns an iterator over the non zero entries of the alpha vector.
	 * 
	 */
	public abstract Iterator<Entry<Integer,Double>> getNonZeroEntries();
	
	/**
	 * @author Guy Shani
	 * 
	 * Finalizes the values of the belief state. Should be called after all values are final.
	 * 
	 */
	public abstract void finalizeValues();
	
	/**
	 * @author Guy Shani
	 * 
	 * Returns a string representation of this alpha vector.
	 * 
	 */
	public String toString(){
		String sVector = "AV", sValue = "";
		int iEndState = 0;
		double dValue = -1.0, dNextValue = -1.0;
		int cEntries = 0;
		
		sVector += m_iID;
		sVector += "(a=" + m_iAction + "[";
		
		for( int iState = 0 ; iState < m_cStates ; iState++ ){
		//for( int iState : m_pPOMDP.getValidStates() ){
			dValue = valueAt( iState );
			if( dValue != 0.0 ){
				cEntries++;
				
				iEndState = iState + 1;
				if( iState < m_cStates - 1 ){
					dNextValue = valueAt( iEndState );
					while( ( iEndState < m_cStates - 1 ) && ( dValue == dNextValue ) ){
						iEndState++;
						dNextValue = valueAt( iEndState );
					}
				}
				
				sValue = dValue + "";
				if( sValue.length() > 6 )
					sValue = sValue.substring( 0, 4 );
				
				if( iEndState == iState + 1 )
					sVector += iState + "=" + sValue + ",";
				else{
					sVector += "(" + iState + "-" + ( iEndState - 1 ) + ")=" + sValue + ",";
					iState = iEndState - 1;
				}
			}
		}
		if( cEntries > 0 )
			sVector = sVector.substring( 0, sVector.length() - 1 ) + "]";
		else
			sVector += "]";
		sVector += " W=" + m_bsWitness + ")";
		return sVector;
	}

	/**
	 * @author Guy Shani
	 * 
	 * Returns the id of this alpha vector. 
	 * 
	 */
	public long getId(){
		return m_iID;
	}
	/**
	 * @author Guy Shani
	 * 
	 * Returns the time when this alpah vector was inserted to the value function.
	 * 
	 */
	public int getInsertionTime(){
		return m_iValueFunctionInsertionTime;
	}
	
	/**
	 * @author Guy Shani
	 * 
	 * Sets the time when this alpah vector was inserted to the value function.
	 * @param iTime - absolute time of the value function.
	 */
	public void setInsertionTime( int iTime ){
		m_iValueFunctionInsertionTime = iTime;
	}
	
	
	/**
	 * @author Guy Shani
	 * 
	 * Sets the containing value function.
	 */
	public void setContainer( LinearValueFunctionApproximation vfContainer ){
		//m_vfContainer = vfContainer;
	}
	
	public AlphaVector productTrA( int iAction ){
		AlphaVector avResult = newAlphaVector();
		int iState = 0, iStartState = 0;
		double dValue = 0, dTr = 0.0, dPreviousValue = 0.0, dNewValue = 0.0;
		Iterator itAlphaVectorNonZero = getNonZeroEntries();
		Iterator itTrNonZero = null;
		Entry e = null;
		Pair p = null;
		Object o = null;
		
		while( itAlphaVectorNonZero.hasNext() ){
			o = itAlphaVectorNonZero.next();
			if( o instanceof Entry ){
				e = (Entry)o;
				iState = ((Number) e.getKey()).intValue();
				dValue = ((Number) e.getValue()).doubleValue();
			}
			else if( o instanceof Pair ){
				p = (Pair)o;
				iState = ((Number) p.m_first).intValue();
				dValue = ((Number) p.m_second).doubleValue();
			}
			
			for( iStartState = 0 ; iStartState < m_cStates ; iStartState++ ){
				dTr = m_pPOMDP.tr( iStartState, iAction, iState );
				dPreviousValue = avResult.valueAt( iStartState );
				dNewValue = dPreviousValue + dTr * dValue;
				avResult.setValue( iStartState, dNewValue );
			}
			
		}
		
		return avResult;
	}
			
	public abstract AlphaVector newAlphaVector();
	
	public double getMaxValue(){
		return m_dMaxValue;
	}
	public double getMinValue(){
		double dMinValue = 0.0;
		Iterator<Entry<Integer, Double>> itNonZero = getNonZeroEntries();
		Entry<Integer, Double> e = null;
		while( itNonZero.hasNext() ){
			e = itNonZero.next();
			if( e.getValue() < dMinValue )
				dMinValue = e.getValue();
		}
		return dMinValue;
	}
	
	public double getAvgValue(){
		return m_dAvgValue;
	}
	
	
	public static AlphaVector parseDOM( Element eVector, POMDP pomdp ) throws Exception{
		AlphaVector avNew = null;
		if( pomdp instanceof FactoredPOMDP ){
			avNew = new FactoredAlphaVector( (FactoredPOMDP)pomdp );			
		}
		else{
			avNew = new TabularAlphaVector( null, 0.0, pomdp );			
		}

		avNew.setAction( Integer.parseInt( eVector.getAttribute( "Action" ) ) );
		
		avNew.parseValuesXML( eVector );

		return avNew;
	}
	
	public Element getDOM( Document docValueFunction ) throws Exception{
		Element eVector = docValueFunction.createElement( "AlphaVector" );
		Element eState = null;
		
		Iterator<Entry<Integer,Double>> itAlphaVectorNonZero = getNonZeroEntries();
		Entry<Integer,Double> e = null;
		int iState = 0, cNonZeroStates = 0;
		double dValue = 0.0;
		
		if( itAlphaVectorNonZero != null ){
			while( itAlphaVectorNonZero.hasNext() ){
				e = itAlphaVectorNonZero.next();
				iState = e.getKey();
				dValue = e.getValue();
				eState = docValueFunction.createElement( "State" );
				eState.setAttribute( "Id", iState + "" );
				eState.setAttribute( "Value", dValue + "" );
				
				eVector.appendChild( eState );
			}
			cNonZeroStates = getNonZeroEntriesCount();
		}
		else{
			for( iState = 0 ; iState < m_cStates ; iState++ ){
				dValue = valueAt( iState );
				if( dValue != 0.0 ){
					eState = docValueFunction.createElement( "State" );
					eState.setAttribute( "Id", iState + "" );
					eState.setAttribute( "Value", dValue + "" );
					
					eVector.appendChild( eState );	
					
					cNonZeroStates++;
				}
			}
		}
				
		eVector.setAttribute( "Id", m_iID + "" );
		eVector.setAttribute( "EntriesCount", cNonZeroStates + "" );
		eVector.setAttribute( "Action", m_iAction + "" );
		eVector.setAttribute( "Type", "Flat" );

		return eVector;
	}
	
	public boolean equals( AlphaVector avOther ){
		double dValue = 0.0, dOtherValue = 0.0;
		for( int iState : m_pPOMDP.getValidStates() ){
			dValue = valueAt( iState );
			dOtherValue = avOther.valueAt( iState );
			if( diff( dValue, dOtherValue ) > 0.001 )
				return false;
		}
		return true;
	}
	
	public double diff( AlphaVector avOther ){
		double dValue = 0.0, dOtherValue = 0.0;
		double dMaxDiff = 0.001;
		for( int iState : m_pPOMDP.getValidStates() ){
			dValue = valueAt( iState );
			dOtherValue = avOther.valueAt( iState );
			if( diff( dValue, dOtherValue ) > dMaxDiff )
				dMaxDiff = diff( dValue, dOtherValue );
		}
		return dMaxDiff;
	}
	
	private double diff( double dValue, double dOtherValue ){
		if( dValue > dOtherValue )
			return dValue - dOtherValue;
		else
			return dOtherValue - dValue;
	}
	
	public boolean equals( Object oOther ){
		if( oOther instanceof AlphaVector ){
			AlphaVector avOther = (AlphaVector)oOther;
			return equals( avOther );
		}
		return false;
	}
	
	public abstract int getNonZeroEntriesCount();
	
	
	public void setSumIds( long id1, long id2 ){
		m_aiSumIds[0] = id1;
		m_aiSumIds[1] = id2;
	}

	public long[] getSumIds(){
		return m_aiSumIds;
	}
	public static long getGComputationsCount() {
		return s_cGComputations;
	}
	public static double getAvgGTime(){
		return ( ( s_cTotalTimeInG * 1.0 ) / s_cGComputations );
	}
	public static double getAvgDotProductTime(){
		return ( ( s_cTotalTimeInDotProduct * 1.0 ) / s_cDotProducts );
	}
	
	public AlphaVector addReward( int iAction ){
		AlphaVector avResult = newAlphaVector();
		double dValue = 0.0;
		for( int iState : m_pPOMDP.getValidStates() ){
			dValue = valueAt( iState ) * m_pPOMDP.getDiscountFactor() + m_pPOMDP.R( iState, iAction );
			avResult.setValue( iState, dValue );
		}
		avResult.finalizeValues();
		return avResult;
	}
	public void setAllValues( double dValue ) {
		for( int iState : m_pPOMDP.getValidStates() ){
			setValue( iState, dValue );
		}		
	}
	public double dotProductMax( BeliefState bs, double dMaxValue ){
		Iterator<Entry<Integer,Double>> itNonZero = bs.getNonZeroEntries().iterator();
		Entry<Integer,Double> e = null;
		double dRemainingProb = 1.0, dBelief = 0.0, dValue = 0.0, dDotProductValue = 0.0;
		int iState = 0;
				
		while( itNonZero.hasNext() && ( dDotProductValue + dRemainingProb * m_dMaxValue > dMaxValue) ){
			e = itNonZero.next();
			iState = e.getKey();
			dBelief = e.getValue();
			dValue = valueAt( iState );
			dDotProductValue += dBelief * dValue;
			dRemainingProb -= dBelief;
		}
		if( itNonZero.hasNext() )
			return dMaxValue;
		return dDotProductValue;
	}
	
	public int countEntries(){
		int iAction = 0, iObservation = 0;
		int cEntries = 0;
		for( iAction = 0 ; iAction < m_cActions ; iAction++ ){
			for( iObservation = 0 ; iObservation < m_cObservations ; iObservation++ ){
				if( m_aCachedG[iAction][iObservation] != null )
					cEntries += m_aCachedG[iAction][iObservation].countEntries();
			}
		}
		cEntries += countLocalEntries();
		return cEntries;
	}
	public abstract long countLocalEntries();
	
	public void release(){
		int iAction = 0, iObservation = 0;
		if( m_aCachedG != null ){
			for( iAction = 0 ; iAction < m_aCachedG.length ; iAction++ ){
				if( m_aCachedG[iAction] != null ){
					for( iObservation = 0 ; iObservation< m_cObservations ; iObservation++ ){
						if( m_aCachedG[iAction][iObservation] != null )
							m_aCachedG[iAction][iObservation].release();
					}
				}
			}
		}
	}
	
	public void initHitCount(){
		m_cHitCount = 0;
	}
	public void incrementHitCount(){
		m_cHitCount++;
	}
	public int getHitCount(){
		return m_cHitCount;
	}

	public void parseValuesXML( Element eFunction ){
		int iStateItem = 0, iState = 0;
		double dValue = 0;
		Element eState = null;
		NodeList nlStates = eFunction.getChildNodes();
		for( iStateItem = 0 ; iStateItem < nlStates.getLength() ; iStateItem++ ){
			eState = (Element)nlStates.item( iStateItem );
			dValue = Double.parseDouble( eState.getAttribute( "Value" ) );
			iState = Integer.parseInt( eState.getAttribute( "Id" ) );
			setValue( iState, dValue );
		}
		finalizeValues();
	}

	public void getDOM( Element eFunction, Document doc ){
		Element eState = null;
		double dValue = 0;
		for( int iState : m_pPOMDP.getValidStates() ){
			eState = doc.createElement( "State" );
			dValue = valueAt( iState );
			//iAction = (int)m_ivBestActions.elementAt( iState );
			eState.setAttribute( "Id", iState + "" );
			eState.setAttribute( "Value", dValue + "" );
			eFunction.appendChild( eState );
		}
	}

	public abstract long size();
	public abstract void setSize( int cStates );
	
	public static void initCurrentDotProductCount(){
		s_cCurrentDotProducts = 0;
		s_cCurrentTimeInDotProduct = 0;
	}
	public static double getCurrentDotProductAvgTime(){
		return s_cCurrentTimeInDotProduct / (double)s_cCurrentDotProducts;
	}

	public String getXML(){
		String sXML = "<AlphaVector>";
		Iterator<Entry<Integer,Double>> it = getNonZeroEntries();
		Entry<Integer,Double> e = null;
		while( it.hasNext() ){
			e = it.next();
			sXML += "<Entry>" + e.getKey() + " " + e.getValue() + "</Entry>";
		}
		sXML += "</AlphaVector>";
		return sXML;
	}

	public void detach() {
		m_pPOMDP = null;
		//m_vfContainer = null;
	}

	public void attach( POMDP pomdp ) {
		m_pPOMDP = pomdp;
		m_cStates = m_pPOMDP.getStateCount();
		m_cActions = m_pPOMDP.getActionCount();
		m_cObservations = m_pPOMDP.getObservationCount();
	}

	public int getStateCount() {
		return m_cStates;
	}

	public POMDP getPOMDP() {
		return m_pPOMDP;
	}

	public double[] toArray() {
		double[] adValues = new double[m_cStates];
		int iState = 0;
		for( iState = 0 ; iState < m_cStates ; iState++ )
			adValues[iState] = valueAt( iState );
		return adValues;
	}

	public double dotProduct( double[] adBelief ) {
		int iState = 0;
		double dValue = 0.0;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			if( adBelief[iState] > 0.0 )
				dValue += adBelief[iState] * valueAt( iState );
		}
		return dValue;
	}

	public void setDominated( boolean bDominated ) {
		m_bDominated = bDominated;
	}
	public boolean isDominated(){
		return m_bDominated;
	}
	
	Vector<double[]> m_vWitnesses = new Vector<double[]>();

	public void addWitness( double[] adBelief ) {
		setDominated( false );
		m_vWitnesses.add( adBelief );		
	}
	public void clearWitnesses() {
		m_vWitnesses.clear();		
	}

	public int countWitnesses() {
		int cWitnesses = m_vWitnesses.size();
		//m_vWitnesses.clear();
		return cWitnesses;
	}
	public Collection<double[]> getWitnesses(){
		return m_vWitnesses;
	}
}
