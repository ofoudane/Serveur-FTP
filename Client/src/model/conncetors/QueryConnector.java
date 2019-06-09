package model.conncetors;

import java.io.IOException;
import model.Connection;
import model.query.Query;
import java.util.ArrayList;

public class QueryConnector extends ConcreteConnector<Query>{

	public QueryConnector(Connection connection) {
		super(connection);
	}

	@Override
	public void send(Query query) throws IOException {
		this.getConnection().send(query.toBytes());
		
	}

	@Override
	public ArrayList<Query> receive() throws IOException {
		ArrayList<Query> result = new ArrayList<>();
		Query currentQuery = null;
		
		do{
			currentQuery = this.readOnce();
			if(!currentQuery.isEnd())
				result.add(currentQuery);
		}while(!currentQuery.isEnd()) ;
		
		return result;
	}
	
	
	public Query readOnce() throws IOException {
		Query result = new Query();
		
		result.fromBytes(this.getConnection().readOnce(Query.sizeof()));
		
		return result;
	}

	@Override
	public void sendStop() throws IOException {
		Query stopQuery = new Query("", Query.END_COMMAND);
		this.send(stopQuery);
	}
}
