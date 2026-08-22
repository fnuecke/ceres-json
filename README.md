# Ceres JSON

Gson-backed JSON serialization backend for [Ceres](https://github.com/fnuecke/ceres).

```java
final JsonObject json = JsonSerialization.serialize(value, Value.class);
final Value value = JsonSerialization.deserialize(json, Value.class, null);
```

## Format notes

- Enums are serialized by ordinal. Reordering enum constants breaks existing data.
- Non-finite floats and doubles are encoded as the strings `"nan"`, `"inf"`, `"-inf"`.
- Object arrays follow the semantics of Ceres's binary backend: elements are serialized with the array's component type, so polymorphic elements are rejected.
