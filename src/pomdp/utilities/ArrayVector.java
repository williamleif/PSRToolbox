package pomdp.utilities;

import pomdp.utilities.datastructures.LongVector;
import pomdp.utilities.datastructures.VectorBase;
import pomdp.utilities.datastructures.VectorFactory;


public class ArrayVector<VType> extends VectorBase{
	private VType[][] m_aData;

	
	public ArrayVector( long iSize, VectorFactory<ArrayVector<VType>> vFactory, boolean bSmall ){
		super( iSize, vFactory, bSmall );
		setSize( iSize );
		//if( m_iID == 320 )
		//	Logger.getInstance().logln( "* ArrayVector" );
	}
	/*
	public void finalize(){
		Logger.getInstance().logln( "* ArrayVector" );
	}
	*/
	public void add( VType aElement ){
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
		m_aData = (VType[][])new Object[cRows + 1][];
		for( iRow = 0 ; iRow < cRows ; iRow++ )
			m_aData[iRow] = (VType[])new Object[MAX_ARRAY_SIZE];
		m_aData[cRows] = (VType[])new Object[iLastRow];
		m_iSize = iSize;
		m_cElements = 0;
	}
	public void clear(){
		int iElement = 0;
		for( iElement = 0 ; iElement < m_cElements ; iElement++ ){
			if( elementAt( iElement ) instanceof LongVector ){
				LongVector lv = (LongVector)elementAt( iElement );
				lv.release();
				set( iElement, null );
			}
		}
		m_cElements = 0;
	}
	public void set( long iIndex, VType vElement ){
		m_aData[getFisrtIndex( iIndex )][getSecondIndex( iIndex )] = vElement;
	}
	public void removeElement( VType vElement ){
		long iIndex = indexOf( vElement );
		set( iIndex, elementAt( m_cElements - 1 ) );
		m_cElements--;
	}
	public VType elementAt( long iIndex ){
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
			VType[] aData = (VType[])new Object[iNewSize];
			for( i = 0 ; i < m_cElements ; i++ ){
				aData[i] = m_aData[0][i];
			}
			m_iSize = iNewSize;
			m_aData[0] = aData;
		}
		else{
			int iOldSize = m_aData.length, iNewSize = iOldSize + 1, i = 0;
			VType[][] aData = (VType[][])new Object[iNewSize][];
			for( i = 0 ; i < iOldSize ; i++ ){
				aData[i] = m_aData[i];
			}
			for( i = iOldSize ; i < iNewSize ; i++ ){
				aData[i] = (VType[])new Object[MAX_ARRAY_SIZE];
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
	
	public long indexOf( VType vElement ){
		int i = 0, j = 0;
		int cRows = (int)(m_cElements / MAX_ARRAY_SIZE);
		int cCols = (int)(m_cElements % MAX_ARRAY_SIZE);
		for( j = 0 ; j < cRows ; j++ ){
			for( i = 0 ; i < MAX_ARRAY_SIZE ; i++ ){
				if( vElement.equals( m_aData[j][i] ) ){
					return (long)j * MAX_ARRAY_SIZE + i;
				}
			}
		}
		for( i = 0 ; i < cCols ; i++ ){
			if( vElement.equals( m_aData[cRows][i] ) ){
				return (long)cRows * MAX_ARRAY_SIZE + i;
			}
		}
		return -1;
		
	}
	public boolean contains( VType vElement ) {
		return indexOf( vElement ) != -1;
	}
	public void addAll( ArrayVector<VType> v ){
		long iIndex = 0;
		for( iIndex = 0 ; iIndex < v.m_cElements ; iIndex++ ){
			add( v.elementAt( iIndex ) );
		}
	}
	public void validateSize( long cVertexes ){
		if( m_iSize > cVertexes )
			Logger.getInstance().log( "AV", 0, "validateSize", "Expected " + cVertexes + " real size " + m_iSize + ", elements " + m_cElements );	
	}
}