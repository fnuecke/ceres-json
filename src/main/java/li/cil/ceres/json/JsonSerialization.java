package li.cil.ceres.json;

import com.google.gson.*;
import li.cil.ceres.Ceres;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;

import javax.annotation.Nullable;
import java.lang.reflect.Array;

public final class JsonSerialization {
    private static final String NAN = "nan";
    private static final String POSITIVE_INFINITY = "inf";
    private static final String NEGATIVE_INFINITY = "-inf";

    public static <T> JsonObject serialize(final T value, final Class<T> type) throws SerializationException {
        final Serializer visitor = new Serializer();
        Ceres.getSerializer(type).serialize(visitor, type, value);
        return visitor.data;
    }

    public static <T> T deserialize(final JsonObject data, final Class<T> type, @Nullable final T into) throws SerializationException {
        try {
            return Ceres.getSerializer(type).deserialize(new Deserializer(data), type, into);
        } catch (final SerializationException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new SerializationException(e);
        }
    }

    private static JsonPrimitive floatToJson(final float value) {
        return Float.isFinite(value) ? new JsonPrimitive(value) : nonFiniteToJson(value);
    }

    private static JsonPrimitive doubleToJson(final double value) {
        return Double.isFinite(value) ? new JsonPrimitive(value) : nonFiniteToJson(value);
    }

    private static JsonPrimitive nonFiniteToJson(final double value) {
        if (Double.isNaN(value)) {
            return new JsonPrimitive(NAN);
        }
        return new JsonPrimitive(value > 0 ? POSITIVE_INFINITY : NEGATIVE_INFINITY);
    }

    private static float jsonToFloat(final JsonElement element) throws SerializationException {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return (float) nonFiniteFromJson(element.getAsString());
        }
        return element.getAsFloat();
    }

    private static double jsonToDouble(final JsonElement element) throws SerializationException {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            return nonFiniteFromJson(element.getAsString());
        }
        return element.getAsDouble();
    }

    private static double nonFiniteFromJson(final String value) throws SerializationException {
        return switch (value) {
            case NAN -> Double.NaN;
            case POSITIVE_INFINITY -> Double.POSITIVE_INFINITY;
            case NEGATIVE_INFINITY -> Double.NEGATIVE_INFINITY;
            default ->
                throw new SerializationException(String.format("Invalid non-finite floating-point value [%s].", value));
        };
    }

    private static void checkComponentType(final Class<?> componentType) throws SerializationException {
        if (Enum.class.isAssignableFrom(componentType) && !componentType.isEnum()) {
            throw new SerializationException(String.format("Cannot serialize arrays with abstract enum component type [%s]. Use a concrete enum type.", componentType.getName()));
        }
    }

    private static Object enumConstantFromJson(final Class<?> type, final JsonElement element) throws SerializationException {
        final int ordinal = element.getAsInt();
        if (element.getAsDouble() != ordinal) {
            throw new SerializationException(String.format("Invalid enum ordinal [%s] for type [%s].", element.getAsString(), type.getName()));
        }
        final Object[] enumConstants = type.getEnumConstants();
        if (ordinal < 0 || ordinal >= enumConstants.length) {
            throw new SerializationException(String.format("Enum ordinal [%d] is out of range for type [%s], which has [%d] constants.", ordinal, type.getName(), enumConstants.length));
        }
        return enumConstants[ordinal];
    }

    private static final class Serializer implements SerializationVisitor {
        private final JsonObject data = new JsonObject();

        @Override
        public void putBoolean(final String name, final boolean value) {
            data.addProperty(name, value);
        }

        @Override
        public void putByte(final String name, final byte value) {
            data.addProperty(name, value);
        }

        @Override
        public void putChar(final String name, final char value) {
            data.addProperty(name, (int) value);
        }

        @Override
        public void putShort(final String name, final short value) {
            data.addProperty(name, value);
        }

        @Override
        public void putInt(final String name, final int value) {
            data.addProperty(name, value);
        }

        @Override
        public void putLong(final String name, final long value) {
            data.addProperty(name, value);
        }

        @Override
        public void putFloat(final String name, final float value) {
            data.add(name, floatToJson(value));
        }

        @Override
        public void putDouble(final String name, final double value) {
            data.add(name, doubleToJson(value));
        }

        @Override
        public void putObject(final String name, final Class<?> type, @Nullable final Object value) throws SerializationException {
            if (value == null) {
                data.add(name, JsonNull.INSTANCE);
            } else {
                data.add(name, toJson(name, type, value));
            }
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static JsonElement toJson(final String name, final Class<?> type, final Object value) throws SerializationException {
            if (type.isArray()) {
                return arrayToJson(name, type.getComponentType(), value);
            } else if (type.isEnum()) {
                return new JsonPrimitive(((Enum<?>) value).ordinal());
            } else if (type == String.class) {
                return new JsonPrimitive((String) value);
            } else {
                final Serializer nested = new Serializer();
                Ceres.getSerializer(type).serialize(nested, (Class) type, value);
                return nested.data;
            }
        }

        private static JsonArray arrayToJson(final String name, final Class<?> componentType, final Object value) throws SerializationException {
            final int length = Array.getLength(value);
            final JsonArray array = new JsonArray(length);
            if (componentType == boolean.class) {
                for (int i = 0; i < length; i++) array.add(Array.getBoolean(value, i));
            } else if (componentType == char.class) {
                for (int i = 0; i < length; i++) array.add((int) Array.getChar(value, i));
            } else if (componentType == float.class) {
                for (int i = 0; i < length; i++) array.add(floatToJson(Array.getFloat(value, i)));
            } else if (componentType == double.class) {
                for (int i = 0; i < length; i++) array.add(doubleToJson(Array.getDouble(value, i)));
            } else if (componentType.isPrimitive()) {
                for (int i = 0; i < length; i++) array.add((Number) Array.get(value, i));
            } else {
                checkComponentType(componentType);
                for (int i = 0; i < length; i++) {
                    final Object element = Array.get(value, i);
                    if (element == null) {
                        array.add(JsonNull.INSTANCE);
                        continue;
                    }
                    if (!componentType.isEnum() && element.getClass() != componentType) {
                        throw new SerializationException(String.format("Polymorphism detected in array [%s]. This is not supported.", name));
                    }
                    array.add(toJson(name, componentType, element));
                }
            }
            return array;
        }
    }

    private record Deserializer(JsonObject data) implements DeserializationVisitor {
        @Override
        public boolean exists(final String name) {
            return data.has(name);
        }

        @Override
        public boolean getBoolean(final String name) {
            return data.get(name).getAsBoolean();
        }

        @Override
        public byte getByte(final String name) {
            return data.get(name).getAsByte();
        }

        @Override
        public char getChar(final String name) {
            return (char) data.get(name).getAsInt();
        }

        @Override
        public short getShort(final String name) {
            return data.get(name).getAsShort();
        }

        @Override
        public int getInt(final String name) {
            return data.get(name).getAsInt();
        }

        @Override
        public long getLong(final String name) {
            return data.get(name).getAsLong();
        }

        @Override
        public float getFloat(final String name) {
            return jsonToFloat(data.get(name));
        }

        @Override
        public double getDouble(final String name) {
            return jsonToDouble(data.get(name));
        }

        @Nullable
        @Override
        public Object getObject(final String name, final Class<?> type, @Nullable final Object into) throws SerializationException {
            final JsonElement element = data.get(name);
            if (element == null) {
                throw new SerializationException(String.format("Missing property [%s].", name));
            }
            if (element.isJsonNull()) {
                return null;
            }
            return fromJson(element, type, into);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Object fromJson(final JsonElement element, final Class<?> type, @Nullable final Object into) throws SerializationException {
            if (type.isArray()) {
                return jsonToArray(element.getAsJsonArray(), type.getComponentType(), into);
            } else if (type.isEnum()) {
                return enumConstantFromJson(type, element);
            } else if (type == String.class) {
                return element.getAsString();
            } else {
                return Ceres.getSerializer(type).deserialize(new Deserializer(element.getAsJsonObject()), (Class) type, into);
            }
        }

        private static Object jsonToArray(final JsonArray array, final Class<?> componentType, @Nullable final Object into) throws SerializationException {
            if (!componentType.isPrimitive()) {
                checkComponentType(componentType);
            }
            final Object result = into != null && Array.getLength(into) == array.size() ? into : Array.newInstance(componentType, array.size());
            for (int i = 0; i < array.size(); i++) {
                final JsonElement element = array.get(i);
                if (componentType == boolean.class) {
                    Array.setBoolean(result, i, element.getAsBoolean());
                } else if (componentType == byte.class) {
                    Array.setByte(result, i, element.getAsByte());
                } else if (componentType == char.class) {
                    Array.setChar(result, i, (char) element.getAsInt());
                } else if (componentType == short.class) {
                    Array.setShort(result, i, element.getAsShort());
                } else if (componentType == int.class) {
                    Array.setInt(result, i, element.getAsInt());
                } else if (componentType == long.class) {
                    Array.setLong(result, i, element.getAsLong());
                } else if (componentType == float.class) {
                    Array.setFloat(result, i, jsonToFloat(element));
                } else if (componentType == double.class) {
                    Array.setDouble(result, i, jsonToDouble(element));
                } else if (element.isJsonNull()) {
                    Array.set(result, i, null);
                } else {
                    Array.set(result, i, fromJson(element, componentType, Array.get(result, i)));
                }
            }
            return result;
        }
    }
}
