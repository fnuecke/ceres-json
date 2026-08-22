package li.cil.ceres.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import li.cil.ceres.Ceres;
import li.cil.ceres.api.DeserializationVisitor;
import li.cil.ceres.api.SerializationException;
import li.cil.ceres.api.SerializationVisitor;

import javax.annotation.Nullable;
import java.lang.reflect.Array;

public final class JsonSerialization {
    public static <T> JsonObject serialize(final T value, final Class<T> type) throws SerializationException {
        final Serializer visitor = new Serializer();
        Ceres.getSerializer(type).serialize(visitor, type, value);
        return visitor.data;
    }

    public static <T> T deserialize(final JsonObject data, final Class<T> type, @Nullable final T into) throws SerializationException {
        return Ceres.getSerializer(type).deserialize(new Deserializer(data), type, into);
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
            data.addProperty(name, value);
        }

        @Override
        public void putDouble(final String name, final double value) {
            data.addProperty(name, value);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Override
        public void putObject(final String name, final Class<?> type, @Nullable final Object value) throws SerializationException {
            if (value == null) {
                data.add(name, JsonNull.INSTANCE);
            } else if (type.isArray()) {
                data.add(name, arrayToJson(type.getComponentType(), value));
            } else if (type.isEnum()) {
                data.addProperty(name, ((Enum<?>) value).ordinal());
            } else if (type == String.class) {
                data.addProperty(name, (String) value);
            } else {
                final Serializer nested = new Serializer();
                Ceres.getSerializer(type).serialize(nested, (Class) type, value);
                data.add(name, nested.data);
            }
        }

        private static JsonArray arrayToJson(final Class<?> componentType, final Object value) throws SerializationException {
            final int length = Array.getLength(value);
            final JsonArray array = new JsonArray(length);
            if (componentType == boolean.class) {
                for (int i = 0; i < length; i++) array.add(Array.getBoolean(value, i));
            } else if (componentType == char.class) {
                for (int i = 0; i < length; i++) array.add((int) Array.getChar(value, i));
            } else if (componentType.isPrimitive()) {
                for (int i = 0; i < length; i++) array.add((Number) Array.get(value, i));
            } else {
                throw new SerializationException("Object arrays are not supported.");
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
            return data.get(name).getAsFloat();
        }

        @Override
        public double getDouble(final String name) {
            return data.get(name).getAsDouble();
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        @Nullable
        @Override
        public Object getObject(final String name, final Class<?> type, @Nullable final Object into) throws SerializationException {
            final JsonElement element = data.get(name);
            if (element == null || element.isJsonNull()) {
                return null;
            }
            if (type.isArray()) {
                return jsonToArray(element.getAsJsonArray(), type.getComponentType(), into);
            } else if (type.isEnum()) {
                return type.getEnumConstants()[element.getAsInt()];
            } else if (type == String.class) {
                return element.getAsString();
            } else {
                return Ceres.getSerializer(type).deserialize(new Deserializer(element.getAsJsonObject()), (Class) type, into);
            }
        }

        private static Object jsonToArray(final JsonArray array, final Class<?> componentType, @Nullable final Object into) throws SerializationException {
            final Object result = into != null && Array.getLength(into) == array.size()
                    ? into : Array.newInstance(componentType, array.size());
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
                    Array.setFloat(result, i, element.getAsFloat());
                } else if (componentType == double.class) {
                    Array.setDouble(result, i, element.getAsDouble());
                } else {
                    throw new SerializationException("Object arrays are not supported.");
                }
            }
            return result;
        }
    }
}
