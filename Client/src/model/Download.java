package model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;

import model.conncetors.QueryConnector;
import model.query.Query;

public class Download {
	public static final int SUCCESS = 1;
	public final static int FAILED  = 0;
	public final static int NOT_FOUND = -1;

	public static final int WRONG_PASSWORD = 0;
	public static final int UNKNOWN_USER   = -1;
	public final static int USERNAME_MIN_LENGTH = 2;
	public final static int PASSWORD_MIN_LENGTH = 8;
	public final static int USER_ALREADY_EXISTS = 0;
	public final static int INVALID_CREDENTIALS = -1;

	public final static String PUBLIC_PATH		= "#~public";
	
	private QueryConnector queryConnector;
	
	
	public Download(Connection connection) {
		this.queryConnector = new QueryConnector(connection);
	}
	
	public Collection<Query> directoryFiles(String directory) throws IOException{
		Query askForDirectory = new Query(directory, Query.DISPLAY_COMMAND);

		this.queryConnector.send(askForDirectory);

		ArrayList<Query> result = this.queryConnector.receive();
		
		return result;
	}
	
	public Collection<Query> directoryFiles() throws IOException{
		return this.directoryFiles(".");
	}
	
	public void downloadFile(File destFile, String path) throws IOException{
		
		
		Query downloadQuery = new Query(path, Query.DOWNLOAD_COMMAND);
		Query currentQuery	= null;
		
		queryConnector.send(downloadQuery);
		
		currentQuery = this.queryConnector.readOnce();

		if(currentQuery.getCode() == NOT_FOUND) {
			throw new NoSuchFileException(path);
		}else {
			FileOutputStream output = new FileOutputStream(destFile);
			
			while(!currentQuery.isEnd()){
				output.write(currentQuery.getData(), 0, currentQuery.getCode());
				currentQuery = this.queryConnector.readOnce();
			}
			
			output.flush();
			output.close();
		}
		
		

	}
	
	
	public void downloadFile(String newFilePath, String path) throws IOException {
		this.downloadFile(new File(newFilePath), path);
	}
	
	
	public Query uploadFile(File fileToUpload, String serverDestination) throws IOException{
		if(fileToUpload == null || !fileToUpload.exists()) {
			throw new NoSuchFileException("The file you wanted to upload is invalid !");
		}else {
			FileInputStream inputStream = new FileInputStream(fileToUpload);
			Query queryToSend = null;
			byte[] buffer 	  = new byte[Query.STRING_SIZE];
			int readBytes = Query.STRING_SIZE;
			Query response    = null;
			
			this.queryConnector.send(new Query(serverDestination, Query.UPLOAD_COMMAND));
			
			while(readBytes == Query.STRING_SIZE ) {				
				readBytes = inputStream.read(buffer);	
				queryToSend = new Query(buffer, readBytes);
				this.queryConnector.send(queryToSend);
			}
			
			this.queryConnector.sendStop();
			
			response = this.queryConnector.readOnce();
			
			inputStream.close();
			
			return response;
		}
	}
	
	
	public Query connect(String username, String password) throws IOException {
		Query usernameQuery = new Query(username, Query.CONNECTION_COMMAND);
		Query passwordQuery = new Query(password, Query.CONNECTION_COMMAND);
		
		this.queryConnector.send(usernameQuery);
		this.queryConnector.send(passwordQuery);
		
		return this.queryConnector.readOnce();
	}
	
	public Query register(String username, String password) throws IOException {
		Query usernameQuery = new Query(username, Query.REGISTER_COMMAND);
		Query passwordQuery = new Query(password, Query.REGISTER_COMMAND);
		
		this.queryConnector.send(usernameQuery);
		this.queryConnector.send(passwordQuery);
		
		return this.queryConnector.readOnce();
	}
	
	public Query deleteFile(String filePath) throws IOException {
		Query request = new Query(filePath, Query.DELETE_COMMAND);
		
		this.queryConnector.send(request);
		
		return this.queryConnector.readOnce();
	}
	
	public Query updatePublicState(String filePath, boolean isPublic) throws IOException {
		
		Query stateChangeRequest = new Query(filePath, Query.PUBLIC_STATE_COMMAND);
		
		Query updatedStateQuery  = new Query(filePath, isPublic ? 1 : 0);
		
		this.queryConnector.send(stateChangeRequest);
		this.queryConnector.send(updatedStateQuery);

		return this.queryConnector.readOnce();
	}
	
	
	public Query createDirectory(String newDirectoryPath) throws IOException {
		
		Query createDirQuery = new Query(newDirectoryPath, Query.CREATE_DIR_COMMAND);
		
		this.queryConnector.send(createDirQuery);
		
		return this.queryConnector.readOnce();
	}
	
	public void disconnect() throws IOException{
		Query request = new Query("Disconnect", Query.DISCONNECT_COMMAND);
		this.queryConnector.send(request);
	}
	
	public Query getFileDetails(String filePath) throws IOException{
		Query request = new Query(filePath, Query.FILE_DETAILS_COMMAND);
		
		this.queryConnector.send(request);
		
		return this.queryConnector.readOnce();
	}
}
