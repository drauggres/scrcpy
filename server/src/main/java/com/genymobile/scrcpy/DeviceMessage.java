package com.genymobile.scrcpy;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class DeviceMessage {
    private static final int MESSAGE_MAX_SIZE = 1 << 18; // 256k
    public static final int MAX_EVENT_SIZE = 4096;
    public static final int TYPE_CLIPBOARD = 0;
    public static final int TYPE_ACK_CLIPBOARD = 1;
    public static final int TYPE_PUSH_RESPONSE = 101;

    public static final long SEQUENCE_INVALID = ControlMessage.SEQUENCE_INVALID;

    private int type;

    private DeviceMessage(int type) {
        this.type = type;
    }

    private static final class ClipboardMessage extends DeviceMessage {
        public static final int CLIPBOARD_TEXT_MAX_LENGTH = MESSAGE_MAX_SIZE - 5; // type: 1 byte; length: 4 bytes
        private final byte[] raw;
        private final int len;
        private ClipboardMessage(String text) {
            super(TYPE_CLIPBOARD);
            this.raw = text.getBytes(StandardCharsets.UTF_8);
            this.len = StringUtils.getUtf8TruncationIndex(raw, CLIPBOARD_TEXT_MAX_LENGTH);
        }
        public void writeToByteArray(byte[] array, int offset) {
            ByteBuffer buffer = ByteBuffer.wrap(array, offset, array.length - offset);
            buffer.put((byte) this.getType());
            buffer.putInt(len);
            buffer.put(raw, 0, len);
        }
        public int getLen() {
            return 5 + len;
        }
    }

    private static final class ClipboardAckMessage extends DeviceMessage {
        private final long sequence;
        private ClipboardAckMessage(long sequence) {
            super(TYPE_ACK_CLIPBOARD);
            this.sequence = sequence;
        }
        public void writeToByteArray(byte[] array, int offset) {
            ByteBuffer buffer = ByteBuffer.wrap(array, offset, array.length - offset);
            buffer.put((byte) this.getType());
            buffer.putLong(sequence);
        }
        public int getLen() {
            return 9;
        }
    }

    private static final class FilePushResponseMessage extends DeviceMessage {
        private final short id;
        private final int result;

        private FilePushResponseMessage(short id, int result) {
            super(TYPE_PUSH_RESPONSE);
            this.id = id;
            this.result = result;
        }

        @Override
        public void writeToByteArray(byte[] array, int offset) {
            ByteBuffer buffer = ByteBuffer.wrap(array, offset, array.length - offset);
            buffer.put((byte) this.getType());
            buffer.putShort(id);
            buffer.put((byte) result);
        }

        @Override
        public int getLen() {
            return 4;
        }
    }

    public static DeviceMessage createClipboard(String text) {
        return new ClipboardMessage(text);
    }

    public static DeviceMessage createPushResponse(short id, int result) {
        return new FilePushResponseMessage(id, result);
    }

    public static DeviceMessage createAckClipboard(long sequence) {
        return new ClipboardAckMessage(sequence);
    }

    public int getType() {
        return type;
    }

    public void writeToByteArray(byte[] array) {
        writeToByteArray(array, 0);
    }

    public byte[] writeToByteArray(int offset) {
        byte[] temp = new byte[offset + this.getLen()];
        writeToByteArray(temp, offset);
        return temp;
    }

    public abstract void writeToByteArray(byte[] array, int offset);
    public abstract int getLen();
}
