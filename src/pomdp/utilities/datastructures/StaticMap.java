package pomdp.utilities.datastructures;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import pomdp.utilities.Pair;

public class StaticMap implements Collection<Map.Entry<Integer,Double>>, Serializable{
	private int[] m_aiIndexes;
	private double[] m_adValues;
	private int m_cNonZeroValues;
	
	public StaticMap( double[] adAllValues, double dEpsilon ){
		countNonZeroEntries( adAllValues, dEpsilon );
		initArrays( adAllValues, dEpsilon );
	}
	
	private void initArrays( double[] adAllValues, double dEpsilon ){
		int idx = 0, realIdx = 0;
		m_aiIndexes = new int[m_cNonZeroValues];
		m_adValues = new double[m_cNonZeroValues];
		for( idx = 0 ; idx < adAllValues.length && realIdx < m_cNonZeroValues ; idx++ ){
			if( Math.abs( adAllValues[idx] ) > dEpsilon ){
				m_aiIndexes[realIdx] = idx;
				m_adValues[realIdx] = adAllValues[idx];
				realIdx++;
			}
		}
	}

	private void countNonZeroEntries( double[] adAllValues, double dEpsilon ){
		int idx = 0;
		m_cNonZeroValues = 0;
		for( idx = 0 ; idx < adAllValues.length ; idx++ ){
			if( Math.abs( adAllValues[idx] ) > dEpsilon ){
				m_cNonZeroValues++;
			}
		}
	}
	
	private int find( int iRealIdx ){
		int iStart = 0, iEnd = m_cNonZeroValues;
		int iMedian = 0;
		
		while( ( iEnd - iStart ) > 1 ){
			iMedian = ( iStart + iEnd ) / 2;
			if( m_aiIndexes[iMedian] == iRealIdx ){
				return iMedian;
			}
			else if( m_aiIndexes[iMedian] > iRealIdx ){
				iEnd = iMedian;
			}
			else{
				iStart = iMedian + 1;
			}
		}
		if( iEnd == iStart + 1 ){
			if( m_aiIndexes[iStart] == iRealIdx )
				return iStart;
		}
		return -1;
	}
	/*
	private int find( int iRealIdx, int iStart, int iEnd ){
		if( iEnd == iStart + 1 ){
			if( m_aiIndexes[iStart] == iRealIdx )
				return iStart;
			else
				return -1;
		}
		if( iEnd <= iStart ){
			return -1;
		}
		int iMedian = ( iStart + iEnd ) / 2;
		if( m_aiIndexes[iMedian] == iRealIdx ){
			return iMedian;
		}
		if( m_aiIndexes[iMedian] > iRealIdx ){
			return find( iRealIdx, iStart, iMedian );
		}
		else{
			return find( iRealIdx, iMedian + 1, iEnd );
		}
	}
	*/
	public double get( int idx ){
		int iMapIdx = find( idx );
		if( iMapIdx == -1 ){
			return 0.0;
		}
		else{
			return m_adValues[iMapIdx];
		}
	}
	
	public int countEntries(){
		return m_cNonZeroValues;	
	}

	public Iterator<Map.Entry<Integer,Double>> iterator() {
		return new StaticMapIterator( m_aiIndexes, m_adValues, m_cNonZeroValues );
	}
	
	private class StaticMapIterator implements Iterator<Map.Entry<Integer,Double>>{
		private int[] m_aiIndexes;
		private double[] m_adValues;
		private int m_cNonZeroValues;
		private int m_iCurrent;
		
		public StaticMapIterator( int[] aiIndexes, double[] adValues, int cNonZeroValues ){
			m_aiIndexes = aiIndexes;
			m_adValues = adValues;
			m_cNonZeroValues = cNonZeroValues;	
			m_iCurrent = 0;
		}

		public boolean hasNext() {
			return m_iCurrent < m_cNonZeroValues;
		}

		public Map.Entry<Integer,Double> next() {
			if( hasNext() ){
				m_iCurrent++;
				return new Pair<Integer,Double>( new Integer( m_aiIndexes[m_iCurrent - 1] ),
										new Double( m_adValues[m_iCurrent - 1] ) );
			}
			return null;
		}

		public void remove() {
			//not implemented
		}	
	}

	public int size() {
		return m_cNonZeroValues;
	}

	public void set( int idx, double dValue ) {
		int iMapIdx = find( idx );
		if( iMapIdx != -1 ){
			m_adValues[iMapIdx] = dValue;
		}				
	}

	@Override
	public boolean add(Entry<Integer, Double> arg0) {
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends Entry<Integer, Double>> arg0) {
		return false;
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean contains(Object oState) {
		int iState = (Integer)oState;
		if( find( iState ) != -1 )
			return true;
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> col) {
		for( Object o : col )
			if( !contains( o ) )
				return false;
		return true;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean remove(Object arg0) {
		return false;
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		return false;
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		return false;
	}

	@Override
	public Object[] toArray() {
		return null;
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		return null;
	}

	public void clearZeroEntries() {
		int iCurrentNonZeros = m_cNonZeroValues;
		countNonZeroEntries( m_adValues, 0.0 );
		if( m_cNonZeroValues != iCurrentNonZeros )
		{
			int idx = 0, realIdx = 0;
			int[] aiIndexes = new int[m_cNonZeroValues];
			double[] adValues = new double[m_cNonZeroValues];
			for( idx = 0 ; idx < m_adValues.length ; idx++ ){
				if( m_adValues[idx] > 0.0 ){
					adValues[realIdx] = m_adValues[idx];
					aiIndexes[realIdx] = m_aiIndexes[idx];
					realIdx++;
				}
			}
			m_adValues = adValues;
			m_aiIndexes = aiIndexes;
		}
	}
}
