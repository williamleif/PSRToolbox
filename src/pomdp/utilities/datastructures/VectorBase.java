package pomdp.utilities.datastructures;

import java.io.Serializable;

public abstract class VectorBase implements Serializable{
	protected long m_cElements;
	protected VectorFactory m_vFactory;
	protected boolean m_bSmall;
	protected long m_iSize;
	private long m_iID;
	protected int m_cUses;
	private static long m_cIDs = 0;
	public static int MAX_ARRAY_SIZE = 1000000;
	
	public VectorBase( long iSize, VectorFactory vFactory, boolean bSmall ){
		m_iSize = iSize;
		m_vFactory = vFactory;
		m_bSmall = bSmall;
	}
	public static VectorBase newInstance( long iSize, VectorFactory ivFactory, boolean bSmall ){
		return null;
	}
	protected int getFisrtIndex( long iIndex ){
		return (int)( iIndex / MAX_ARRAY_SIZE );
	}
	protected int getSecondIndex( long iIndex ){
		return (int)( iIndex % MAX_ARRAY_SIZE );
	}
	public long size() {
		return m_cElements;
	}
	public void clear(){
		m_cElements = 0;
	}
	public void release(){
		/*
		clear();
		if( m_bSmall )
			m_vFactory.recycleSmall( this );
		else
			m_vFactory.recycleLarge( this );
			*/			
	}

	public long getID() {
		return m_iID;
	}
	public void addUse(){
		m_cUses++;
	}

	public int getUseCount() {
		return m_cUses;
	}
	public String toString(){
		return "V" + getID();
	}
	public void clearFactory(){
		m_vFactory = null;
	}
}
