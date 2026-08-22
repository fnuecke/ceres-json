package li.cil.ceres.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.Serialized;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public final class JsonSerializationTests {
    @Test
    public void flatFieldsAreSerializedCorrectly() {
        final Flat value = new Flat();
        value.booleanValue = true;
        value.byteValue = 123;
        value.charValue = 'å';
        value.shortValue = 234;
        value.intValue = 456;
        value.longValue = Long.MAX_VALUE;
        value.floatValue = 678.9f;
        value.doubleValue = 789.0;
        value.stringValue = "test string";
        value.uuidValue = UUID.randomUUID();

        final JsonObject serialized = assertDoesNotThrow(() -> JsonSerialization.serialize(value, Flat.class));
        final Flat deserialized = assertDoesNotThrow(() -> JsonSerialization.deserialize(serialized, Flat.class, null));

        assertEquals(value.booleanValue, deserialized.booleanValue);
        assertEquals(value.byteValue, deserialized.byteValue);
        assertEquals(value.charValue, deserialized.charValue);
        assertEquals(value.shortValue, deserialized.shortValue);
        assertEquals(value.intValue, deserialized.intValue);
        assertEquals(value.longValue, deserialized.longValue);
        assertEquals(value.floatValue, deserialized.floatValue);
        assertEquals(value.doubleValue, deserialized.doubleValue);
        assertEquals(value.stringValue, deserialized.stringValue);
        assertEquals(value.uuidValue, deserialized.uuidValue);
    }

    @Test
    public void valuesSurviveTextRoundTrip() {
        final Flat value = new Flat();
        value.booleanValue = true;
        value.byteValue = Byte.MIN_VALUE;
        value.charValue = Character.MAX_VALUE;
        value.shortValue = Short.MIN_VALUE;
        value.intValue = Integer.MIN_VALUE;
        value.longValue = Long.MIN_VALUE;
        value.floatValue = Float.MIN_VALUE;
        value.doubleValue = Double.MAX_VALUE;
        value.stringValue = "with \"quotes\" and \0, \n, \t control characters";

        final JsonObject parsed = reparse(JsonSerialization.serialize(value, Flat.class));
        final Flat deserialized = JsonSerialization.deserialize(parsed, Flat.class, null);

        assertEquals(value.booleanValue, deserialized.booleanValue);
        assertEquals(value.byteValue, deserialized.byteValue);
        assertEquals(value.charValue, deserialized.charValue);
        assertEquals(value.shortValue, deserialized.shortValue);
        assertEquals(value.intValue, deserialized.intValue);
        assertEquals(value.longValue, deserialized.longValue);
        assertEquals(value.floatValue, deserialized.floatValue);
        assertEquals(value.doubleValue, deserialized.doubleValue);
        assertEquals(value.stringValue, deserialized.stringValue);
    }

    @Test
    public void primitiveArraysAreSerializedCorrectly() {
        final WithArrays value = new WithArrays();
        value.booleanArray = new boolean[]{true, false, true};
        value.byteArray = new byte[]{Byte.MIN_VALUE, 0, Byte.MAX_VALUE};
        value.charArray = new char[]{'a', 'ä', Character.MAX_VALUE};
        value.shortArray = new short[]{Short.MIN_VALUE, 0, Short.MAX_VALUE};
        value.intArray = new int[]{Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
        value.longArray = new long[]{Long.MIN_VALUE, 0, Long.MAX_VALUE};
        value.floatArray = new float[]{-1.5f, 0f, 1.5f};
        value.doubleArray = new double[]{-2.5, 0, 2.5};

        final JsonObject serialized = JsonSerialization.serialize(value, WithArrays.class);

        assertAllArraysEqual(value, JsonSerialization.deserialize(serialized, WithArrays.class, null));
        assertAllArraysEqual(value, JsonSerialization.deserialize(reparse(serialized), WithArrays.class, null));
    }

    @Test
    public void enumsAreSerializedByOrdinal() {
        final WithEnum value = new WithEnum();
        value.enumValue = TestEnum.THREE;

        final JsonObject serialized = JsonSerialization.serialize(value, WithEnum.class);

        assertEquals(TestEnum.THREE.ordinal(), serialized.get("enumValue").getAsInt());

        assertEquals(value.enumValue, JsonSerialization.deserialize(serialized, WithEnum.class, null).enumValue);
        assertEquals(value.enumValue, JsonSerialization.deserialize(reparse(serialized), WithEnum.class, null).enumValue);
    }

    @Test
    public void nestedObjectsAreSerializedCorrectly() {
        final Outer value = new Outer();
        value.inner = new Inner();
        value.inner.value = 123;

        final JsonObject serialized = JsonSerialization.serialize(value, Outer.class);

        assertTrue(serialized.get("inner").isJsonObject());

        final Outer deserialized = JsonSerialization.deserialize(reparse(serialized), Outer.class, null);

        assertNotNull(deserialized.inner);
        assertEquals(value.inner.value, deserialized.inner.value);
    }

    @Test
    public void subclassSerializesSuperclassFields() {
        final Derived value = new Derived();
        value.baseValue = 123;
        value.derivedValue = 234;

        final JsonObject serialized = JsonSerialization.serialize(value, Derived.class);
        final Derived deserialized = JsonSerialization.deserialize(reparse(serialized), Derived.class, null);

        assertEquals(value.baseValue, deserialized.baseValue);
        assertEquals(value.derivedValue, deserialized.derivedValue);
    }

    @Test
    public void nullValuesAreSerializedAsJsonNull() {
        final Outer value = new Outer();

        final JsonObject serialized = JsonSerialization.serialize(value, Outer.class);

        assertTrue(serialized.get("inner").isJsonNull());
        assertTrue(serialized.get("stringValue").isJsonNull());

        final Outer into = new Outer();
        into.inner = new Inner();
        into.stringValue = "overwrite me";
        final Outer deserialized = JsonSerialization.deserialize(serialized, Outer.class, into);

        assertNull(deserialized.inner);
        assertNull(deserialized.stringValue);
    }

    @Test
    public void deserializingIntoExistingInstanceReusesInstance() {
        final WithEnum value = new WithEnum();
        value.enumValue = TestEnum.TWO;

        final JsonObject serialized = JsonSerialization.serialize(value, WithEnum.class);

        final WithEnum into = new WithEnum();
        final WithEnum deserialized = JsonSerialization.deserialize(serialized, WithEnum.class, into);

        assertSame(into, deserialized);
        assertEquals(TestEnum.TWO, deserialized.enumValue);
    }

    @Test
    public void finalArraysAreDeserializedInPlace() {
        final WithFinalArray value = new WithFinalArray();
        value.values[0] = 23;
        value.values[1] = 64;
        value.values[2] = 420;

        final JsonObject serialized = JsonSerialization.serialize(value, WithFinalArray.class);
        final WithFinalArray deserialized = JsonSerialization.deserialize(serialized, WithFinalArray.class, null);

        assertArrayEquals(value.values, deserialized.values);
    }

    @Test
    public void arrayLengthMismatchAllocatesNewArray() {
        final WithArrays value = new WithArrays();
        value.intArray = new int[]{1, 2, 3, 4};

        final JsonObject serialized = JsonSerialization.serialize(value, WithArrays.class);

        final WithArrays into = new WithArrays();
        into.intArray = new int[]{9, 9};
        final WithArrays deserialized = JsonSerialization.deserialize(serialized, WithArrays.class, into);

        assertArrayEquals(value.intArray, deserialized.intArray);
    }

    @Test
    public void missingPropertiesKeepExistingValues() {
        final WithEnum value = new WithEnum();
        value.enumValue = TestEnum.TWO;
        value.intValue = 123;

        final JsonObject serialized = JsonSerialization.serialize(value, WithEnum.class);
        serialized.remove("intValue");

        final WithEnum into = new WithEnum();
        into.intValue = 234;
        final WithEnum deserialized = JsonSerialization.deserialize(serialized, WithEnum.class, into);

        assertEquals(TestEnum.TWO, deserialized.enumValue);
        assertEquals(234, deserialized.intValue);
    }

    @Test
    public void objectArraysAreNotSupported() {
        final WithObjectArray value = new WithObjectArray();
        value.values = new String[]{"a", "b"};

        assertThrows(SerializationException.class, () -> JsonSerialization.serialize(value, WithObjectArray.class));
    }

    private static JsonObject reparse(final JsonObject json) {
        return JsonParser.parseString(json.toString()).getAsJsonObject();
    }

    private static void assertAllArraysEqual(final WithArrays expected, final WithArrays actual) {
        assertArrayEquals(expected.booleanArray, actual.booleanArray);
        assertArrayEquals(expected.byteArray, actual.byteArray);
        assertArrayEquals(expected.charArray, actual.charArray);
        assertArrayEquals(expected.shortArray, actual.shortArray);
        assertArrayEquals(expected.intArray, actual.intArray);
        assertArrayEquals(expected.longArray, actual.longArray);
        assertArrayEquals(expected.floatArray, actual.floatArray);
        assertArrayEquals(expected.doubleArray, actual.doubleArray);
    }

    @Serialized
    public static final class Flat {
        public boolean booleanValue;
        public byte byteValue;
        public char charValue;
        public short shortValue;
        public int intValue;
        public long longValue;
        public float floatValue;
        public double doubleValue;
        public String stringValue;
        public UUID uuidValue;
    }

    @Serialized
    public static final class WithArrays {
        public boolean[] booleanArray;
        public byte[] byteArray;
        public char[] charArray;
        public short[] shortArray;
        public int[] intArray;
        public long[] longArray;
        public float[] floatArray;
        public double[] doubleArray;
    }

    public enum TestEnum {
        ONE, TWO, THREE
    }

    @Serialized
    public static final class WithEnum {
        public TestEnum enumValue;
        public int intValue;
    }

    @Serialized
    public static final class Inner {
        public int value;
    }

    @Serialized
    public static final class Outer {
        public Inner inner;
        public String stringValue;
    }

    @Serialized
    public static class Base {
        public int baseValue;
    }

    @Serialized
    public static final class Derived extends Base {
        public int derivedValue;
    }

    @Serialized
    public static final class WithFinalArray {
        public final int[] values = new int[3];
    }

    @Serialized
    public static final class WithObjectArray {
        public String[] values;
    }
}
