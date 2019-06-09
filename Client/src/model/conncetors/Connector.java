package model.conncetors;

import java.io.IOException;
import java.util.ArrayList;

import model.query.AttributeSerializer;

public interface Connector<E extends AttributeSerializer>{
	public void send(E obj) throws IOException;
	public ArrayList<E> receive() throws IOException;
	public E 			readOnce() throws IOException;
	public void 		sendStop() throws IOException;
}
