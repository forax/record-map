# RecordMap
`RecordMap` is a map implementation that mostly supports the `java.util.Map` API but uses
a record `RecordEntry` instead of `Map.Entry` for its entry set.

Java 20 introduces the ability to use record pattern in enhanced for but
sadly `Map.Entry` is not a record so this code does not compile
```java
  Map<String, String> map = ...
  for(Map.Entry(var key, var value) : map.entrySet()) {
    ...
  }
```
At least not yet.

The class `RecordMap` solves that issue, the following compiles with Java 20
```java
  RecordMap<String, String> map = ...
  for(RecordEntry(var key, var value) : map.entrySet()) {
    ...
  }
```

Obviously there is a trick somewhere because `Map.entrySet()` is typed
`Set<Map.Entry<K,V>` and `Set<RecordEntry<K,V>` can not be a subtype of
`Set<Map.Entry<K, V>` because parametrized types are not covariant in Java.

This works because
- at compile time, the actual code is using a raw type `Map` and then the signature
  of the methods are later patched by rewriting the bytecode.
- at runtime, the implementation of the entry set returned by `entrySet()`
  throws an `UnsupportedoperationException` for any methods that takes an entry
  as parameter.

In practice, it makes `RecordMap` a valid implementation of `Map` by making
`Set<RecordEntry<K,V>> entrySet()` an override of `Set<Map.Entry<K,V>>`.







