package com.xuxiaocheng.WList.Utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

@SuppressWarnings("NumericCastThatLosesPrecision")
public final class ByteBufIOUtil {
    private ByteBufIOUtil() {
        super();
    }

    public static final byte[] EmptyByteArray = new byte[0];

    public static byte readByte(final @NotNull ByteBuf buffer) throws IOException {
        try {
            return buffer.readByte();
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static byte @NotNull [] readByteArray(final @NotNull ByteBuf buffer) throws IOException {
        final int length = ByteBufIOUtil.readVariableLenInt(buffer);
        if (length <= 0)
            return ByteBufIOUtil.EmptyByteArray;
        if (buffer.readableBytes() < length)
            throw new IOException(new IndexOutOfBoundsException(length));
        final byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return bytes;
    }

    public static boolean readBoolean(final @NotNull ByteBuf buffer) throws IOException {
        try {
            return buffer.readBoolean();
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static short readShort(final @NotNull ByteBuf buffer) throws IOException {
        try {
            return buffer.readShort();
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static short readVariableLenShort(final @NotNull ByteBuf buffer) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            final byte current = ByteBufIOUtil.readByte(buffer);
            value |= (current & 0x7F) << position;
            if ((current & 0x80) == 0)
                break;
            position += 7;
            if (position >= 16)
                throw new IOException("Short in stream is too big.");
        }
        return (short) value;
    }

    public static int readMedium(final @NotNull ByteBuf buffer) throws IOException {
        try {
            return buffer.readMedium();
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static int readVariableLenMedium(final @NotNull ByteBuf buffer) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            final byte current = ByteBufIOUtil.readByte(buffer);
            value |= (current & 0x7F) << position;
            if ((current & 0x80) == 0)
                break;
            position += 7;
            if (position >= 24)
                throw new IOException("Medium in stream is too big.");
        }
        return value;
    }

    public static int readVariable2LenMedium(final @NotNull ByteBuf buffer) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            final short current = ByteBufIOUtil.readShort(buffer);
            value |= (current & 0x7FFF) << position;
            if ((current & 0x8000) == 0)
                break;
            position += 15;
            if (position >= 24)
                throw new IOException("Medium in stream is too big.");
        }
        return value;
    }

    public static int readInt(final @NotNull ByteBuf buffer) throws IOException {
        try {
            return buffer.readInt();
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static int readVariableLenInt(final @NotNull ByteBuf buffer) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            final byte current = ByteBufIOUtil.readByte(buffer);
            value |= (current & 0x7F) << position;
            if ((current & 0x80) == 0)
                break;
            position += 7;
            if (position >= 32)
                throw new IOException("Int in stream is too big.");
        }
        return value;
    }

    public static int readVariable2LenInt(final @NotNull ByteBuf buffer) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            final short current = ByteBufIOUtil.readShort(buffer);
            value |= (current & 0x7FFF) << position;
            if ((current & 0x8000) == 0)
                break;
            position += 15;
            if (position >= 32)
                throw new IOException("Int in stream is too big.");
        }
        return value;
    }

    public static int readVariable3LenInt(final @NotNull ByteBuf buffer) throws IOException {
        int value = 0;
        int position = 0;
        while (true) {
            final int current = ByteBufIOUtil.readMedium(buffer);
            value |= (current & 0x7FFFFF) << position;
            if ((current & 0x800000) == 0)
                break;
            position += 23;
            if (position >= 32)
                throw new IOException("Int in stream is too big.");
        }
        return value;
    }

    public static long readLong(final @NotNull ByteBuf buffer) throws IOException {
        try {
            return buffer.readLong();
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static long readVariableLenLong(final @NotNull ByteBuf buffer) throws IOException {
        long value = 0;
        int position = 0;
        while (true) {
            final byte current = ByteBufIOUtil.readByte(buffer);
            value |= (long) (current & 0x7F) << position;
            if ((current & 0x80) == 0)
                break;
            position += 7;
            if (position >= 64)
                throw new IOException("Long in stream is too big.");
        }
        return value;
    }

    public static long readVariable2LenLong(final @NotNull ByteBuf buffer) throws IOException {
        long value = 0;
        int position = 0;
        while (true) {
            final short current = ByteBufIOUtil.readShort(buffer);
            value |= (current & 0x7FFFL) << position;
            if ((current & 0x8000) == 0)
                break;
            position += 15;
            if (position >= 64)
                throw new IOException("Long in stream is too big.");
        }
        return value;
    }

    public static long readVariable3LenLong(final @NotNull ByteBuf buffer) throws IOException {
        long value = 0;
        int position = 0;
        while (true) {
            final int current = ByteBufIOUtil.readMedium(buffer);
            value |= (current & 0x7FFFFFL) << position;
            if ((current & 0x800000) == 0)
                break;
            position += 23;
            if (position >= 64)
                throw new IOException("Long in stream is too big.");
        }
        return value;
    }

    public static long readVariable4LenLong(final @NotNull ByteBuf buffer) throws IOException {
        long value = 0;
        int position = 0;
        while (true) {
            final int current = ByteBufIOUtil.readInt(buffer);
            value |= (current & 0x7FFFFFFFL) << position;
            if ((current & 0x80000000) == 0)
                break;
            position += 31;
            if (position >= 64)
                throw new IOException("Long in stream is too big.");
        }
        return value;
    }

    public static float readFloat(final @NotNull ByteBuf buffer) throws IOException {
        return Float.intBitsToFloat(ByteBufIOUtil.readInt(buffer));
    }

    public static float readVariableLenFloat(final @NotNull ByteBuf buffer) throws IOException {
        return Float.intBitsToFloat(ByteBufIOUtil.readVariableLenInt(buffer));
    }

    public static float readVariable2LenFloat(final @NotNull ByteBuf buffer) throws IOException {
        return Float.intBitsToFloat(ByteBufIOUtil.readVariable2LenInt(buffer));
    }

    public static float readVariable3LenFloat(final @NotNull ByteBuf buffer) throws IOException {
        return Float.intBitsToFloat(ByteBufIOUtil.readVariable3LenInt(buffer));
    }

    public static double readDouble(final @NotNull ByteBuf buffer) throws IOException {
        return Double.longBitsToDouble(ByteBufIOUtil.readLong(buffer));
    }

    public static double readVariableLenDouble(final @NotNull ByteBuf buffer) throws IOException {
        return Double.longBitsToDouble(ByteBufIOUtil.readVariableLenLong(buffer));
    }

    public static double readVariable2LenDouble(final @NotNull ByteBuf buffer) throws IOException {
        return Double.longBitsToDouble(ByteBufIOUtil.readVariable2LenLong(buffer));
    }

    public static double readVariable3LenDouble(final @NotNull ByteBuf buffer) throws IOException {
        return Double.longBitsToDouble(ByteBufIOUtil.readVariable3LenLong(buffer));
    }

    public static double readVariable4LenDouble(final @NotNull ByteBuf buffer) throws IOException {
        return Double.longBitsToDouble(ByteBufIOUtil.readVariable4LenLong(buffer));
    }

    public static @NotNull String readUTF(final @NotNull ByteBuf buffer) throws IOException {
        return new String(ByteBufIOUtil.readByteArray(buffer), StandardCharsets.UTF_8);
    }

    public static @NotNull UUID readUUID(final @NotNull ByteBuf buffer) throws IOException {
        return new UUID(ByteBufIOUtil.readVariable3LenLong(buffer), ByteBufIOUtil.readVariable3LenLong(buffer));
    }

    @SuppressWarnings("unchecked")
    public static @Nullable <T extends Serializable> T readSerializable(final @NotNull ByteBuf buffer) throws IOException {
        try (final ObjectInputStream objectInputStream = new ObjectInputStream(new ByteBufInputStream(buffer))) {
            return (T) objectInputStream.readObject();
        } catch (final ClassNotFoundException | ClassCastException | IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static <T> @Nullable T readObjectNullable(final @NotNull ByteBuf buffer, final @NotNull Deserializer<@NotNull T> deserializer) throws IOException {
        if (ByteBufIOUtil.readBoolean(buffer))
            return null;
        return deserializer.deserialize(buffer);
    }

    public static void writeByte(final @NotNull ByteBuf buffer, final byte b) throws IOException {
        try {
            buffer.writeByte(b);
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static void writeByteArray(final @NotNull ByteBuf buffer, final byte @NotNull [] b) throws IOException {
        try {
            ByteBufIOUtil.writeVariableLenInt(buffer, b.length);
            buffer.writeBytes(b);
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static void writeBoolean(final @NotNull ByteBuf buffer, final boolean f) throws IOException {
        try {
            buffer.writeBoolean(f);
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static void writeShort(final @NotNull ByteBuf buffer, final short s) throws IOException {
        try {
            buffer.writeShort(s);
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static void writeVariableLenShort(final @NotNull ByteBuf buffer, final short s) throws IOException {
        short value = s;
        while ((value & 0xFF80) != 0) {
            ByteBufIOUtil.writeByte(buffer, (byte) ((value & 0x7F) | 0x80));
            value = (short) ((value & 0xFFFF) >>> 7);
        }
        ByteBufIOUtil.writeByte(buffer, (byte) value);
    }

    public static void writeMedium(final @NotNull ByteBuf buffer, final int i) throws IOException {
        try {
            buffer.writeMedium(i);
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static void writeVariableLenMedium(final @NotNull ByteBuf buffer, final int i) throws IOException {
        int value = i & 0xFFFFFF;
        while ((value & 0xFFFF80) != 0) {
            ByteBufIOUtil.writeByte(buffer, (byte) ((value & 0x7F) | 0x80));
            value = (value & 0xFFFFFF) >>> 7;
        }
        ByteBufIOUtil.writeByte(buffer, (byte) value);
    }

    public static void writeVariable2LenMedium(final @NotNull ByteBuf buffer, final int i) throws IOException {
        int value = i;
        while ((value & 0xFFFF8000) != 0) {
            ByteBufIOUtil.writeShort(buffer, (short) ((value & 0x7FFF) | 0x8000));
            value >>>= 15;
        }
        ByteBufIOUtil.writeShort(buffer, (short) value);
    }

    public static void writeInt(final @NotNull ByteBuf buffer, final int i) throws IOException {
        try {
            buffer.writeInt(i);
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static void writeVariableLenInt(final @NotNull ByteBuf buffer, final int i) throws IOException {
        int value = i;
        while ((value & 0xFFFFFF80) != 0) {
            ByteBufIOUtil.writeByte(buffer, (byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        ByteBufIOUtil.writeByte(buffer, (byte) value);
    }

    public static void writeVariable2LenInt(final @NotNull ByteBuf buffer, final int i) throws IOException {
        int value = i;
        while ((value & 0xFFFF8000) != 0) {
            ByteBufIOUtil.writeShort(buffer, (short) ((value & 0x7FFF) | 0x8000));
            value >>>= 15;
        }
        ByteBufIOUtil.writeShort(buffer, (short) value);
    }

    public static void writeVariable3LenInt(final @NotNull ByteBuf buffer, final int i) throws IOException {
        int value = i;
        while ((value & 0xFF800000) != 0) {
            ByteBufIOUtil.writeMedium(buffer, ((value & 0x7FFFFF) | 0x800000));
            value >>>= 23;
        }
        ByteBufIOUtil.writeMedium(buffer, value);
    }

    public static void writeLong(final @NotNull ByteBuf buffer, final long l) throws IOException {
        try {
            buffer.writeLong(l);
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static void writeVariableLenLong(final @NotNull ByteBuf buffer, final long l) throws IOException {
        long value = l;
        while ((value & 0xFFFFFFFFFFFFFF80L) != 0) {
            ByteBufIOUtil.writeByte(buffer, (byte) ((value & 0x7F) | 0x80));
            value >>>= 7;
        }
        ByteBufIOUtil.writeByte(buffer, (byte) value);
    }

    public static void writeVariable2LenLong(final @NotNull ByteBuf buffer, final long l) throws IOException {
        long value = l;
        while ((value & 0xFFFFFFFFFFFF8000L) != 0) {
            ByteBufIOUtil.writeShort(buffer, (short) ((value & 0x7FFF) | 0x8000));
            value >>>= 15;
        }
        ByteBufIOUtil.writeShort(buffer, (short) value);
    }

    public static void writeVariable3LenLong(final @NotNull ByteBuf buffer, final long l) throws IOException {
        long value = l;
        while ((value & 0xFFFFFFFFFF800000L) != 0) {
            ByteBufIOUtil.writeMedium(buffer, (int) ((value & 0x7FFFFF) | 0x800000));
            value >>>= 23;
        }
        ByteBufIOUtil.writeMedium(buffer, (int) value);
    }

    public static void writeVariable4LenLong(final @NotNull ByteBuf buffer, final long l) throws IOException {
        long value = l;
        while ((value & 0xFFFFFFFF80000000L) != 0) {
            ByteBufIOUtil.writeInt(buffer, (int) ((value & 0x7FFFFFFF) | 0x80000000));
            value >>>= 23;
        }
        ByteBufIOUtil.writeInt(buffer, (int) value);
    }

    public static void writeFloat(final @NotNull ByteBuf buffer, final float f) throws IOException {
        ByteBufIOUtil.writeInt(buffer, Float.floatToIntBits(f));
    }

    public static void writeVariableLenFloat(final @NotNull ByteBuf buffer, final float f) throws IOException {
        ByteBufIOUtil.writeVariableLenInt(buffer, Float.floatToIntBits(f));
    }

    public static void writeVariable2LenFloat(final @NotNull ByteBuf buffer, final float f) throws IOException {
        ByteBufIOUtil.writeVariable2LenInt(buffer, Float.floatToIntBits(f));
    }

    public static void writeVariable3LenFloat(final @NotNull ByteBuf buffer, final float f) throws IOException {
        ByteBufIOUtil.writeVariable3LenInt(buffer, Float.floatToIntBits(f));
    }

    public static void writeDouble(final @NotNull ByteBuf buffer, final double d) throws IOException {
        ByteBufIOUtil.writeLong(buffer, Double.doubleToLongBits(d));
    }

    public static void writeVariableLenDouble(final @NotNull ByteBuf buffer, final double d) throws IOException {
        ByteBufIOUtil.writeVariableLenLong(buffer, Double.doubleToLongBits(d));
    }

    public static void writeVariable2LenDouble(final @NotNull ByteBuf buffer, final double d) throws IOException {
        ByteBufIOUtil.writeVariable2LenLong(buffer, Double.doubleToLongBits(d));
    }

    public static void writeVariable3LenDouble(final @NotNull ByteBuf buffer, final double d) throws IOException {
        ByteBufIOUtil.writeVariable3LenLong(buffer, Double.doubleToLongBits(d));
    }

    public static void writeVariable4LenDouble(final @NotNull ByteBuf buffer, final double d) throws IOException {
        ByteBufIOUtil.writeVariable4LenLong(buffer, Double.doubleToLongBits(d));
    }

    public static void writeUTF(final @NotNull ByteBuf buffer, final @NotNull String s) throws IOException {
        ByteBufIOUtil.writeByteArray(buffer, s.getBytes(StandardCharsets.UTF_8));
    }

    public static void writeUUID(final @NotNull ByteBuf buffer, final @NotNull UUID id) throws IOException {
        ByteBufIOUtil.writeVariable3LenLong(buffer, id.getMostSignificantBits());
        ByteBufIOUtil.writeVariable3LenLong(buffer, id.getLeastSignificantBits());
    }

    public static void writeSerializable(final @NotNull ByteBuf buffer, final @Nullable Serializable serializable) throws IOException {
        try (final ObjectOutput objectOutputStream = new ObjectOutputStream(new ByteBufOutputStream(buffer))) {
            objectOutputStream.writeObject(serializable);
        } catch (final IndexOutOfBoundsException exception) {
            throw new IOException(exception);
        }
    }

    public static <T> void writeObjectNullable(final @NotNull ByteBuf buffer, final @Nullable T object, final @NotNull Serializer<@NotNull T> serializer) throws IOException {
        ByteBufIOUtil.writeBoolean(buffer, object == null);
        if (object != null)
            serializer.serialize(buffer, object);
    }

    public static byte @NotNull [] allToByteArray(final @NotNull ByteBuf buffer) {
        if (buffer.hasArray()) {
            final int offset = buffer.arrayOffset();
            return Arrays.copyOfRange(buffer.array(), offset, offset + buffer.readableBytes());
        }
        final int index = buffer.readerIndex();
        final byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes(bytes);
        buffer.readerIndex(index);
        return bytes;
    }

    @FunctionalInterface
    public interface Deserializer<T> {
        T deserialize(final @NotNull ByteBuf buffer) throws IOException;
    }

    @FunctionalInterface
    public interface Serializer<T> {
        void serialize(final @NotNull ByteBuf buffer, final T object) throws IOException;
    }
}
