package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import model.Download;
import model.FileDetails;
import model.query.Query;

public class Status {
	private StringProperty 					usernameProperty;
	private BooleanProperty 				connectedProperty;
	private BooleanProperty					inPublicDir;
	private FileDetails 		   			fileDetails;
	private ObservableList<FileDetails> 	fileList;
	private Stack<String> 		   			pathLevel;
	private Download			   			server;
	private Consumer<String>	        onLevelChange;
 	
	public Status(Download download) {
		this.usernameProperty = new  SimpleStringProperty();
		this.connectedProperty = new SimpleBooleanProperty(false);
		
		this.connectedProperty.addListener((obs, oldVal, newVal)->{
			if(newVal.equals(false)) {
				this.inPublicDir.set(true);
			}else {
				this.inPublicDir.set(false);
			}
		});
		
		
		this.fileList 			= FXCollections.observableArrayList();
		this.pathLevel 			= new Stack<>();
		this.inPublicDir 		= new SimpleBooleanProperty(true);
		this.server				= download;
		
	}

	
	
	public void refreshFileList() throws IOException {
		Collection<Query> files = new ArrayList<>();
		Collection<FileDetails> displayedFiles = new ArrayList<>();
		
		if(this.inPublicDir.get()) {
			files = this.server.directoryFiles(Download.PUBLIC_PATH);
		}else {
			files = this.server.directoryFiles(this.fullPath());
		}
		
		displayedFiles = files.stream()
							 .filter(q->!q.dataAsString().equals(".") && !q.dataAsString().equals(".."))
							 .map(query-> new FileDetails(query.dataAsString(), query.getCode() == 1))
							 .collect(Collectors.toList());

		if(this.inPublicDir.get()) {
			displayedFiles = displayedFiles.stream()
										   .map(f->extractCurrentDir(f.getPath()))
										   .filter(path->!path.equals(""))
										   .map(path->new FileDetails(path, true))
										   .collect(Collectors.toSet());
		}
		
		this.fileList.setAll(displayedFiles);
		
	}
	
	public String extractCurrentDir(String filePath) {
		String result = "";
		
		if(filePath.startsWith(this.fullPath())) {
			int startIndex = this.fullPath().length();
			String[] parts = filePath.substring(startIndex).split("/");
			if(parts.length > 0)
				result= parts[0];
		}
		
		return result;
	}
	
	
	public void nextLevel(String nextLevel) {
		this.pathLevel.push(nextLevel);
		this.onLevelChange.accept(this.fullPath());
	}
	
	public void previousLevel() {
		if(!pathLevel.isEmpty()) {
			pathLevel.pop();
			this.onLevelChange.accept(this.fullPath());
		}
	}
	
	public void clearLevels(){
		this.pathLevel.clear();
		this.onLevelChange.accept("");
	}
	
	
	public String fullPath() {
		String path = "";
		
		for(int i = 0; i < this.pathLevel.size(); i++) {
			path += this.pathLevel.get(i) + "/";
		}
		
		return path;
	}
	
	
	public void setFileDetails(FileDetails fileDetails) throws IOException {
		if(fileDetails != null ) {
			Query details = null;
			
			details = this.server.getFileDetails(this.fullPath() + fileDetails.getPath());

			if(details.getCode() == Download.NOT_FOUND && !isInPublic()) {
				throw new IllegalArgumentException("Le fichier demandé : " + fileDetails.getPath() + " n'existe pas");
			}else if(details.getCode() == Download.NOT_FOUND) {
				fileDetails.setSize(4);
				fileDetails.setType("directory");
			}else {
				fileDetails.setSize(details.getCode());
				fileDetails.setType(details.dataAsString());
			}
			
			
		}
		this.fileDetails = fileDetails;
	}

	
	
	public void setOnLevelChange(Consumer<String> action) {
		this.onLevelChange = action;
	}
	
	public void setConnected(Boolean isConnected) {
		this.connectedProperty.set(isConnected);
	}
	
	public void setUsername(String username) {
		this.usernameProperty.set(username);
	}
	
	
	public boolean isConnected() {
		return this.connectedProperty.get();
	}
	
	public String getUsername() {
		return this.usernameProperty.get();
	}
	
	public StringProperty usernameProperty() {
		return this.usernameProperty;
	}

	public BooleanProperty connectedProperty() {
		return connectedProperty;
	}
	
	public ObservableList<FileDetails> getFileList(){
		return this.fileList;
	}


	public FileDetails getFileDetails() {
		return fileDetails;
	}
	
	public BooleanProperty inPublicProperty() {
		return this.inPublicDir;
	}

	public boolean isInPublic() {
		return this.inPublicDir.get();
	}


	public void setPublic(boolean inPublic) {
		this.inPublicProperty().set(inPublic);
	}
	
	
	
	
}
