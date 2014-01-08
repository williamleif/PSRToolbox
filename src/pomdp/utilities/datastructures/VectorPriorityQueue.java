package pomdp.utilities.datastructures;

import java.util.Iterator;
import java.util.Vector;

public class VectorPriorityQueue implements PriorityQueue {

	private Vector<PriorityQueueElement> m_vData;
	
	public VectorPriorityQueue(){
		m_vData = new Vector<PriorityQueueElement>();
	}
	
	public VectorPriorityQueue( int iCapacity ){
		m_vData = new Vector<PriorityQueueElement>( iCapacity );
	}
	
	public PriorityQueueElement extractMax() {
		PriorityQueueElement pqeFirst = m_vData.firstElement();
		boolean bDone = m_vData.removeElement( pqeFirst );
		return pqeFirst;
	}

	public void insert( PriorityQueueElement element ){
		int idx = 0;
		if( isEmpty() ){
			m_vData.add( element );
		}
		else if( m_vData.elementAt( 0 ).getPriority() <= element.getPriority() ){
			m_vData.add( 0, element );
		}
		else if( m_vData.lastElement().getPriority() >= element.getPriority() ){
			m_vData.add( element );
		}
		else{ 
			for( idx = 1 ; idx < m_vData.size() ; idx++ ){
				if( m_vData.elementAt( idx ).getPriority() <= element.getPriority() ){
					m_vData.add( idx, element );
					return;
				}
			}
		}
	}
	
	private void swap( int idx1, int idx2 ){
		PriorityQueueElement element1 = m_vData.elementAt( idx1 );
		PriorityQueueElement element2 = m_vData.elementAt( idx2 );
		m_vData.set( idx1, element2 );
		m_vData.set( idx2, element1 );
	}

	public void adjust( PriorityQueueElement element ){
		int idx = indexOf( element );
		if( idx == -1 )
			return;
		while( ( idx > 0 ) && ( m_vData.elementAt( idx - 1 ).getPriority() < element.getPriority() ) ){
			swap( idx, idx - 1 );
			idx--;
		}
		while( ( idx < size() - 1 ) && ( m_vData.elementAt( idx + 1 ).getPriority() > element.getPriority() ) ){
			swap( idx, idx + 1 );
			idx++;
		}
	}

	public Iterator iterator() {
		return m_vData.iterator();
	}

	public boolean isEmpty() {
		return m_vData.size() == 0;
	}

	public int size() {
		return m_vData.size();
	}

	public void clear() {
		m_vData.clear();
	}

	public int swapCount() {
		return 0;
	}

	public int indexOf( PriorityQueueElement element ){
		return linearSearch( element, 0, m_vData.size() - 1 );
	}
	
	private int linearSearch( PriorityQueueElement element, int iFirst, int iLast ){
		int idx = 0;
		for( idx = iFirst ; idx <= iLast ; idx++ ){
			if( m_vData.elementAt( idx ) == element ){
				return idx;
			}
		}
		return -1;
	}
}
