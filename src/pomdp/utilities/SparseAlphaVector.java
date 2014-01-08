package pomdp.utilities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import pomdp.environments.POMDP;

public class SparseAlphaVector extends AlphaVector {

	protected Map m_mValues;
	
	public SparseAlphaVector( BeliefState bsWitness, int iAction, POMDP pomdp ){
		super( bsWitness, iAction, pomdp );
		m_mValues = new HashMap();
	}

	public double valueAt( int iState ){
		Integer iKey = new Integer( iState );
		Double dValue = (Double) m_mValues.get( iKey );
		if( dValue == null )
			return 0.0;
		return dValue.doubleValue() - m_dOffset;
	}

	public void setValue( int iState, double dValue ){
		Integer iKey = new Integer( iState );
		Double dDoubleValue = (Double)m_mValues.remove( iKey );
		if( dValue != 0.0 ){
			dDoubleValue = new Double( dValue );
			m_mValues.put( iKey, dDoubleValue );
			if( dValue > m_dMaxValue )
				m_dMaxValue = dValue;
			m_dAvgValue += dValue / m_cStates;
		}
	}

	public Iterator getNonZeroEntries() {
		return m_mValues.entrySet().iterator();
	}

	public void finalizeValues() {
		// TODO Auto-generated method stub
		
	}

	public AlphaVector newAlphaVector() {
		AlphaVector avResult = new SparseAlphaVector( null, 0, m_pPOMDP );
		return avResult;
	}


	public void accumulate( AlphaVector av ){
		int iState = 0;
		double dValue = 0.0, dLocalValue = 0.0;
		Iterator itNonZero = av.getNonZeroEntries();
		
		if( itNonZero != null ){
			Map.Entry e = null;
			while( itNonZero.hasNext() ){
				e = (Entry) itNonZero.next();
				iState = ((Number) e.getKey()).intValue();
				dValue = ((Number) e.getValue()).doubleValue();
				dLocalValue = valueAt( iState );
				setValue( iState, dValue + dLocalValue );
			}
		}
		else{
			for( iState = 0 ; iState < m_cStates ; iState++ ){
				setValue( iState, valueAt( iState ) + av.valueAt( iState ) );
			}
		}
	}

	public int getNonZeroEntriesCount() {
		return m_mValues.size();
	}

	public long countLocalEntries() {
		return m_mValues.size();
	}

	
	public long size() {
		return m_mValues.size();
	}

	
	public void setSize(int cStates) {
		//no need to do anything - we use a map here		
	}
}
