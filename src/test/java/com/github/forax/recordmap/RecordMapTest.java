package com.github.forax.recordmap;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static java.util.stream.IntStream.range;
import static org.junit.jupiter.api.Assertions.*;

public class RecordMapTest {

  @Test
  public void size() {
    var map = new RecordMap<String, Integer>();
    map.put("Ana", 12);
    map.put("Bob", 17);
    assertEquals(2, map.size());
  }

  @Test
  public void isEmpty() {
    var map = new RecordMap<Integer, String>();
    assertTrue(map.isEmpty());
    map.put(0, "cat");
    assertFalse(map.isEmpty());
  }

  @Test
  public void containsValue() {
    var map = new RecordMap<String, Integer>();
    map.put("Yohan", 12);
    map.put("Ian", 17);
    assertAll(
        () -> assertTrue(map.containsValue(12)),
        () -> assertFalse(map.containsValue(42))
    );
  }

  @Test
  public void containsKey() {
    var map = new RecordMap<String, Integer>();
    map.put("Esther", 12);
    map.put("Elana", 17);
    assertAll(
        () -> assertTrue(map.containsKey("Elana")),
        () -> assertFalse(map.containsKey("John"))
    );
  }

  @Test
  public void get() {
    var map = new RecordMap<String, Integer>();
    map.put("Job", 12);
    map.put("Bob", 17);
    assertAll(
        () -> assertEquals(17, map.get("Bob")),
        () -> assertNull(map.get("Jane"))
    );
  }

  @Test
  public void getOrDefault() {
    var map = new RecordMap<String, Integer>();
    map.put("Von", 128);
    map.put("De", 48);
    assertAll(
        () -> assertEquals(128, map.getOrDefault("Von", 128)),
        () -> assertEquals(48, map.getOrDefault("De", 48)),
        () -> assertEquals(-1, map.getOrDefault("Ben", -1))
    );
  }

  @Test
  public void put() {
    var map = new RecordMap<String, Integer>();
    assertNull(map.put("Ana", 12));
    assertNull(map.put("Bob", 17));
    assertEquals(12, map.put("Ana", 43));
    assertAll(
        () -> assertEquals(2, map.size()),
        () -> assertEquals(43, map.getOrDefault("Ana", null)),
        () -> assertEquals(17, map.getOrDefault("Bob", null)),
        () -> assertNull(map.getOrDefault("Ella", null))
    );
  }

  @Test
  public void put2() {
    var map = new RecordMap<Integer, Integer>();
    range(0, 1_000_000).forEach(i -> map.put(i, i));
    var counter = 0;
    for(var entry: map) {
      assertEquals(counter, entry.key());
      assertEquals(counter, entry.value());
      counter++;
    }
  }

  @Test
  public void putIfAbsent() {
    var map = new RecordMap<Integer, Integer>();
    map.put("foo", 3);
    assertNull(map.putIfAbsent("bar", 10));
    assertEquals(3, map.putIfAbsent("foo", 4));
    assertAll(
        () -> assertEquals(2, map.size()),
        () -> assertEquals(3, map.getOrDefault("foo", null)),
        () -> assertEquals(10, map.getOrDefault("bar", null))
    );
  }

  @Test
  public void putIfAbsent2() {
    var map = new RecordMap<Integer, Integer>();
    range(0, 1_000_000).forEach(i -> map.putIfAbsent(i, i));
    var counter = 0;
    for(var entry: map) {
      assertEquals(counter, entry.key());
      assertEquals(counter, entry.value());
      counter++;
    }
  }

  @Test
  public void remove() {
    var map = new RecordMap<String, Integer>();
    assertThrows(UnsupportedOperationException.class, () -> map.remove("foo"));
  }

  @Test
  public void removeTwoParameters() {
    var map = new RecordMap<String, Integer>();
    assertThrows(UnsupportedOperationException.class, () -> map.remove("foo", 3));
  }



  @Test
  public void putAll() {
    var map = new RecordMap<String, Integer>();
    map.putAll(Map.of("Iga", 3, "Olga", 4, "Sega", 8));
    assertAll(
        () -> assertEquals(3, map.size()),
        () -> assertEquals(3, map.getOrDefault("Iga", null)),
        () -> assertEquals(4, map.getOrDefault("Olga", null)),
        () -> assertEquals(8, map.getOrDefault("Sega", null)),
        () -> assertNull(map.getOrDefault("Ada", null))
    );
  }

  @Test
  public void clear() {
    var map = new RecordMap<String, String>();
    map.put("foo", "bar");
    map.clear();
    assertAll(
        () -> assertEquals(0, map.size()),
        () -> assertNull(map.getOrDefault("foo", null))
    );
  }

  @Test
  public void iterator() {
    var map = new RecordMap<String, String>();
    range(0, 1_000_000).forEach(i -> map.put("" + i, "" + i));
    var counter = 0;
    for(var entry: map) {
      assertEquals("" + counter, entry.getKey());
      assertEquals("" + counter, entry.getValue());
      counter++;
    }
  }

  @Test
  public void entrySet() {
    var map = new RecordMap<String, String>();
    range(0, 1_000_000).forEach(i -> map.put("" + i, "" + i));
    var counter = 0;
    for(var entry: map.entrySet()) {
      assertEquals("" + counter, entry.getKey());
      assertEquals("" + counter, entry.getValue());
      counter++;
    }
  }

  @Test
  public void keySet() {
    var map = new RecordMap<String, String>();
    map.put("foo", "bar");
    map.put("baz", "whizz");
    var keySet = map.keySet();
    map.put("", "");
    assertAll(
        () -> assertEquals(2, keySet.size()),
        () -> assertEquals(Set.of("foo", "baz"), keySet),
        () -> assertEquals("[foo, baz]", keySet.toString())
    );
  }

  @Test
  public void keySet2() {
    var map = new RecordMap<Integer, Integer>();
    range(0, 1_000_000).forEach(i -> map.put(i, i));
    var counter = 0;
    for(var key: map.keySet()) {
      assertEquals(counter++, key);
    }
  }

  @Test
  public void values() {
    var map = new RecordMap<String, Object>();
    map.put("foo", 56);
    map.put("baz", "bar");
    var values = map.values();
    map.put("", "");
    assertAll(
        () -> assertEquals(2, values.size()),
        () -> assertEquals(List.of(56, "bar"), values),
        () -> assertEquals("[56, bar]", values.toString())
    );
  }

  @Test
  public void values2() {
    var map = new RecordMap<Integer, Integer>();
    range(0, 1_000_000).forEach(i -> map.put(i, i));
    var counter = 0;
    for(var value: map.values()) {
      assertEquals(counter++, value);
    }
  }

  @Test
  public void testEquals() {
    var map = new RecordMap<String, Object>();
    map.put("foo", 56);
    map.put("baz", "bar");
    assertAll(
        () -> assertEquals(Map.of("foo", 56, "baz", "bar"), map),
        () -> assertEquals(map, Map.of("foo", 56, "baz", "bar"))
    );
  }

  @Test
  public void testHashCode() {
    var map = new RecordMap<String, Object>();
    map.put("foo", 56);
    map.put("baz", "bar");
    assertEquals(Map.of("foo", 56, "baz", "bar").hashCode(), map.hashCode());
  }

  @Test
  public void testToString() {
    var map = new RecordMap<String, Object>();
    range(0, 10).
        forEach(i -> map.put(i, i));
    assertEquals("{0=0, 1=1, 2=2, 3=3, 4=4, 5=5, 6=6, 7=7, 8=8, 9=9}", map.toString());
  }

  @Test
  public void forEachBiConsumer() {
    var map = new RecordMap<Integer, Integer>();
    range(0, 1_000_000).
        forEach(i -> map.put(i, i));
    var box = new Object() { int counter; };
    map.forEach((key, value) -> {
      assertEquals(box.counter, key);
      assertEquals(box.counter, value);
      box.counter++;
    });
  }

  @Test
  public void forEachRecord() {
    var map = new RecordMap<Integer, Integer>();
    range(0, 1_000_000).
        forEach(i -> map.put(i, i));
    var box = new Object() { int counter; };
    map.forEach(entry -> {
      assertEquals(box.counter, entry.key());
      assertEquals(box.counter, entry.value());
      box.counter++;
    });
  }

  @Test
  public void replace() {
    var map = new RecordMap<String, Integer>();
    map.put("foo", 17);
    map.put("bar", 54);
    assertEquals(17, map.replace("foo", 37));
    assertNull(map.replace("baz", 20));
  }

  @Test
  public void replaceThreeParameters() {
    var map = new RecordMap<String, Integer>();
    map.put("foo", 21);
    map.put("bar", 22);
    assertTrue(map.replace("foo", 21, 101));
    assertFalse(map.replace("bar", 99, 101));
    assertFalse(map.replace("baz", 66, 101));
    assertAll(
        () -> assertEquals(2, map.size()),
        () -> assertEquals(101, map.getOrDefault("foo", null)),
        () -> assertEquals(22, map.getOrDefault("bar", null))
    );
  }

  @Test
  public void replaceAll() {
    var map = new RecordMap<Integer, Integer>();
    range(0, 1_000_000).forEach(i -> map.put(i, i));
    map.replaceAll((k, v) -> ((Integer) v) + 5);
    var box = new Object() { int counter = 0; };
    map.forEach((key, value) -> {
      assertEquals(box.counter, key);
      assertEquals(box.counter + 5, value);
      box.counter++;
    });
  }

  @Test
  public void computeIfAbsent() {
    var map = new RecordMap<String, Integer>();
    assertEquals(10, map.computeIfAbsent("foo", key -> 10));
    assertEquals(100, map.computeIfAbsent("bar", key -> 100));
    assertEquals(10, map.computeIfAbsent("foo", key -> 1_000));
    assertAll(
        () -> assertEquals(2, map.size()),
        () -> assertEquals(10, map.getOrDefault("foo", null)),
        () -> assertEquals(100, map.getOrDefault("bar", null))
    );
  }

  @Test
  public void computeIfAbsent2() {
    var map = new RecordMap<Integer, Integer>();
    range(0, 1_000_000).forEach(i -> map.computeIfAbsent(i, key -> key));
    var counter = 0;
    for(var entry: map) {
      assertEquals(counter, entry.key());
      assertEquals(counter, entry.value());
      counter++;
    }
  }

  @Test
  public void computeIfPresent() {
    var map = new RecordMap<Integer, String>();
    map.put(10, "foo");
    map.put(20, "bar");
    assertEquals("foo2", map.computeIfPresent(10, (k, v) -> v + "2" ));
    assertNull(map.computeIfPresent(100, (k, v) -> v + "3"));
    assertAll(
        () -> assertEquals(2, map.size()),
        () -> assertEquals("foo2", map.getOrDefault(10, "")),
        () -> assertEquals("bar", map.getOrDefault(20, ""))
    );
  }

  @Test
  public void compute() {
    var map = new RecordMap<Integer, String>();
    map.put(10, "foo");
    assertEquals("1:foo", map.compute(10, (k, v) -> "1:" + v));
    assertEquals("2:null", map.compute(100, (k, v) -> "2:" + v));
    assertAll(
        () -> assertEquals(2, map.size()),
        () -> assertEquals("1:foo", map.getOrDefault(10, "")),
        () -> assertEquals("2:null", map.getOrDefault(100, ""))
    );
  }

  @Test
  public void merge() {
    var map = new RecordMap<Integer, String>();
    var concat = (BiFunction<String, String, String>) String::concat;
    assertEquals("foo", map.merge(10, "foo", concat));
    assertEquals("baz", map.merge(100, "baz", concat));
    assertEquals("foobar", map.merge(10, "bar", concat));
    assertAll(
        () -> assertEquals(2, map.size()),
        () -> assertEquals("foobar", map.getOrDefault(10, "")),
        () -> assertEquals("baz", map.getOrDefault(100, ""))
    );
  }
}