package model.query;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class Query implements AttributeSerializer{
	public  static int STRING_SIZE = 100;
	
	public final static int FILE_DETAILS_COMMAND = 9;
	public final static int DISCONNECT_COMMAND  = 8;
	public final static int CREATE_DIR_COMMAND  = 7;
	public final static int PUBLIC_STATE_COMMAND= 6;
	public final static int DELETE_COMMAND 		= 5;
	public final static int REGISTER_COMMAND 	= 4;
	public final static int DISPLAY_COMMAND 	= 3;
	public final static int UPLOAD_COMMAND  	= 2;
	public final static int DOWNLOAD_COMMAND 	= 1;
	public final static int CONNECTION_COMMAND 	= 0;
	public final static int END_COMMAND 		= -1;
	
	
	
	private int code;
	private byte[] data;

	
	public Query(byte[] data, int code) {
		this.code = code;
		this.data = ByteBuffer.allocate(STRING_SIZE).put(data).array();
	}
	
	public Query(String message, int code) {
		this(message.getBytes(), code);
	}
	
	
	public Query(byte[] data) {
		this(data, data.length);
	}

	public Query(String message) {
		this(message.getBytes());
	}
	
	public Query(int code) {
		this(new byte[0], code);
	}
	
	public Query() {
		this(new byte[STRING_SIZE], 0);
	}
	
	@Override
	public byte[] toBytes() {
		ByteBuffer buffer = ByteBuffer.allocate(sizeof());

		buffer.order(ByteOrder.LITTLE_ENDIAN);

		buffer.put(this.data);
						
		buffer.putInt(code);		
						
		return buffer.array();
	}

	@Override
	public void fromBytes(byte[] bytes) {
		ByteBuffer buffer = ByteBuffer.wrap(bytes);
		
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		
		buffer.get(this.data, 0, STRING_SIZE);
		
		buffer.position(STRING_SIZE);
		
		this.code = buffer.getInt();
	}
	
	@Override
	public boolean isEnd() {
		return this.getCode() == END_COMMAND;
	}
	
	
	public String dataAsString() {
		try {
			String res = new String(data, "utf8");
			
			int stopIndex = res.indexOf('\0');
			
			if(stopIndex == -1)
				stopIndex = res.length() - 1;
			
			return res.substring(0, stopIndex);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	
	
	
	
	
	public final static int sizeof() {
		return STRING_SIZE + Integer.SIZE / Byte.SIZE;
	}


	
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}
	
	public void setData(String message) {
		this.data = message.getBytes();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + code;
		result = prime * result + Arrays.hashCode(data);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Query other = (Query) obj;
		if (code != other.code)
			return false;
		if (!Arrays.equals(data, other.data))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Query [code=" + code + ", data=" + this.dataAsString() + "]";
	}
	
	
	
	
	
	

}
