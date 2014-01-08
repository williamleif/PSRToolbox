package pomdp.utilities.datastructures;

import pomdp.utilities.Logger;

public class DoubleVector extends VectorBase{

	//private double[][] m_aData;
	private float[][] m_aData;
	
	public DoubleVector( long iSize, VectorFactory<DoubleVector> vFactory, boolean bSmall ){
		super( iSize, vFactory, bSmall );
		setSize( iSize );
	}
	public DoubleVector( long iSize ){
		super( iSize, null, false );
		setSize( iSize );
	}
	/*
	public void finalize(){
		Logger.getInstance().logln( "* DoubleVector release " + getId() );
		System.exit( 0 );
	}
	*/
	
	private void set( int i, int j, double v ){
		m_aData[i][j] = (float)v;
	}
	
	public void add( double dElement ){
		if( m_cElements == m_iSize )
			expand();
		set( getFisrtIndex( m_cElements ), getSecondIndex( m_cElements ), dElement );
		m_cElements++;
	}
	public void setSize( long iSize ){
		//if( m_aData == null || iSize > m_iSize )
		int cRows = (int)( iSize/MAX_ARRAY_SIZE );
		int iLastRow = (int)( iSize%MAX_ARRAY_SIZE ), iRow = 0;
		m_aData = new float[cRows + 1][];
		for( iRow = 0 ; iRow < cRows ; iRow++ )
			m_aData[iRow] = new float[MAX_ARRAY_SIZE];
		m_aData[cRows] = new float[iLastRow];
		m_iSize = iSize;
		m_cElements = 0;
	}
	public void clear(){
		m_cElements = 0;
	}
	public void set( long iIndex, double dElement ){
		set( getFisrtIndex( iIndex ), getSecondIndex( iIndex ), dElement );
		if( iIndex >= m_cElements )
			m_cElements = iIndex + 1;
	}
	public void removeElement( long iElement ){
		long iIndex = indexOf( iElement );
		set( iIndex, elementAt( m_cElements - 1 ) );
		m_cElements--;
	}
	public double elementAt( long iIndex ){
		if( iIndex < 0 || iIndex >= m_cElements )
			return Double.NaN;
		return m_aData[getFisrtIndex( iIndex )][getSecondIndex( iIndex )];
	}
	private void expand(){
		if( m_iSize < MAX_ARRAY_SIZE ){
			int iNewSize = 0, i = 0;
			if( m_iSize * 2 > MAX_ARRAY_SIZE )
				iNewSize = MAX_ARRAY_SIZE;
			else
				iNewSize = (int)m_iSize * 2;
			float[] aData = new float[iNewSize];
			for( i = 0 ; i < m_cElements ; i++ ){
				aData[i] = m_aData[0][i];
			}
			m_iSize = iNewSize;
			m_aData[0] = aData;
		}
		else{
			int iOldSize = m_aData.length, iNewSize = iOldSize + 1, i = 0;
			float[][] aData = new float[iNewSize][];
			for( i = 0 ; i < iOldSize ; i++ ){
				aData[i] = m_aData[i];
			}
			for( i = iOldSize ; i < iNewSize ; i++ ){
				aData[i] = new float[MAX_ARRAY_SIZE];
			}
			m_iSize = iNewSize * MAX_ARRAY_SIZE;
			m_aData = aData;
		}
	}
	public void reduce(){
		/*
		long iNewSize = m_cElements, i = 0;
		long[] aData = new long[iNewSize];
		for( i = 0 ; i < m_cElements ; i++ ){
			aData[i] = m_aData[i];
		}
		m_aData = aData;
		*/
	}
	public long indexOf( long iElement ){
		int i = 0, j = 0;
		int cRows = (int)(m_cElements / MAX_ARRAY_SIZE);
		int cCols = (int)(m_cElements % MAX_ARRAY_SIZE);
		for( j = 0 ; j < cRows ; j++ ){
			for( i = 0 ; i < MAX_ARRAY_SIZE ; i++ ){
				if( m_aData[j][i] == iElement ){
					return (long)j * MAX_ARRAY_SIZE + i;
				}
			}
		}
		for( i = 0 ; i < cCols ; i++ ){
			if( m_aData[cRows][i] == iElement ){
				return (long)cRows * MAX_ARRAY_SIZE + i;
			}
		}
		return -1;
		
	}
	public boolean contains( long iElement ) {
		return indexOf( iElement ) != -1;
	}
	public void addAll( DoubleVector v ){
		long iIndex = 0;
		for( iIndex = 0 ; iIndex < v.m_cElements ; iIndex++ ){
			add( v.elementAt( iIndex ) );
		}
	}
	public void validateSize( long cVertexes ){
		if( m_iSize > cVertexes )
			Logger.getInstance().log( "DV", 0, "validateSize", "Expected " + cVertexes + " real size " + m_iSize + ", elements " + m_cElements );	
	}
}
