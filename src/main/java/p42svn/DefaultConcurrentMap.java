package p42svn;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Pavel Belevich
 *         Date: 5/21/11
 *         Time: 8:56 PM
 */
public class DefaultConcurrentMap<K, V> implements ConcurrentMap<K, V> {

    public static interface DefaultValueFactory<K, V> {
        V getDefaultValue(K key);
    }

    private ConcurrentMap<K, V> delegate;
    private DefaultValueFactory<K, V> defaultValueFactory;

    public DefaultConcurrentMap(ConcurrentMap<K, V> delegate, DefaultValueFactory<K, V> defaultValueFactory) {
        this.delegate = delegate;
        this.defaultValueFactory = defaultValueFactory;
    }

    public V putIfAbsent(K key, V value) {
        return delegate.putIfAbsent(key, value);
    }

    public boolean remove(Object key, Object value) {
        return delegate.remove(key, value);
    }

    public boolean replace(K key, V oldValue, V newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    public V replace(K key, V value) {
        return delegate.replace(key, value);
    }

    public int size() {
        return delegate.size();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public boolean containsKey(Object key) {
        return delegate.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return delegate.containsValue(value);
    }

    @SuppressWarnings({"unchecked"})
    public V get(Object key) {
        V value = delegate.get(key);
        if (value == null) {
            V defaultValue = defaultValueFactory.getDefaultValue((K) key);
            value = delegate.putIfAbsent((K)key, defaultValue);
            if (value == null) {
                value = defaultValue;
            }
        }
        return value;
    }

    public V put(K key, V value) {
        return delegate.put(key, value);
    }

    public V remove(Object key) {
        return delegate.remove(key);
    }

    public void putAll(Map<? extends K, ? extends V> m) {
        delegate.putAll(m);
    }

    public void clear() {
        delegate.clear();
    }

    public Set<K> keySet() {
        return delegate.keySet();
    }

    public Collection<V> values() {
        return delegate.values();
    }

    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    public int hashCode() {
        return delegate.hashCode();
    }
}
