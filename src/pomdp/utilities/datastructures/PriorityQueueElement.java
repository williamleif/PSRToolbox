package pomdp.utilities.datastructures;

public class PriorityQueueElement{
	private double m_dPriority;
	private int m_iLocation;
	private PriorityQueue m_pqContainer;
	
	public PriorityQueueElement(){
		m_dPriority = 0.0;
		m_iLocation = -1;
		m_pqContainer = null;
	}
	public PriorityQueueElement( PriorityQueue pqContainer, double dPriority, int iLocation ){
		m_dPriority = dPriority;
		m_iLocation = iLocation;
		m_pqContainer = pqContainer;
	}
	public void setContainer( PriorityQueue pqContainer ){
		m_pqContainer = pqContainer;
	}
	
	public void increasePriority( double dPriority ){
		if( m_dPriority < dPriority ){
			m_dPriority = dPriority;
			if( m_pqContainer != null )
				m_pqContainer.adjust( this );
		}
	}
	
	public void addPriority( double dPriority ){
		m_dPriority += dPriority;
		if( m_pqContainer != null )
			m_pqContainer.adjust( this );
	}
	
	public void setPriority( double dPriority ){
		if( m_dPriority != dPriority ){
			m_dPriority = dPriority;
			if( m_pqContainer != null )
				m_pqContainer.adjust( this );
		}
	}
	
	public double getPriority(){
		return m_dPriority;
	}
	public void setLocation( int iLocation ){
		m_iLocation = iLocation;
	}
	public int getLocation(){
		return m_iLocation;
	}
	public void clear(){
		//m_dPriority = Double.MAX_VALUE * -1;
		m_iLocation = -1;
		setContainer( null );
	}
}
