package pomdp.utilities.datastructures;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Vector;

public class MultiValueMap<K,V> implements SortedMap<K,V> {

	private SortedMap<K,Vector<V>> m_mTrueMap;
	private int m_cElements;
	
	public MultiValueMap(){
		m_mTrueMap = new TreeMap<K, Vector<V>>();
		m_cElements = 0;
	}
	
	public MultiValueMap( Comparator<K> comp ){
		m_mTrueMap = new TreeMap<K, Vector<V>>( comp );
		m_cElements = 0;
	}
	
	private MultiValueMap( SortedMap<K,Vector<V>> mTrueMap ){
		m_mTrueMap = mTrueMap;
		m_cElements = 0;
	}
	
	@Override
	public Comparator<? super K> comparator() {
		return m_mTrueMap.comparator();
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public K firstKey() {
		return m_mTrueMap.firstKey();
	}

	@Override
	public SortedMap<K, V> headMap(K toKey) {
		return new MultiValueMap<K, V>( m_mTrueMap.headMap( toKey ) );
	}

	@Override
	public Set<K> keySet() {
		return m_mTrueMap.keySet();
	}

	@Override
	public K lastKey() {
		return m_mTrueMap.lastKey();
	}

	public V lastValue() {
		K key = m_mTrueMap.lastKey();
		Vector<V> vValues = m_mTrueMap.get( key );
		return vValues.elementAt( 0 );
	}

	@Override
	public SortedMap<K, V> subMap(K fromKey, K toKey) {
		return new MultiValueMap<K, V>( m_mTrueMap.subMap( fromKey, toKey ) );
	}

	@Override
	public SortedMap<K, V> tailMap(K fromKey) {
		return new MultiValueMap<K, V>( m_mTrueMap.tailMap( fromKey ) );
	}

	@Override
	public Collection<V> values() {
		Vector<V> vAllValues = new Vector<V>();
		for( Vector<V> vValues : m_mTrueMap.values() ){
			vAllValues.addAll( vValues );
		}
		return vAllValues;
	}

	@Override
	public void clear() {
		m_mTrueMap.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return m_mTrueMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for( Vector<V> vValues : m_mTrueMap.values() ){
			if( vValues.contains( value ) )
				return true;
		}
		return false;
	}

	@Override
	public V get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	public Collection<V> getValues( K key ){
		return m_mTrueMap.get( key );
	}
	
	@Override
	public boolean isEmpty() {
		return m_mTrueMap.isEmpty();
	}

	@Override
	public V put(K key, V value) {
		Vector<V> vValues = null;
		if( !m_mTrueMap.containsKey( key ) ){
			vValues = new Vector<V>();
			m_mTrueMap.put( key, vValues ); 
		}
		else{
			vValues = m_mTrueMap.get( key );
		}
		if( !vValues.contains( value ) ){
			vValues.add( value );
			m_cElements++;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public V remove(Object key) {
		return null;
	}

	public void removeEntry(K key, V value) {
		Vector<V> vValues = m_mTrueMap.get( key );
		if( vValues != null ){
			if( vValues.remove( value ) ){
				m_cElements--;
				if( vValues.size() == 0 )
					m_mTrueMap.remove( key );
			}
		}
	}

	@Override
	public int size() {
		return m_cElements;
	}

}
