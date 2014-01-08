package pomdp.utilities.datastructures;


public class LongVector extends VectorBase{

	private long[][] m_aData;
	
	public LongVector( long iSize, VectorFactory<LongVector> vFactory, boolean bSmall ){
		super( iSize, vFactory, bSmall );
		setSize( iSize );
	}

	public LongVector( long iSize ){
		super( iSize, null, true );
		setSize( iSize );
	}

	public void add( long iElement ){
		if( m_cElements == m_iSize )
			expand();
		int iFirstIndex = getFisrtIndex( m_cElements );
		int iSecondIndex = getSecondIndex( m_cElements );
		m_aData[iFirstIndex][iSecondIndex] = iElement;
		m_cElements++;
	}
	public void setSize( long iSize ){
		//if( m_aData == null || iSize > m_iSize )
		int cRows = (int)( iSize/MAX_ARRAY_SIZE );
		int iLastRow = (int)( iSize%MAX_ARRAY_SIZE ), iRow = 0;
		m_aData = new long[cRows + 1][];
		for( iRow = 0 ; iRow < cRows ; iRow++ )
			m_aData[iRow] = new long[MAX_ARRAY_SIZE];
		m_aData[cRows] = new long[iLastRow];
		m_iSize = iSize;
		m_cElements = 0;
	}
	public void clear(){
		m_cElements = 0;
	}
	public void set( long iIndex, long iElement ){
		m_aData[getFisrtIndex( iIndex )][getSecondIndex( iIndex )] = iElement;
	}
	public void removeElement( long iElement ){
		long iIndex = indexOf( iElement );
		set( iIndex, elementAt( m_cElements - 1 ) );
		m_cElements--;
	}
	public long elementAt( long iIndex ){
		if( iIndex < 0 || iIndex >= m_cElements )
			return -1;
		return m_aData[getFisrtIndex( iIndex )][getSecondIndex( iIndex )];
	}
	private void expand(){
		if( m_iSize < MAX_ARRAY_SIZE ){
			int iNewSize = 0, i = 0;
			if( m_iSize * 2 > MAX_ARRAY_SIZE )
				iNewSize = MAX_ARRAY_SIZE;
			else
				iNewSize = (int)m_iSize * 2;
			long[] aData = new long[iNewSize];
			for( i = 0 ; i < m_cElements ; i++ ){
				aData[i] = m_aData[0][i];
			}
			m_iSize = iNewSize;
			m_aData[0] = aData;
		}
		else{
			int iOldSize = m_aData.length, iNewSize = iOldSize + 1, i = 0;
			long[][] aData = new long[iNewSize][];
			for( i = 0 ; i < iOldSize ; i++ ){
				aData[i] = m_aData[i];
			}
			for( i = iOldSize ; i < iNewSize ; i++ ){
				aData[i] = new long[MAX_ARRAY_SIZE];
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
	public void addAll( LongVector v ){
		long iIndex = 0;
		for( iIndex = 0 ; iIndex < v.m_cElements ; iIndex++ ){
			add( v.elementAt( iIndex ) );
		}
	}
	public static VectorBase newInstance( long iSize, VectorFactory vFactory, boolean bSmall ){
		return new LongVector( iSize, vFactory, bSmall );
	}

}
