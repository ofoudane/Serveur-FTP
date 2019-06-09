package model.query;

public interface AttributeSerializer {
	public byte[] 	toBytes();
	public void   	fromBytes(byte[] bytes);
	public boolean  isEnd();
}