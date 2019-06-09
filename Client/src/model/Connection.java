package model;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import model.query.AttributeSerializer;

public class Connection {
	private Socket socket;
	private DataInputStream input;
	private DataOutputStream output;

	public Connection(String host, Integer port) throws IllegalArgumentException {
		try {
			this.socket = new Socket(host, port);
			this.init();
		}catch(IOException e) {
			System.err.println("Exception initialising socket : " + e.getMessage());
			throw new IllegalArgumentException(e);
		}
	}
	
	public void init() throws IOException {
		this.input = new DataInputStream(this.socket.getInputStream());
		this.output = new DataOutputStream(this.socket.getOutputStream());
	}

	public void send(byte[] message) throws IOException{
		this.output.write(message);
		this.output.flush();
	}
	
	public void send(AttributeSerializer objToSend) throws IOException {
		this.send(objToSend.toBytes());
	}
	
	public byte[] readOnce(int size) throws IOException {		
		byte[] result = new byte[size];
		
		this.input.readFully(result);
		
		return result;	
	}
	
	
}
