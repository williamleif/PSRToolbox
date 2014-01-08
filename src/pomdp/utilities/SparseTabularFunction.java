package pomdp.utilities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import pomdp.utilities.datastructures.Function;

public class SparseTabularFunction extends Function {

	protected HashMap m_mSingleParameterValues;
	protected HashMap[] m_mDualParameterValues;
	protected HashMap<Integer,Double>[][] m_mTripleParametermValues;
	
	public SparseTabularFunction( int[] aiDims ){
		super( aiDims );
		int i = 0, j = 0;
		if( aiDims.length >= 1 ){
			m_mSingleParameterValues = new HashMap();
			if( aiDims.length >= 2 ){
				m_mDualParameterValues = new HashMap[aiDims[0]];
				for( i = 0 ; i < aiDims[0] ; i++ )
					m_mDualParameterValues[i] = new HashMap();
				if( aiDims.length >= 3 ){
					m_mTripleParametermValues = new HashMap[aiDims[0]][aiDims[1]];
					for( i = 0 ; i < aiDims[0] ; i++ )
						for( j = 0 ; j < aiDims[1] ; j++ )
							m_mTripleParametermValues[i][j] = new HashMap<Integer,Double>();
				}
			}
		}
	}

	public double valueAt( int arg1 ){
		Double dValue = (Double) m_mSingleParameterValues.get( new Integer( arg1 ) );
		if( dValue == null )
			return 0.0;
		return dValue.doubleValue();
	}

	public double valueAt( int arg1, int arg2 ){
		Double dValue = (Double) m_mDualParameterValues[arg1].get( new Integer( arg2 ) );
		if( dValue == null )
			return 0.0;
		return dValue.doubleValue();
	}

	public double valueAt( int arg1, int arg2, int arg3 ){
		Double dValue = m_mTripleParametermValues[arg1][arg2].get( arg3 );
		if( dValue == null )
			return 0.0;
		return dValue.doubleValue();
	}

	public void setValue( int arg1, double dValue ){
		if( dValue > m_dMaxValue )
			m_dMaxValue = dValue;
		if( dValue < m_dMinValue )
			m_dMinValue = dValue;
		if( dValue != 0.0 ){
			m_mSingleParameterValues.put( new Integer( arg1 ), new Double( dValue ) );
		}
		else{
			m_mSingleParameterValues.remove( new Integer( arg1 ) );
		}
	}

	public void setValue( int arg1, int arg2, double dValue ){
		if( dValue > m_dMaxValue )
			m_dMaxValue = dValue;
		if( dValue < m_dMinValue )
			m_dMinValue = dValue;
		if( dValue != 0.0 ){
			m_mDualParameterValues[arg1].put( new Integer( arg2 ), new Double( dValue ) );
		}
		else{
			m_mDualParameterValues[arg1].remove( new Integer( arg2 ) );
		}
	}

	public void setValue( int arg1, int arg2, int arg3, double dValue ){
		
		
		if( dValue > m_dMaxValue )
			m_dMaxValue = dValue;
		if( dValue < m_dMinValue )
			m_dMinValue = dValue;
		if( dValue != 0.0 ){
			m_mTripleParametermValues[arg1][arg2].put( arg3, dValue );
		}
		else{
			m_mTripleParametermValues[arg1][arg2].remove( arg3 );
		}
		
	}

	public Iterator<Entry<Integer,Double>> getNonZeroEntries( int arg1, int arg2 ){
		return m_mTripleParametermValues[arg1][arg2].entrySet().iterator();
	}
	
	public Iterator getNonZeroEntries() {
		return m_mSingleParameterValues.entrySet().iterator();
	}

	public int countNonZeroEntries( int arg1, int arg2 ){
		return m_mTripleParametermValues[arg1][arg2].size();
	}

	public int countEntries() {
		int cEntries = 0;
		int i = 0, j = 0;
		for( i = 0 ; i < m_mTripleParametermValues.length ; i++ )
			for( j = 0 ; j < m_mTripleParametermValues[0].length ; j++ )
				cEntries += m_mTripleParametermValues[i][j].size();
		return cEntries;
	}

	public int countNonZeroEntries() {
		return m_mSingleParameterValues.size();
	}
}
