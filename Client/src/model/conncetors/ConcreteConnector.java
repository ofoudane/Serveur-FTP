package model.conncetors;

import model.Connection;
import model.query.AttributeSerializer;

public abstract class ConcreteConnector<E extends AttributeSerializer> implements Connector<E>{
	private Connection connection;
	
	
	public ConcreteConnector(Connection connection) {
		this.connection = connection;
	}

	
	public Connection getConnection() {
		return this.connection;
	}
		

}
