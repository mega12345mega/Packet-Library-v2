package com.luneruniverse.packetlibv2.packets;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Holds any primitive data type, null, and Strings for basic communication
 */
public class PrimitivePacket extends Packet {
	
	private Object value;
	private byte[] data;
	
	/**
	 * Only allows primitive data types, null, and Strings
	 * @param value The data to store
	 */
	public PrimitivePacket(Object value) {
		setValue(value);
	}
	public PrimitivePacket(DataInputStream in) throws IOException {
		switch (in.read()) {
			case 0:
				setValue(null);
				break;
			case 1:
				setValue(in.readBoolean());
				break;
			case 2:
				setValue(in.readByte());
				break;
			case 3:
				setValue(in.readShort());
				break;
			case 4:
				setValue(in.readChar());
				break;
			case 5:
				setValue(in.readInt());
				break;
			case 6:
				setValue(in.readLong());
				break;
			case 7:
				setValue(in.readFloat());
				break;
			case 8:
				setValue(in.readDouble());
				break;
			case 9:
				StringBuilder request = new StringBuilder();
				int strLength = in.readInt();
				for (int i = 0; i < strLength; i++)
					request.append(in.readChar());
				setValue(request.toString());
				break;
			default:
				throw new IOException("Primitive packet is corrupted!");
		}
	}
	
	/**
	 * Only allows primitive data types, null, and Strings
	 * @param value The data to store
	 * @see #getValue()
	 */
	public void setValue(Object value) {
		this.value = value;
		if (value == null) {
			data = new byte[] {0};
			return;
		}
		try (ByteArrayOutputStream data = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(data);) {
			switch (value.getClass().getName()) {
				case "java.lang.Boolean":
					out.write(1);
					out.writeBoolean((Boolean) value);
					break;
				case "java.lang.Byte":
					out.write(2);
					out.writeByte((Byte) value);
					break;
				case "java.lang.Short":
					out.write(3);
					out.writeShort((Short) value);
					break;
				case "java.lang.Character":
					out.write(4);
					out.writeChar((Character) value);
					break;
				case "java.lang.Integer":
					out.write(5);
					out.writeInt((Integer) value);
					break;
				case "java.lang.Long":
					out.write(6);
					out.writeLong((Long) value);
					break;
				case "java.lang.Float":
					out.write(7);
					out.writeFloat((Float) value);
					break;
				case "java.lang.Double":
					out.write(8);
					out.writeDouble((Double) value);
					break;
				case "java.lang.String":
					out.write(9);
					out.writeInt(((String) value).length());
					out.writeChars((String) value);
					break;
				default:
					throw new IllegalArgumentException("Only primitive data types and Strings are supported!");
			}
			this.data = data.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException("Impossible, this error is", e);
		}
	}
	
	/**
	 * Gets the stored value <br>
	 * Cast the result to the stored data type if known
	 * @see #isNull()
	 * @see #isBoolean()
	 * @see #isByte()
	 * @see #isShort()
	 * @see #isChar()
	 * @see #isInteger()
	 * @see #isLong()
	 * @see #isFloat()
	 * @see #isDouble()
	 * @see #isString()
	 * @return The stored data
	 */
	public Object getValue() {
		return value;
	}
	/**
	 * @return If the stored value is null
	 */
	public boolean isNull() {
		return value == null;
	}
	/**
	 * @return If the stored value is a boolean
	 */
	public boolean isBoolean() {
		return value instanceof Boolean;
	}
	/**
	 * @return If the stored value is a byte
	 */
	public boolean isByte() {
		return value instanceof Byte;
	}
	/**
	 * @return If the stored value is a short
	 */
	public boolean isShort() {
		return value instanceof Short;
	}
	/**
	 * @return If the stored value is a character
	 */
	public boolean isChar() {
		return value instanceof Character;
	}
	/**
	 * @return If the stored value is an integer
	 */
	public boolean isInteger() {
		return value instanceof Integer;
	}
	/**
	 * @return If the stored value is a long
	 */
	public boolean isLong() {
		return value instanceof Long;
	}
	/**
	 * @return If the stored value is a float
	 */
	public boolean isFloat() {
		return value instanceof Float;
	}
	/**
	 * @return If the stored value is a double
	 */
	public boolean isDouble() {
		return value instanceof Double;
	}
	/**
	 * @return If the stored value is a string
	 */
	public boolean isString() {
		return value instanceof String;
	}
	
	public void write(DataOutputStream out) throws IOException {
		out.write(data);
	}
	
}
