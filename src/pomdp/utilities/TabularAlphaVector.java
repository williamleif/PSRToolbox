package pomdp.utilities;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import pomdp.environments.POMDP;
import pomdp.utilities.datastructures.StaticMap;

public class TabularAlphaVector extends AlphaVector{
	private double[] m_aValues;
	private StaticMap m_mValues;

	public TabularAlphaVector( BeliefState bsWitness, double dDefaultValue, POMDP pomdp ){
		this( bsWitness, 0, pomdp );
		int iState = 0;
		for( iState = 0 ; iState < m_cStates ; iState++ ){
			m_aValues[iState] = dDefaultValue;
		}
		m_dOffset = 0.0;
		m_mValues = null;

	}

	public TabularAlphaVector( BeliefState bsWitness, int iAction, POMDP pomdp ){
		super( bsWitness, iAction, pomdp );
		m_aValues = new double[m_cStates];
		m_mValues = null;
	}
	
	public double valueAt( int iState ){
		if( m_aValues != null )
			return m_aValues[iState] - m_dOffset;
		else			
			return m_mValues.get( iState ) - m_dOffset;
	}
	
	public void setValue( int iState, double dValue ){
		//if( dValue > 1000.0 )
		//	Logger.getInstance().logln( "setValue: BUGBUG" );
		if( dValue > m_dMaxValue )
			m_dMaxValue = dValue;
		m_dAvgValue += dValue / m_cStates;
		m_aValues[iState] = dValue;
	}
	
	public Iterator getNonZeroEntries() {
		if( m_mValues == null )
			finalizeValues();
		return m_mValues.iterator();
	}
	
	private static int g_cGain = 0;
	private static int g_cFinalized = 0;
	
	public void finalizeValues() {
		m_mValues = new StaticMap( m_aValues, 0.001 );	
		g_cGain += ( m_aValues.length - m_mValues.countEntries() * 2 );
		g_cFinalized++;
		if( false && g_cFinalized % 100 == 0 ){
			Logger.getInstance().logln( "After " + g_cFinalized + " finalized, avg gain is " + ( g_cGain / g_cFinalized ) + "/" + m_aValues.length );
		}
		m_aValues = null;
	}
	
	public AlphaVector newAlphaVector() {
		AlphaVector avResult = new TabularAlphaVector( null, 0, m_pPOMDP );
		return avResult;
	}

	public void accumulate( AlphaVector av ){
		int iState = 0;
		double dValue = 0.0, dLocalValue = 0.0;
		Iterator itNonZero = av.getNonZeroEntries();
		
		if( itNonZero != null ){
			Pair p = null;
			while( itNonZero.hasNext() ){
				p = (Pair) itNonZero.next();
				iState = ((Number)p.m_first).intValue();
				dValue = ((Number)p.m_second).doubleValue();
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
		if( m_mValues == null )
			return m_cStates;
		return m_mValues.size();
	}

	public long countLocalEntries() {
		return m_mValues.size();
	}
	
	public long size() {
		return m_mValues.size();
	}

	
	public void setSize( int cStates ){
		m_cStates = cStates;
		m_aValues = new double[m_cStates];
		m_mValues = null;
	}
}
