package li.cil.ceres.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
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
    public void nonFiniteFloatsAreEncodedAsStrings() {
        final Flat value = new Flat();
        value.floatValue = Float.NaN;
        value.doubleValue = Double.NEGATIVE_INFINITY;

        final JsonObject serialized = JsonSerialization.serialize(value, Flat.class);

        assertEquals("nan", serialized.get("floatValue").getAsString());
        assertEquals("-inf", serialized.get("doubleValue").getAsString());

        final Flat deserialized = JsonSerialization.deserialize(reparse(serialized), Flat.class, null);

        assertTrue(Float.isNaN(deserialized.floatValue));
        assertEquals(Double.NEGATIVE_INFINITY, deserialized.doubleValue);
    }

    @Test
    public void nonFiniteFloatsInArraysAreEncodedAsStrings() {
        final WithArrays value = new WithArrays();
        value.floatArray = new float[]{Float.POSITIVE_INFINITY, 1.5f};
        value.doubleArray = new double[]{Double.NaN, -2.5};

        final JsonObject serialized = JsonSerialization.serialize(value, WithArrays.class);

        assertEquals("inf", serialized.get("floatArray").getAsJsonArray().get(0).getAsString());
        assertEquals("nan", serialized.get("doubleArray").getAsJsonArray().get(0).getAsString());

        final WithArrays deserialized = JsonSerialization.deserialize(reparse(serialized), WithArrays.class, null);

        assertEquals(Float.POSITIVE_INFINITY, deserialized.floatArray[0]);
        assertEquals(1.5f, deserialized.floatArray[1]);
        assertTrue(Double.isNaN(deserialized.doubleArray[0]));
        assertEquals(-2.5, deserialized.doubleArray[1]);
    }

    @Test
    public void invalidNonFiniteStringsAreRejected() {
        final Flat value = new Flat();

        final JsonObject serialized = JsonSerialization.serialize(value, Flat.class);
        serialized.addProperty("floatValue", "bogus");

        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(serialized, Flat.class, null));
    }

    @Test
    public void negativeZeroSurvivesTextRoundTrip() {
        final Flat value = new Flat();
        value.floatValue = -0.0f;
        value.doubleValue = -0.0;

        final Flat deserialized = JsonSerialization.deserialize(reparse(JsonSerialization.serialize(value, Flat.class)), Flat.class, null);

        assertEquals(Float.floatToRawIntBits(-0.0f), Float.floatToRawIntBits(deserialized.floatValue));
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(deserialized.doubleValue));
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
    public void stringArraysAreSerializedCorrectly() {
        final WithStringArray value = new WithStringArray();
        value.values = new String[]{"a", null, "c"};

        final JsonObject serialized = JsonSerialization.serialize(value, WithStringArray.class);
        final WithStringArray deserialized = JsonSerialization.deserialize(reparse(serialized), WithStringArray.class, null);

        assertArrayEquals(value.values, deserialized.values);
    }

    @Test
    public void objectArraysAreSerializedCorrectly() {
        final WithItemArray value = new WithItemArray();
        value.values = new Item[]{new Item(), null, new Item()};
        value.values[0].v = 123;
        value.values[2].v = 234;

        final JsonObject serialized = JsonSerialization.serialize(value, WithItemArray.class);
        final WithItemArray deserialized = JsonSerialization.deserialize(reparse(serialized), WithItemArray.class, null);

        assertEquals(123, deserialized.values[0].v);
        assertNull(deserialized.values[1]);
        assertEquals(234, deserialized.values[2].v);
    }

    @Test
    public void deserializingObjectArraysReusesExistingElements() {
        final WithItemArray value = new WithItemArray();
        value.values = new Item[]{new Item(), new Item()};
        value.values[0].v = 123;
        value.values[1].v = 234;

        final JsonObject serialized = JsonSerialization.serialize(value, WithItemArray.class);

        final WithItemArray into = new WithItemArray();
        into.values = new Item[]{new Item(), new Item()};
        final Item existingElement = into.values[0];
        final Item[] existingArray = into.values;

        final WithItemArray deserialized = JsonSerialization.deserialize(serialized, WithItemArray.class, into);

        assertSame(existingArray, deserialized.values);
        assertSame(existingElement, deserialized.values[0]);
        assertEquals(123, deserialized.values[0].v);
        assertEquals(234, deserialized.values[1].v);
    }

    @Test
    public void enumArraysAreSerializedByOrdinal() {
        final WithEnumArray value = new WithEnumArray();
        value.values = new TestEnum[]{TestEnum.THREE, TestEnum.ONE, TestEnum.TWO};

        final JsonObject serialized = JsonSerialization.serialize(value, WithEnumArray.class);

        final JsonArray array = serialized.get("values").getAsJsonArray();
        assertEquals(TestEnum.THREE.ordinal(), array.get(0).getAsInt());
        assertEquals(TestEnum.ONE.ordinal(), array.get(1).getAsInt());
        assertEquals(TestEnum.TWO.ordinal(), array.get(2).getAsInt());

        final WithEnumArray deserialized = JsonSerialization.deserialize(reparse(serialized), WithEnumArray.class, null);

        assertArrayEquals(value.values, deserialized.values);
    }

    @Test
    public void enumConstantsWithBodiesSerializeByComponentType() {
        final WithComplexEnumArray value = new WithComplexEnumArray();
        value.values = new TestComplexEnum[]{TestComplexEnum.TIMES, TestComplexEnum.PLUS};

        final JsonObject serialized = assertDoesNotThrow(() -> JsonSerialization.serialize(value, WithComplexEnumArray.class));
        final WithComplexEnumArray deserialized = JsonSerialization.deserialize(reparse(serialized), WithComplexEnumArray.class, null);

        assertArrayEquals(value.values, deserialized.values);
        assertEquals(6, deserialized.values[0].apply(3));
        assertEquals(4, deserialized.values[1].apply(3));
    }

    @Test
    public void multiDimensionalArraysAreSerializedCorrectly() {
        final WithIntMatrix value = new WithIntMatrix();
        value.values = new int[][]{{1, 2}, {3}};

        final JsonObject serialized = JsonSerialization.serialize(value, WithIntMatrix.class);
        final WithIntMatrix deserialized = JsonSerialization.deserialize(reparse(serialized), WithIntMatrix.class, null);

        assertArrayEquals(value.values, deserialized.values);
    }

    @Test
    public void polymorphicArraysAreRejected() {
        final WithBaseArray value = new WithBaseArray();
        value.values = new Base[]{new Derived()};

        assertThrows(SerializationException.class, () -> JsonSerialization.serialize(value, WithBaseArray.class));
    }

    @Test
    public void abstractEnumArraysAreRejected() {
        final WithAbstractEnumArray value = new WithAbstractEnumArray();
        value.values = new Enum<?>[]{TestEnum.ONE};

        assertThrows(SerializationException.class, () -> JsonSerialization.serialize(value, WithAbstractEnumArray.class));
    }

    @Test
    public void outOfRangeEnumOrdinalsAreRejected() {
        final WithEnum value = new WithEnum();
        value.enumValue = TestEnum.ONE;

        final JsonObject serialized = JsonSerialization.serialize(value, WithEnum.class);
        serialized.addProperty("enumValue", 99);

        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(serialized, WithEnum.class, null));
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
    public void stringValuesMatchingNonFiniteMarkersAreNotMisinterpreted() {
        final Flat value = new Flat();
        value.stringValue = "nan";

        final Flat deserialized = JsonSerialization.deserialize(reparse(JsonSerialization.serialize(value, Flat.class)), Flat.class, null);

        assertEquals("nan", deserialized.stringValue);

        final WithStringArray withArray = new WithStringArray();
        withArray.values = new String[]{"nan", "inf", "-inf"};

        final WithStringArray deserializedArray = JsonSerialization.deserialize(reparse(JsonSerialization.serialize(withArray, WithStringArray.class)), WithStringArray.class, null);

        assertArrayEquals(withArray.values, deserializedArray.values);
    }

    @Test
    public void enumArraysWithNullElementsRoundTrip() {
        final WithEnumArray value = new WithEnumArray();
        value.values = new TestEnum[]{TestEnum.ONE, null, TestEnum.THREE};

        final WithEnumArray deserialized = JsonSerialization.deserialize(reparse(JsonSerialization.serialize(value, WithEnumArray.class)), WithEnumArray.class, null);

        assertArrayEquals(value.values, deserialized.values);
    }

    @Test
    public void abstractEnumArraysAreRejectedOnRead() {
        final JsonObject data = new JsonObject();
        final JsonArray values = new JsonArray();
        values.add(0);
        data.add("values", values);

        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(data, WithAbstractEnumArray.class, null));
    }

    @Test
    public void invalidNonFiniteStringsInArraysAreRejected() {
        final WithArrays value = new WithArrays();
        value.floatArray = new float[]{1f};

        final JsonObject serialized = JsonSerialization.serialize(value, WithArrays.class);
        serialized.get("floatArray").getAsJsonArray().set(0, new JsonPrimitive("bogus"));

        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(serialized, WithArrays.class, null));
    }

    @Test
    public void outOfRangeEnumOrdinalsInArraysAreRejected() {
        final WithEnumArray value = new WithEnumArray();
        value.values = new TestEnum[]{TestEnum.ONE};

        final JsonObject serialized = JsonSerialization.serialize(value, WithEnumArray.class);
        serialized.get("values").getAsJsonArray().set(0, new JsonPrimitive(99));

        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(serialized, WithEnumArray.class, null));
    }

    @Test
    public void nonIntegralEnumOrdinalsAreRejected() {
        final WithEnum value = new WithEnum();
        value.enumValue = TestEnum.ONE;

        final JsonObject serialized = JsonSerialization.serialize(value, WithEnum.class);

        serialized.addProperty("enumValue", 1.9);
        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(serialized, WithEnum.class, null));

        serialized.addProperty("enumValue", 4294967296L);
        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(serialized, WithEnum.class, null));
    }

    @Test
    public void malformedValuesAreRejectedWithSerializationException() {
        final WithEnum value = new WithEnum();
        value.enumValue = TestEnum.ONE;

        final JsonObject bogusInt = JsonSerialization.serialize(value, WithEnum.class);
        bogusInt.addProperty("intValue", "bogus");

        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(bogusInt, WithEnum.class, null));

        final WithEnumArray arrayValue = new WithEnumArray();
        arrayValue.values = new TestEnum[]{TestEnum.ONE};

        final JsonObject objectForArray = JsonSerialization.serialize(arrayValue, WithEnumArray.class);
        objectForArray.add("values", new JsonObject());

        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(objectForArray, WithEnumArray.class, null));
    }

    @Test
    public void nestedArraysAreReusedInPlace() {
        final WithIntMatrix value = new WithIntMatrix();
        value.values = new int[][]{{1, 2}, {3}};

        final JsonObject serialized = JsonSerialization.serialize(value, WithIntMatrix.class);

        final WithIntMatrix into = new WithIntMatrix();
        into.values = new int[][]{{9, 9}, {9}};
        final int[][] outer = into.values;
        final int[] inner = into.values[0];

        final WithIntMatrix deserialized = JsonSerialization.deserialize(serialized, WithIntMatrix.class, into);

        assertSame(outer, deserialized.values);
        assertSame(inner, deserialized.values[0]);
        assertArrayEquals(value.values, deserialized.values);
    }

    @Test
    public void rootLevelArraysAreSupported() {
        final TestEnum[] value = {TestEnum.TWO, TestEnum.THREE};

        final JsonObject serialized = JsonSerialization.serialize(value, TestEnum[].class);
        final TestEnum[] deserialized = JsonSerialization.deserialize(reparse(serialized), TestEnum[].class, null);

        assertArrayEquals(value, deserialized);
    }

    @Test
    public void missingPropertiesForObjectsFailLoudly() {
        assertThrows(SerializationException.class, () -> JsonSerialization.deserialize(new JsonObject(), int[].class, null));
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

    public enum TestComplexEnum {
        PLUS {
            @Override
            public int apply(final int x) {
                return x + 1;
            }
        },
        TIMES {
            @Override
            public int apply(final int x) {
                return x * 2;
            }
        };

        public abstract int apply(int x);
    }

    @Serialized
    public static final class WithEnum {
        public TestEnum enumValue;
        public int intValue;
    }

    @Serialized
    public static final class WithEnumArray {
        public TestEnum[] values;
    }

    @Serialized
    public static final class WithComplexEnumArray {
        public TestComplexEnum[] values;
    }

    @Serialized
    public static final class WithAbstractEnumArray {
        public Enum<?>[] values;
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
    public static final class Item {
        public int v;
    }

    @Serialized
    public static final class WithItemArray {
        public Item[] values;
    }

    @Serialized
    public static final class WithStringArray {
        public String[] values;
    }

    @Serialized
    public static final class WithBaseArray {
        public Base[] values;
    }

    @Serialized
    public static final class WithIntMatrix {
        public int[][] values;
    }

    @Serialized
    public static final class WithFinalArray {
        public final int[] values = new int[3];
    }
}
