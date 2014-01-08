package pomdp.utilities.datastructures;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;


/*
 * Created on May 3, 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author shanigu
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TabularFunction extends Function{

	private double[] m_tbl1DValues;
	private double[][] m_tbl2DValues;
	private double[][][] m_tbl3DValues;
	
	public TabularFunction( int[] aiDimensions ){
		super( aiDimensions );
		m_tbl1DValues = null;
		m_tbl2DValues = null;
		m_tbl3DValues = null;
		if( aiDimensions.length >= 1 ){
			m_tbl1DValues = new double[aiDimensions[0]];
		}
		if( aiDimensions.length >= 2 ){
			m_tbl2DValues = new double[aiDimensions[0]][aiDimensions[1]];
		}
		if( aiDimensions.length == 3 ){
			m_tbl3DValues = new double[aiDimensions[0]][aiDimensions[1]][aiDimensions[2]];
		}
	}
	
	/* (non-Javadoc)
	 * @see Function#valueAt(int)
	 */
	
	public double valueAt( int arg1 ) {
		if( m_tbl1DValues == null )
			return 0.0;		
		return m_tbl1DValues[arg1];
	}

	/* (non-Javadoc)
	 * @see Function#valueAt(int, int)
	 */
	public double valueAt( int arg1, int arg2 ) {
		
		if( m_tbl2DValues == null )
			return 0.0;		
		return m_tbl2DValues[arg1][arg2];
	}

	/* (non-Javadoc)
	 * @see Function#valueAt(int, int, int)
	 */
	public double valueAt( int arg1, int arg2, int arg3 ) {
		return m_tbl3DValues[arg1][arg2][arg3];
	}
	
	public void setValue( int arg1, double dValue ){
		if( dValue > m_dMaxValue )
			m_dMaxValue = dValue;
		if( dValue < m_dMinValue )
			m_dMinValue = dValue;
		m_tbl1DValues[arg1] = dValue;
	}
	
	public void setValue( int arg1, int arg2, double dValue ){
		if( dValue > m_dMaxValue )
			m_dMaxValue = dValue;
		if( dValue < m_dMinValue )
			m_dMinValue = dValue;
		m_tbl2DValues[arg1][arg2] = dValue;
	}
	
	public void setValue( int arg1, int arg2, int arg3, double dValue ){
		if( dValue > m_dMaxValue )
			m_dMaxValue = dValue;
		if( dValue < m_dMinValue )
			m_dMinValue = dValue;
		m_tbl3DValues[arg1][arg2][arg3] = dValue;
	}

	
	public Iterator<Entry<Integer,Double>> getNonZeroEntries( int arg1, int arg2 ){
		return new ArrayIterator( m_tbl3DValues[arg1][arg2] );
	}


	public Iterator<Entry<Integer,Double>> getNonZeroEntries() {
		return new ArrayIterator( m_tbl1DValues );
	}

	public int countNonZeroEntries( int arg1, int arg2 ){
		return countNonZeroEntries( m_tbl3DValues[arg1][arg2] );
	}

	private int countNonZeroEntries( double[] array ){
		int cEntries = 0, iEntry = 0;
		for( iEntry = 0 ; iEntry < array.length ; iEntry++ ){
			if( array[iEntry] != 0.0 ){
				cEntries++;
			}
		}
		return cEntries;
	}
	
	public int countNonZeroEntries(){
		return countNonZeroEntries( m_tbl1DValues );
	}

	public int countEntries() {
		int cEntries = 0;
		if( m_tbl1DValues != null )
			cEntries += m_tbl1DValues.length;
		if( m_tbl2DValues != null )
			cEntries += m_tbl2DValues.length * m_tbl2DValues[0].length;
		if( m_tbl3DValues != null )
			cEntries += m_tbl3DValues.length * m_tbl3DValues[0].length * m_tbl3DValues[0][0].length;
		return cEntries;
	}


	private class ArrayIterator implements Iterator<Entry<Integer,Double>>{
		private double[] m_adArray;
		private int m_iCurrent;

		public ArrayIterator( double[] array ){
			m_adArray = array;
			m_iCurrent = 0;
		}

		public boolean hasNext() {
			while( ( m_iCurrent < m_adArray.length ) && ( m_adArray[m_iCurrent] == 0.0 ) ){
				m_iCurrent++;
			}
			return m_iCurrent < m_adArray.length;
		}

		public Entry<Integer,Double> next() {
			while( ( m_iCurrent < m_adArray.length ) && ( m_adArray[m_iCurrent] == 0.0 ) ){
				m_iCurrent++;
			}
			if( m_iCurrent < m_adArray.length ){
				m_iCurrent++;
				return new ArrayEntry<Integer,Double>( m_iCurrent - 1, m_adArray[m_iCurrent - 1] );	
			}
			return null;
		}

		public void remove() {
		}

		private class ArrayEntry<K,V> implements Entry<K,V>{
			private K m_kKey;
			private V m_vValue;

			public ArrayEntry( K kKey, V vValue ){
				m_kKey = kKey;
				m_vValue = vValue;
			}

			public K getKey() {
				return m_kKey;
			}

			public V getValue() {
				return m_vValue;
			}

			public V setValue( V arg0 ){
				return null;
			}

		}
	}
}
