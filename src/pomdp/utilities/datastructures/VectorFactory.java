package pomdp.utilities.datastructures;

import java.io.Serializable;
import java.util.Stack;

public abstract class VectorFactory<VType extends VectorBase> implements Serializable{
	private Stack<VType> m_sLargeVectors;
	private Stack<VType> m_sSmallVectors;
	
	protected int ITERATION_SIZE = 10000;
	
	private long m_cSmallRecycling, m_cSmallMax, m_cSmallMin, m_cSmallSumSizes, m_cSmallTotal;
	private long m_cLargeRecycling, m_cLargeMax, m_cLargeMin, m_cLargeSumSizes, m_cLargeTotal;
	
	public VectorFactory(){
		m_sLargeVectors = new Stack<VType>();
		m_sSmallVectors = new Stack<VType>();
		
		m_cSmallRecycling = 0;
		m_cSmallMax = 0;
		m_cSmallMin = Integer.MAX_VALUE;
		m_cSmallSumSizes = 0;
		m_cSmallTotal = 0;
		
		m_cLargeRecycling = 0;
		m_cLargeMax = 0;
		m_cLargeMin = Integer.MAX_VALUE;
		m_cLargeSumSizes = 0;
		m_cLargeTotal = 0;
	}
	public abstract VType newInstance( int iSize, boolean bSmall );
	
	public VType getSmallVector(){
		VType v = newInstance( 16, true );
		return v;
	}
	
	public VType getLargeVector(){
		VType v = newInstance( 256, false );
		return v;
	}
	public void recycleSmall( VType v ){
	}
	public void recycleLarge( VType v ){
	}
	public void clear(){
		for( VType v : m_sSmallVectors ){
			v.clearFactory();
		}
		m_sSmallVectors.clear();
		for( VType v : m_sLargeVectors ){
			v.clearFactory();
		}
		m_sLargeVectors.clear();
	}
}
