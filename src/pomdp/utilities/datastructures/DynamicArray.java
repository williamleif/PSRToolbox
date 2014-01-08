package pomdp.utilities.datastructures;

import java.util.AbstractList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import pomdp.utilities.AlphaVector;

public class DynamicArray<V> implements List<V>, Collection<V> {

	private V[] m_avData;
	private int m_cElements;
	
	public DynamicArray(){
		m_cElements = 0;
		m_avData = (V[]) new Object[1000];
	}
	
	public DynamicArray(DynamicArray<V> array) {
		m_cElements = array.m_cElements;
		m_avData = (V[]) new Object[array.m_avData.length];
		for( int i = 0 ; i < m_cElements ; i++ )
			m_avData[i] = array.m_avData[i];
	}

	public DynamicArray(Collection<V> c) {
		this();
		for( V e : c )
			add( e );
	}

	public V elementAt( int i ){
		return get( i );
	}
	
	@Override
	public V get(int i) {
		return m_avData[i];
	}

	@Override
	public int size() {
		return m_cElements;
	}

	@Override
	public boolean add(V e) {
		m_avData[m_cElements] = e;
		m_cElements++;
		return true;
	}

	@Override
	public void add(int index, V element) {
		for( int i = m_cElements ; i > index ; i++ )
			m_avData[i] = m_avData[i-1];
		m_avData[index] = element;
		m_cElements++;
	}

	@Override
	public boolean addAll(Collection<? extends V> c) {
		for( V e : c )
			add( e );
		return true;
	}

	@Override
	public boolean addAll(int index, Collection<? extends V> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void clear() {
		m_cElements = 0;
	}

	@Override
	public boolean contains(Object o) {
		for( int i = 0 ; i < m_cElements ; i++ )
			if( m_avData[i].equals( o ) )
				return true;
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for( Object e : c )
			if( !contains( e ) )
				return false;
		return true;
	}

	@Override
	public int indexOf(Object o) {
		for( int i = 0 ; i < m_cElements ; i++ )
			if( m_avData[i].equals( o ) )
				return i;
		return -1;
	}

	@Override
	public boolean isEmpty() {
		return m_cElements == 0;
	}

	@Override
	public Iterator<V> iterator() {
		return new ArrayIterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public ListIterator<V> listIterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ListIterator<V> listIterator(int index) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean remove(Object o) {
		int idx = indexOf( o );
		if( idx >= 0 ){
			remove( idx );
			return true;
		}
		return false;
	}

	@Override
	public V remove(int index) {
		V e = m_avData[index];
		for( int i = index ; i < m_cElements ; i++ )
			m_avData[i] = m_avData[i + 1];
		m_cElements--;
		return e;
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		for( Object o : c )
			remove( o );
		return true;
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public V set(int index, V element) {
		V e = m_avData[index];
		m_avData[index] = element;
		return e;
	}

	@Override
	public List<V> subList(int fromIndex, int toIndex) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] toArray() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		// TODO Auto-generated method stub
		return null;
	}

	private class ArrayIterator implements Iterator<V>{

		private int m_iIdx;
		
		public ArrayIterator(){
			m_iIdx = 0;
		}
		
		@Override
		public boolean hasNext() {
			return m_iIdx < m_cElements;
		}

		@Override
		public V next() {
			return m_avData[m_iIdx++];
		}

		@Override
		public void remove() {
			// TODO Auto-generated method stub
			
		}

		
	}

	public V lastElement() {
		return m_avData[m_cElements - 1];
	}
	
}
