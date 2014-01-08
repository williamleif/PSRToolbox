package pomdp.utilities;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import pomdp.utilities.datastructures.StaticMap;

public class TabularBeliefState extends BeliefState {
	protected double[] m_aStateProbabilities;
	protected StaticMap m_mNonZeroEntries;
	private Map m_mDominatingNonZeroEntries;
	protected boolean m_bSparse;
	
	public TabularBeliefState( int cStates, int cActions, int cObservations, int id, boolean bSparse, boolean bCacheBeliefStates, BeliefStateFactory bsFactory ){
		super( cStates, cActions, cObservations, id, bCacheBeliefStates, bsFactory );
		m_bSparse = bSparse;		
		m_aStateProbabilities = new double[m_cStates];
	}

	public double valueAt( int iState ){
		if( m_aStateProbabilities != null )
			return m_aStateProbabilities[iState];
		else{
			double dValue = m_mNonZeroEntries.get( iState );
			return dValue;
		}		
	}
	
	public synchronized void setValueAt( int iState, double dValue ){
		if( m_aStateProbabilities != null )
			m_aStateProbabilities[iState] = dValue;
		if( m_mNonZeroEntries != null )
			m_mNonZeroEntries.set( iState, dValue );
		if( dValue != 0.0 ){
			if( dValue == 1.0 ){
				m_bDeterministic = true;
				m_iDeterministicIndex = iState;
			}
			if( dValue > m_dMaxBelief ){
				m_dMaxBelief = dValue;
				m_iMaxBeliefState = iState;
			}
		}
	}
	
	/**
	 * Returns an Iterator over the non-zero entries of the belief state
	 * @return
	 */
	public Collection<Entry<Integer,Double>> getNonZeroEntries(){
		if( ( m_mNonZeroEntries == null ) && ( m_aStateProbabilities != null ) ){
			m_mNonZeroEntries = new StaticMap( m_aStateProbabilities, 0.0 );
			if( m_bSparse )
				m_aStateProbabilities = null;
		}
		return m_mNonZeroEntries;
	}
	
	public Iterator getDominatingNonZeroEntries(){
		if( m_mDominatingNonZeroEntries == null ){
			Iterator itNonZero = getNonZeroEntries().iterator();
			double dMaxProb = 0.0;
			Entry e = null;
			int iState = 0;
			double dProb = 0.0;
			
			m_mDominatingNonZeroEntries = new TreeMap();
			
			while( itNonZero.hasNext() ){
				e = (Entry) itNonZero.next();
				iState = ((Integer) e.getKey()).intValue();
				dProb = ((Double) e.getValue()).doubleValue();
				if( dProb > 0.01 )
					m_mDominatingNonZeroEntries.put( new Integer( iState ), new Double( dProb ) );
				if( dProb > dMaxProb )
					dMaxProb = dProb;
			}
			
			if( m_mDominatingNonZeroEntries.size() == 0 ){
				itNonZero = getNonZeroEntries().iterator();
				while( itNonZero.hasNext() ){
					e = (Entry) itNonZero.next();
					iState = ((Integer) e.getKey()).intValue();
					dProb = ((Double) e.getValue()).doubleValue();
					if( dProb > dMaxProb / 2 )
						m_mDominatingNonZeroEntries.put( new Integer( iState ), new Double( dProb ) );
				}		
			}			
		}
		
		return m_mDominatingNonZeroEntries.entrySet().iterator();
	}
	
	public int getNonZeroEntriesCount(){
		getNonZeroEntries();
		return m_mNonZeroEntries.size();
	}
	
	public double[] toArray(){
		return (double[]) m_aStateProbabilities.clone();
	}
	
	public int countEntries() {
		return m_mNonZeroEntries.size();
	}

	public long size() {
		if( m_bSparse )
			return m_mNonZeroEntries.size();
		else
			return m_cStates;
	}

	public Map<Integer, Double> getNonZeroEntriesMap() {
		TreeMap<Integer, Double> mNonZero = new TreeMap<Integer, Double>();
		Iterator<Entry<Integer,Double>> itNonZero = getNonZeroEntries().iterator();
		Entry<Integer,Double> e = null;
		while( itNonZero.hasNext() ){
			e = itNonZero.next();
			mNonZero.put( e.getKey(), e.getValue() );
		}
		return mNonZero;
	}

	public void clearZeroEntries() {
		if( m_mNonZeroEntries != null ){
			m_mNonZeroEntries.clearZeroEntries();
		}
		
	}
}
