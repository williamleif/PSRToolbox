
package pomdp.utilities.datastructures;


import java.util.Iterator;
import java.util.Vector;

public class Heap implements PriorityQueue{
	
	private Vector<PriorityQueueElement> m_vData; 
	private int m_cSwaps;
	
	public Heap(){
		m_vData = new Vector<PriorityQueueElement>();
		m_vData.add( null );
		m_cSwaps = 0;
	}
	
	public PriorityQueueElement extractMax() {
		if( isEmpty() )
			return null;
		PriorityQueueElement eFirst = m_vData.elementAt( 1 );
		PriorityQueueElement eLast = m_vData.remove( m_vData.size() - 1 );
		if( !isEmpty() ){
			m_vData.set( 1, eLast );
			eLast.setLocation( 1 );
			moveDown( 1 );
		}
		eFirst.clear();
		return eFirst;
	}

	public void insert( PriorityQueueElement element ){
		int idx = element.getLocation();
		if( ( idx <= 0 ) || ( idx >= m_vData.size() ) || ( m_vData.elementAt( idx ) != element ) ){
			m_vData.add( element );
			element.setContainer( this );
			element.setLocation( m_vData.size() - 1 );
			moveUp( m_vData.size() - 1 );
		}
	}
	
	private int parent( int idx ){
		return idx / 2;
	}
	
	private int lChild( int idx ){
		return idx * 2;
	}
	
	private int rChild( int idx ){
		return idx * 2 + 1;
	}
	
	private void swap( int idx1, int idx2 ){
		PriorityQueueElement element1 = m_vData.elementAt( idx1 );
		PriorityQueueElement element2 = m_vData.elementAt( idx2 );
		m_vData.set( idx1, element2 );
		m_vData.set( idx2, element1 );
		element2.setLocation( idx1 );
		element1.setLocation( idx2 );
		m_cSwaps++;
	}
	
	private void moveUp( int iChild ){
		int iParent = parent( iChild );
		
		if( iParent > 0 ){
			PriorityQueueElement pqChild = m_vData.elementAt( iChild );
			PriorityQueueElement pqParent = m_vData.elementAt( iParent );
			
			if( pqChild.getPriority() >  pqParent.getPriority() ){
				swap( iParent, iChild );
				moveUp( iParent );
			}		
		}		
	}
	
	private void moveDown( int iParent ){
		int iLeft = lChild( iParent );
		int iRight = rChild( iParent );
		int iMax = iParent;
		PriorityQueueElement pqLeft = null, pqRight = null, pqParent = m_vData.elementAt( iParent );
		
		if( iLeft < m_vData.size() ){
			pqLeft = m_vData.elementAt( iLeft );
			if( pqLeft.getPriority() > pqParent.getPriority() ){
				iMax = iLeft;
			}
			if( iRight < m_vData.size() ){
				pqRight = m_vData.elementAt( iRight );
				if( pqRight.getPriority() > (m_vData.elementAt( iMax )).getPriority() ){
					iMax = iRight;
				}
			}
		}
		if( iMax != iParent ){
			swap( iMax, iParent );
			moveDown( iMax );
		}
	}

	public boolean isEmpty() {
		return m_vData.size() == 1;
	}

	public void adjust( PriorityQueueElement pqElement ){
		int iParent = parent( pqElement.getLocation() );
		PriorityQueueElement pqParent = m_vData.elementAt( iParent );
		if( ( pqParent!= null ) && ( pqParent.getPriority() < pqElement.getPriority() ) )
			moveUp( pqElement.getLocation() );
		else
			moveDown( pqElement.getLocation() );
	}

	public Iterator<PriorityQueueElement> iterator() {
		Iterator<PriorityQueueElement> it = new Vector<PriorityQueueElement>( m_vData ).iterator();
		it.next();
		return it;
	}

	public int size() {
		return m_vData.size() - 1;
	}

	public void clear() {
		Iterator<PriorityQueueElement> itElements = m_vData.iterator();
		PriorityQueueElement pqe = null;
		
		itElements.next(); //skip the first null;
		while( itElements.hasNext() ){
			pqe = itElements.next();
			pqe.clear();
		}
			
		m_vData.clear();
		m_vData.add( null );

		
	}

	public int swapCount() {
		return m_cSwaps;
	}

}
