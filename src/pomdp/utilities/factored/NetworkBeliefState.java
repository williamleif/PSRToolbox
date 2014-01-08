package pomdp.utilities.factored;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import pomdp.utilities.BeliefState;
import pomdp.utilities.Logger;
import pomdp.utilities.TabularBeliefState;

public class NetworkBeliefState extends TabularBeliefState {

	protected double[] m_adWorkingProbability;
	protected int m_cMachines;
	
	public NetworkBeliefState( int cMachines, int cStates, int cActions, int cObservations, int id, boolean bSparse, boolean bCacheBelifStates ) {
		super(cStates, cActions, cObservations, id, bSparse, bCacheBelifStates, null);
		m_aStateProbabilities = null;
		m_mNonZeroEntries = null;
		m_cMachines = cMachines;
		m_adWorkingProbability = new double[m_cMachines];
	}
	
	protected void initStateProbabilities(){
		int iState = 0;
		double dProb = 0.0, dSum = 0.0;
		m_aStateProbabilities = new double[m_cStates];
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			dProb = computeStateValue( iState );
			m_aStateProbabilities[iState] = dProb;
			dSum += dProb;
		}
		if( diff( dSum, 1.0 ) > 0.001 ){
			Logger.getInstance().logln("BUGBUG initStateProbabilities - sum prob is " + dSum );
			/*
			dSum = 0;
			for( iState = 0 ; iState < m_cStates ; iState++ ){
				dProb = computeStateValue( iState );
				m_aStateProbabilities[iState] = dProb;
				dSum += dProb;
			}
			*/
		}
	}
	
	protected double computeStateValue( int iState ){
		double dProb = 1.0;
		int iMachine = 0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			if( ( iState % 2 ) == 1 ) //machine working
				dProb *= m_adWorkingProbability[iMachine];
			else //machine down
				dProb *= ( 1.0 - m_adWorkingProbability[iMachine] );
			iState /= 2;
		}
		return dProb;
	}
	
	public double valueAt( int iState ){
		if( m_aStateProbabilities == null )
			initStateProbabilities();
		return m_aStateProbabilities[iState];
	}
	
	public double getMachineWorkingProb( int iMachine ){
		return m_adWorkingProbability[iMachine];
	}
	
	public void setMachineWorkingProb( int iMachine, double dProb ){
		m_adWorkingProbability[iMachine] = dProb;
	}
	
	protected double diff( double d1, double d2 ){
		if( d1 > d2 )
			return d1 - d2;
		return d2 - d1;
	}
	
	public boolean equals( Object oOther ){
		if( oOther instanceof NetworkBeliefState ){
			NetworkBeliefState nbs = (NetworkBeliefState)oOther;
			int iMachine = 0;
			for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
				if( diff( getMachineWorkingProb( iMachine ), nbs.getMachineWorkingProb( iMachine ) ) > 0.0001 )
					return false;
			}
			return true;
		}
		return false;
	}
	
	public String toString(){
		String sOutput = "BS" + getId() + "[";
		int iMachine = 0;
		for( iMachine = 0 ; iMachine < m_cMachines ; iMachine++ ){
			sOutput += getMachineWorkingProb( iMachine ) + ", ";
		}
		sOutput += "]";
		return sOutput;
	}
	
	public double probabilityOGivenA( int iAction, int iObservation ){
		if( iAction == m_cActions - 1 )//do-nothing
			return 0.5;
		if( iAction >= m_cMachines ){//after restart observation is always 1
			if( iObservation == 0 )
				return 0.0;
			return 1.0;
		}
		//ping
		if( iObservation == 1 )//up observation: pr(1|b,a) = pr(1|U)pr(U) + pr(1|D)pr(D)
			return m_adWorkingProbability[iAction] * 0.95 + ( 1 - m_adWorkingProbability[iAction] ) * 0.05; 
		else //down observation: pr(0|b,a) = pr(0|U)pr(U) + pr(0|D)pr(D)
			return m_adWorkingProbability[iAction] * 0.05 + ( 1 - m_adWorkingProbability[iAction] ) * 0.95; 
	}
	
	public int getNonZeroEntriesCount(){
		return m_cStates;
	}
	
	public Collection<Entry<Integer, Double>> getNonZeroEntries(){
		if( m_aStateProbabilities == null )
			initStateProbabilities();
		Map<Integer, Double> mNonZero = new TreeMap();
		for( int i = 0 ; i < m_aStateProbabilities.length ; i++ ){
			if( m_aStateProbabilities[i] > 0.0 )
				mNonZero.put( i, m_aStateProbabilities[i] );
		}
		return mNonZero.entrySet();	
	}
	
	private class ArrayIterator implements Iterator<Entry<Integer,Double>>{
		private double[] m_adArray;
		private int m_iCurrent;
		public ArrayIterator( double[] array ){
			m_adArray = array;
			m_iCurrent = 0;
			while( m_iCurrent < m_adArray.length && m_adArray[m_iCurrent] == 0.0 )
				m_iCurrent++;
		}
		public boolean hasNext() {
			return m_iCurrent < m_adArray.length;
		}
		public Entry<Integer,Double> next() {
			if( hasNext() ){
				Entry e = new ArrayEntry( m_iCurrent, m_adArray[m_iCurrent] );
				do{
					m_iCurrent++;
				}while( m_iCurrent < m_adArray.length && m_adArray[m_iCurrent] == 0.0 );
				return e;
			}
			return null;
		}
		public void remove(){
		}
		
		private class ArrayEntry implements Entry<Integer,Double>{

			Integer m_iKey;
			Double m_dValue;
			
			public ArrayEntry( Integer iKey, Double dValue ){
				m_iKey = iKey;
				m_dValue = dValue;
			}

			public Integer getKey() {
				return m_iKey;
			}

			public Double getValue() {
				return m_dValue;
			}

			public Double setValue( Double dValue ){
				m_dValue = dValue;
				return null;
			}
			
		}
	}
}
