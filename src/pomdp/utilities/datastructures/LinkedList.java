package pomdp.utilities.datastructures;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class LinkedList<V> implements List<V> {
	private Link<V> m_lHead, m_lTail;
	private int m_cElements;
	
	public LinkedList(){
		m_lHead = null;
		m_lTail = null;
		m_cElements = 0;
	}
	
	
	public LinkedList( Collection<V> l ) {
		addAll( l );
	}

	@Override
	public boolean add( V vData ) {
		Link<V> lNew = new Link<V>( vData );
		if( m_lTail != null )
			m_lTail.setNext( lNew );
		lNew.setPrevious( m_lTail );
		m_lTail = lNew;
		if( m_lHead == null )
			m_lHead = m_lTail;
		m_cElements++;
		return true;
	}

	@Override
	public void add(int idx, V vData ) {
		throw new NotImplementedException();

	}

	@Override
	public boolean addAll( Collection c ){
		for( V vData : (Collection<V>)c ){
			if( !add( vData ) )
				return false;
		}
		return true;
	}

	@Override
	public boolean addAll(int arg0, Collection arg1) {
		throw new NotImplementedException();
	}

	@Override
	public void clear() {
		m_lHead = null;
		m_lTail = null;
	}

	@Override
	public boolean contains( Object vData ) {
		Link<V> l = get( (V)vData );
		if( l == null )
			return false;
		return true;
	}

	@Override
	public boolean containsAll(Collection arg0) {
		throw new NotImplementedException();
	}

	@Override
	public V get( int idx ) {
		if( idx < 0 || idx >= m_cElements )
			return null;
		Link<V> cur = m_lHead;
		while( idx > 0 ){
			cur = cur.getNext();
			idx--;
		}
		return cur.getData();
	}

	@Override
	public int indexOf( Object arg0 ) {
		throw new NotImplementedException();
	}

	@Override
	public boolean isEmpty() {
		return m_lHead == null;
	}

	@Override
	public Iterator<V> iterator() {
		return new LinkedListIterator<V>( m_lHead, true );
	}

	public Iterator<V> backwardIterator() {
		return new LinkedListIterator<V>( m_lTail, false );
	}

	@Override
	public int lastIndexOf(Object arg0) {
		throw new NotImplementedException();
	}

	@Override
	public ListIterator<V> listIterator() {
		throw new NotImplementedException();
	}

	@Override
	public ListIterator<V> listIterator(int arg0) {
		throw new NotImplementedException();
	}

	protected Link<V> get( V vData ){
		Link<V> lCurrent = m_lHead;
		while( lCurrent != null ){
			if( lCurrent.getData().equals( vData ) )
				return lCurrent;
			lCurrent = lCurrent.getNext();
		}
		return null;
	}
	
	@Override
	public boolean remove( Object oData ){
		V vData = (V)oData;
		Link<V> lData = get( vData );
		if( lData != null ){
			lData.remove();
			m_cElements--;
			return true;
		}
		return false;
	}

	@Override
	public V remove(int idx) {
		throw new NotImplementedException();
	}

	public V removeFirst() {
		if( m_lHead == null )
			return null;
		Link<V> lHead = m_lHead;
		lHead.remove();
		m_cElements--;
		return lHead.getData();
	}

	@Override
	public boolean removeAll(Collection c) {
		throw new NotImplementedException();
	}

	@Override
	public boolean retainAll(Collection c) {
		throw new NotImplementedException();
	}

	@Override
	public V set(int arg0, Object arg1) {
		throw new NotImplementedException();
	}

	@Override
	public int size() {
		return count();
	}

	@Override
	public List subList(int arg0, int arg1) {
		throw new NotImplementedException();
	}

	@Override
	public Object[] toArray() {
		throw new NotImplementedException();
	}

	@Override
	public V[] toArray(Object[] arg0) {
		throw new NotImplementedException();
	}

	private class LinkedListIterator<V> implements Iterator<V>{
		private Link<V> m_lCurrent, m_lLast;
		private boolean m_bForward;
		
		public LinkedListIterator( Link<V> lHead, boolean bForward ){
			m_lCurrent = lHead;
			m_bForward = bForward;
			m_lLast = null;
		}
		@Override
		public boolean hasNext() {
			return m_lCurrent != null;
		}

		@Override
		public V next() {
			V vData = m_lCurrent.getData();
			m_lLast = m_lCurrent;
			if( m_bForward )
				m_lCurrent = m_lCurrent.getNext();
			else
				m_lCurrent = m_lCurrent.getPrevious();
			return vData;
		}

		@Override
		public void remove(){
			if( m_lLast != null ){
				m_lLast.remove();
				m_cElements--;
			}
		}
		
	}
	
	private class Link<V>{
		public V m_vData;
		public Link<V> m_lPrevious, m_lNext;
		public boolean m_bDeleted;
		
		public Link( V vData ){
			m_vData = vData;
			m_bDeleted = false;
		}
		public V getData() {
			return m_vData;
		}
		public Link<V> getNext() {
			return m_lNext;
		}
		public Link<V> getPrevious() {
			return m_lPrevious;
		}
		public void setNext(Link<V> lNext) {
			m_lNext = lNext;			
		}
		public void setPrevious(Link<V> lPrevious) {
			m_lPrevious = lPrevious;
		}
		public synchronized void remove(){
			if( !m_bDeleted ){
				if( m_lTail == this ){
					m_lTail = m_lTail.getPrevious();
					if( m_lTail != null )
						m_lTail.setNext( null );
				}
				if( m_lHead == this ){
					m_lHead = m_lHead.getNext();
					if( m_lHead != null )
						m_lHead.setPrevious( null );
				}
				if( m_lPrevious != null )
					m_lPrevious.setNext( m_lNext );
				if( m_lNext != null ){
					m_lNext.setPrevious( m_lPrevious );
				}
				setNext( null );
				setPrevious( null );
			}
			m_bDeleted = true;
		}
	}

	public V getFirst() {
		if( m_lHead != null )
			return m_lHead.getData();
		return null;
	}

	public V getLast() {
		if( m_lTail != null )
			return m_lTail.getData();
		return null;
	}

	public int count() {
		int cElements = 0;
		Iterator<V> it = iterator();
		while( it.hasNext() ){
			cElements++;
			it.next();
		}
		return cElements;
	}

	public void addSorted( V data, Comparator<V> comp ) {
		add( data );
		Link<V> lCurrent = m_lTail;
		V aux = null;
		while( ( lCurrent != m_lHead ) && ( comp.compare( lCurrent.getData(), lCurrent.getPrevious().getData() ) > 0 ) ){
			aux = lCurrent.getData();
			lCurrent.m_vData = lCurrent.getPrevious().getData();
			lCurrent.getPrevious().m_vData = aux;
			lCurrent = lCurrent.getPrevious();
		}
	}

}
