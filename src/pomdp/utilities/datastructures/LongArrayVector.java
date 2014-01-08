package pomdp.utilities.datastructures;

import pomdp.utilities.Logger;

public class LongArrayVector extends VectorBase{

	private long[][][] m_aData;
	
	private static long m_cIDs = 0;
	
	public LongArrayVector( long iSize, VectorFactory<LongArrayVector> vFactory, boolean bSmall ){
		super( iSize, vFactory, bSmall );
		setSize( iSize );
	}
	/*
	public void finalize(){
		Logger.getInstance().logln( "* LongArrayVector" );
	}
	*/
	public void add( long[] aElement ){
		if( m_cElements == m_iSize )
			expand();
		int iFirstIndex = getFisrtIndex( m_cElements );
		int iSecondIndex = getSecondIndex( m_cElements );
		m_aData[iFirstIndex][iSecondIndex] = aElement;
		m_cElements++;
	}
	public void setSize( long iSize ){
		//if( m_aData == null || iSize > m_iSize )
		int cRows = (int)( iSize/MAX_ARRAY_SIZE );
		int iLastRow = (int)( iSize%MAX_ARRAY_SIZE ), iRow = 0;
		m_aData = new long[cRows + 1][][];
		for( iRow = 0 ; iRow < cRows ; iRow++ )
			m_aData[iRow] = new long[MAX_ARRAY_SIZE][];
		m_aData[cRows] = new long[iLastRow][];
		m_iSize = iSize;
		m_cElements = 0;
	}
	public void clear(){
		m_cElements = 0;
	}
	public void set( long iIndex, long[] aElement ){
		m_aData[getFisrtIndex( iIndex )][getSecondIndex( iIndex )] = aElement;
		if( m_cElements <= iIndex )
			m_cElements = iIndex + 1;
	}
	public void removeElement( long[] aElement ){
		long iIndex = indexOf( aElement );
		set( iIndex, elementAt( m_cElements - 1 ) );
		m_cElements--;
	}
	public long[] elementAt( long iIndex ){
		if( iIndex < 0 || iIndex >= m_cElements )
			return null;
		return m_aData[getFisrtIndex( iIndex )][getSecondIndex( iIndex )];
	}
	private void expand(){
		if( m_iSize < MAX_ARRAY_SIZE ){
			int iNewSize = 0, i = 0;
			if( m_iSize * 2 > MAX_ARRAY_SIZE )
				iNewSize = MAX_ARRAY_SIZE;
			else
				iNewSize = (int)m_iSize * 2;
			long[][] aData = new long[iNewSize][];
			for( i = 0 ; i < m_cElements ; i++ ){
				aData[i] = m_aData[0][i];
			}
			m_iSize = iNewSize;
			m_aData[0] = aData;
		}
		else{
			int iOldSize = m_aData.length, iNewSize = iOldSize + 1, i = 0;
			long[][][] aData = new long[iNewSize][][];
			for( i = 0 ; i < iOldSize ; i++ ){
				aData[i] = m_aData[i];
			}
			for( i = iOldSize ; i < iNewSize ; i++ ){
				aData[i] = new long[MAX_ARRAY_SIZE][];
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
	
	private boolean equals( long[] a1, long[] a2 ){
		if( a1.length != a2.length )
			return false;
		if( a1 == a2 )
			return true;
		int i = 0;
		for( i = 0 ; i < a1.length ; i++ )
			if( a1[i] != a2[i] )
				return false;
		return true;
	}
	public long indexOf( long[] aElement ){
		int i = 0, j = 0;
		int cRows = (int)(m_cElements / MAX_ARRAY_SIZE);
		int cCols = (int)(m_cElements % MAX_ARRAY_SIZE);
		for( j = 0 ; j < cRows ; j++ ){
			for( i = 0 ; i < MAX_ARRAY_SIZE ; i++ ){
				if( equals( m_aData[j][i], aElement ) ){
					return (long)j * MAX_ARRAY_SIZE + i;
				}
			}
		}
		for( i = 0 ; i < cCols ; i++ ){
			if( equals( m_aData[cRows][i], aElement ) ){
				return (long)cRows * MAX_ARRAY_SIZE + i;
			}
		}
		return -1;
		
	}
	public boolean contains( long[] aElement ) {
		return indexOf( aElement ) != -1;
	}
	public void addAll( LongArrayVector v ){
		long iIndex = 0;
		for( iIndex = 0 ; iIndex < v.m_cElements ; iIndex++ ){
			add( v.elementAt( iIndex ) );
		}
	}
	public void validateSize( long cVertexes ){
		if( m_iSize > cVertexes )
			Logger.getInstance().log( "LAV", 0, "validateSize", "Expected " + cVertexes + " real size " + m_iSize + ", elements " + m_cElements );	
	}
}
