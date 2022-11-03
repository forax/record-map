package com.github.forax.recordmap;

import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A map that mostly implements the Map API but uses a record to represent the Map.Entry instead of an interface.
 *
 * @param <K> type of the key
 * @param <V> type of the value
 */
public class RecordMap<K,V> extends AbstractMap/*<K,V>*/ implements Iterable<RecordMap.RecordEntry<K,V>> {
  public record RecordEntry<K, V>(K key, V value) implements Map.Entry<K, V> {

    public RecordEntry {
      Objects.requireNonNull(key);
      Objects.requireNonNull(value);
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Map.Entry<?,?> entry
          && Objects.equals(entry.getKey(), key)
          && Objects.equals(entry.getValue(), value);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(key) ^ Objects.hashCode(value);
    }

    @Override
    public String toString() {
      return key + "=" + value;
    }

    public static <K, V> RecordEntry<K, V> of(K key, V value) {
      return new RecordEntry<>(key, value);
    }

    public static <K, V> RecordEntry<K, V> of(Map.Entry<? extends K, ? extends V> entry) {
      return of(entry.getKey(), entry.getValue());
    }
  }


  private int size;
  private int[] offsets;
  private RecordEntry<K,V>[] entries;


  public RecordMap() {
    //offsets = new int[16];
    //entries = (RecordEntry<K,V>[]) new RecordEntry<?,?>[8];
    offsets = new int[2];
    entries = (RecordEntry<K,V>[]) new RecordEntry<?,?>[1];
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  public int size() {
    return size;
  }

  private RecordEntry<K, V> newRecordEntry(Object key, Object value) {
    return new RecordEntry<>((K) key, (V) value);
  }

  private void rehash() {
    var offsets = new int[this.offsets.length << 1];
    loop: for (int i = 0; i < entries.length; i++) {
      var e = entries[i];
      var index = e.key.hashCode() & (offsets.length - 1);
      for (; ; ) {
        if (offsets[index] == 0) {
          offsets[index] = i + 1;
          continue loop;
        }
        // FIXME this will create clusters
        index = (index + 1) & (offsets.length - 1);
      }
    }
    this.offsets = offsets;
    this.entries = Arrays.copyOf(entries, entries.length << 1);
  }

  @Override
  public V put(Object/*K*/ key, Object/*V*/ value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        if (entries.length == size) {
          rehash();
          index = key.hashCode() & (offsets.length - 1);
          continue;  // restart
        }
        entries[size] = newRecordEntry(key, value);
        offsets[index] = size + 1;
        size++;
        return null;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        var existingValue = entry.value;
        entries[offset - 1] = newRecordEntry(key, value);
        return existingValue;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public V putIfAbsent(Object/*K*/ key, Object/*V*/ value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        if (entries.length == size) {
          rehash();
          index = key.hashCode() & (offsets.length - 1);
          continue;  // restart
        }
        entries[size] = newRecordEntry(key, value);
        offsets[index] = size + 1;
        size++;
        return null;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        return entry.value;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public V computeIfAbsent(Object/*K*/ key, Function/*<? super K, ? extends V>*/ mappingFunction) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(mappingFunction);
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        if (entries.length == size) {
          rehash();
          index = key.hashCode() & (offsets.length - 1);
          continue;  // restart
        }
        var value = (V) mappingFunction.apply(key);
        entries[size] = newRecordEntry(key, value);
        offsets[index] = size + 1;
        size++;
        return value;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        return entry.value;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public V compute(Object/*K*/ key, BiFunction/*<? super K, ? super V, ? extends V>*/ remappingFunction) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(remappingFunction);
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        if (entries.length == size) {
          rehash();
          index = key.hashCode() & (offsets.length - 1);
          continue;  // restart
        }
        var newValue = (V) remappingFunction.apply(key, null);
        entries[size] = newRecordEntry(key, newValue);
        offsets[index] = size + 1;
        size++;
        return newValue;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        var newValue = (V) remappingFunction.apply(key, entry.value);
        entries[offset - 1] = newRecordEntry(key, newValue);
        return newValue;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public V computeIfPresent(Object/*K*/ key, BiFunction/*<? super K, ? super V, ? extends V>*/ remappingFunction) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(remappingFunction);
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        return null;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        var newValue = (V) remappingFunction.apply(key, entry.value);
        entries[offset - 1] = newRecordEntry(key, newValue);
        return newValue;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public V merge(Object/*<K>*/ key, Object/*V*/ value, BiFunction/*<? super V, ? super V, ? extends V>*/ remappingFunction) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    Objects.requireNonNull(remappingFunction);
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        if (entries.length == size) {
          rehash();
          index = key.hashCode() & (offsets.length - 1);
          continue;  // restart
        }
        entries[size] = newRecordEntry(key, value);
        offsets[index] = size + 1;
        size++;
        return (V) value;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        var newValue = (V) remappingFunction.apply(entry.value, value);
        entries[offset - 1] = newRecordEntry(key, newValue);
        return newValue;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public V replace(Object/*K*/ key, Object/*V*/ value) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(value);
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        return null;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        var existingValue = entry.value;
        entries[offset - 1] = newRecordEntry(key, value);
        return existingValue;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public boolean replace(Object/*K*/ key, Object/*V*/ oldValue, Object/*V*/ newValue) {
    Objects.requireNonNull(key);
    Objects.requireNonNull(oldValue);
    Objects.requireNonNull(newValue);
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        return false;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        if (oldValue.equals(entry.value)) {
          entries[offset - 1] = newRecordEntry(key, newValue);
          return true;
        }
        return false;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  private static <K,V> V getOrDefault(int[] offsets, RecordEntry<K,V>[] entries, Object key, V defaultValue) {
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        return defaultValue;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        return entry.value;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public V getOrDefault(Object key, Object/*V*/ defaultValue) {
    Objects.requireNonNull(key);
    return getOrDefault(offsets, entries, key, (V) defaultValue);
  }

  @Override
  public V get(Object key) {
    return (V) getOrDefault(key, null);
  }

  @Override
  public boolean containsKey(Object key) {
    Objects.requireNonNull(key);
    return containsKey(offsets, entries, key);
  }

  private static <K,V> boolean containsKey(int[] offsets, RecordEntry<K,V>[] entries, Object key) {
    var index = key.hashCode() & (offsets.length - 1);
    int offset;
    for (;;) {
      if ((offset = offsets[index]) == 0) {
        return false;
      }
      var entry = entries[offset - 1];
      if (key.equals(entry.key)) {
        return true;
      }
      // FIXME this will create clusters
      index = (index + 1) & (offsets.length - 1);
    }
  }

  @Override
  public boolean containsValue(Object value) {
    Objects.requireNonNull(value);
    for (var i = 0; i < size; i++) {
      var entry = entries[i];
      if (value.equals(entry.value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void putAll(Map/*<? extends K, ? extends V>*/ m) {
    Objects.requireNonNull(m);
    m.forEach(this::put);
  }

  @Override
  public void replaceAll(BiFunction/*<? super K, ? super V, ? extends V>*/ function) {
    Objects.requireNonNull(function);
    for(var i = 0; i < size; i++) {
      var entry = entries[i];
      var key = entry.key;
      var value = entry.value;
      entries[i] = newRecordEntry(key, function.apply(key, value));
    }
  }

  @Override
  public Object remove(Object key) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean remove(Object key, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() {
    size = 0;
    offsets = new int[16];
    entries = (RecordEntry<K,V>[]) new RecordEntry<?,?>[8];
  }

  @Override
  public void forEach(BiConsumer/*<? super K, ? super V>*/ action) {
    for (var i = 0; i < size; i++) {
      var entry = entries[i];
      action.accept(entry.key, entry.value);
    }
  }

  @Override
  public Iterator<RecordEntry<K, V>> iterator() {
    return recordIterator(size, entries);
  }

  @Override
  public void forEach(Consumer<? super RecordEntry<K, V>> action) {
    for (var i = 0; i < size; i++) {
      action.accept(entries[i]);
    }
  }

  private static <K,V> Iterator<RecordEntry<K, V>> recordIterator(int size, RecordEntry<K,V>[] entries) {
    return new Iterator<>() {
      private int index;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public RecordEntry<K, V> next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return entries[index++];
      }
    };
  }

  public Set<RecordEntry<K,V>>/*Set<Map.Entry<K,V>>*/ entrySet() {
    // snapshot
    var size = this.size;
    var offsets = this.offsets;
    var entries = this.entries;
    return new AbstractSet<>() {
      @Override
      public int size() {
        return size;
      }

      @Override
      public Iterator<RecordEntry<K,V>> iterator() {
        return recordIterator(size, entries);
      }

      @Override
      public boolean contains(Object o) {
        if (!(o instanceof Map.Entry<?,?> entry)) {
          return false;
        }
        return getOrDefault(offsets, entries, o, null) == null;
      }
    };
  }

  private static <K> Iterator<K> keyIterator(int size, RecordEntry<K,?>[] entries) {
    return new Iterator<>() {
      private int index;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public K next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return entries[index++].key;
      }
    };
  }

  public Set<K> keySet() {
    // snapshot
    var size = this.size;
    var offsets = this.offsets;
    var entries = this.entries;
    return new AbstractSet<K>() {
      @Override
      public Iterator<K> iterator() {
        return keyIterator(size, entries);
      }

      @Override
      public int size() {
        return size;
      }

      @Override
      public boolean contains(Object o) {
        return containsKey(offsets, entries, o);
      }
    };
  }

  private static <V> Iterator<V> valueIterator(int size, RecordEntry<?,V>[] entries) {
    return new Iterator<>() {
      private int index;

      @Override
      public boolean hasNext() {
        return index < size;
      }

      @Override
      public V next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return entries[index++].value;
      }
    };
  }

  @Override
  public List<V>/*List<V>*/ values() {
    // snapshot
    var size = this.size;
    var offsets = this.offsets;
    var entries = this.entries;
    return new AbstractList<>() {
      @Override
      public V get(int index) {
        Objects.checkIndex(index, size);
        return entries[index].value;
      }

      @Override
      public Iterator<V> iterator() {
        return valueIterator(size, entries);
      }

      @Override
      public boolean contains(Object o) {
        if (!(o instanceof Map.Entry<?, ?> entry)) {
          return false;
        }
        return getOrDefault(offsets, entries, entry.getKey(), null) != null;
      }

      @Override
      public int size() {
        return size;
      }
    };
  }
}
