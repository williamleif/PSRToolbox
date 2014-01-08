package pomdp.utilities.datastructures;

import java.io.Serializable;


public class IntVector implements Serializable{

	private int[] m_aData;
	private int m_cElements;
	private IntVectorFactory m_ivFactory;
	private boolean m_bSmall;
	
	public IntVector( int iSize, IntVectorFactory ivFactory, boolean bSmall ){
		m_aData = new int[iSize];
		m_cElements = 0;
		m_ivFactory = ivFactory;
		m_bSmall = bSmall;
	}
	public IntVector( IntVectorFactory ivFactory, boolean bSmall ){
		this( 256, ivFactory, bSmall );
	}
	public IntVector( int iSize ){
		this( iSize, null, false );
	}
	public void add( int iElement ){
		if( m_cElements == m_aData.length )
			expand();
		m_aData[m_cElements] = iElement;
		m_cElements++;
	}
	public void setSize( int iSize ){
		m_aData = new int[iSize];
	}
	public void clear(){
		m_cElements = 0;
	}
	public void set( int iIndex, int iElement ){
		if( m_cElements <= iIndex )
			m_cElements = iIndex + 1;
		m_aData[iIndex] = iElement;
	}
	public void removeElement( int iElement ){
		int iIndex = indexOf( iElement );
		m_aData[iIndex] = m_aData[m_cElements - 1];
		m_cElements--;
	}
	public int elementAt( int iIndex ){
		if( iIndex < 0 || iIndex >= m_cElements )
			return -1;
		return m_aData[iIndex];
	}
	private void expand(){
		int iOldSize = m_aData.length, iNewSize = iOldSize * 2, i = 0;
		int[] aData = new int[iNewSize];
		for( i = 0 ; i < m_cElements ; i++ ){
			aData[i] = m_aData[i];
		}
		m_aData = aData;
	}
	public void reduce(){
		/*
		int iNewSize = m_cElements, i = 0;
		int[] aData = new int[iNewSize];
		for( i = 0 ; i < m_cElements ; i++ ){
			aData[i] = m_aData[i];
		}
		m_aData = aData;
		*/
	}
	public int indexOf( int iElement ){
		int i = 0;
		for( i = 0 ; i < m_cElements ; i++ ){
			if( m_aData[i] == iElement ){
				return i;
			}
		}
		return -1;
		
	}
	public boolean contains( int iElement ) {
		return indexOf( iElement ) != -1;
	}
	public int size() {
		return m_cElements;
	}
	public void addAll( IntVector v ){
		int iIndex = 0;
		for( iIndex = 0 ; iIndex < v.m_cElements ; iIndex++ ){
			add( v.elementAt( iIndex ) );
		}
	}
	public void release(){
		clear();
		if( m_bSmall )
			m_ivFactory.recycleSmall( this );
		else
			m_ivFactory.recycleLarge( this );			
	}
}
