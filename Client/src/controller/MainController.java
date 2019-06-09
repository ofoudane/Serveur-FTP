package controller;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import model.Connection;
import model.Download;
import model.FileDetails;
import model.FileUtils;
import model.query.Query;
import view.CustomAlert;
import view.FilePicker;


public class MainController implements Initializable{
	public static int port;
	public static String server;

   @FXML
    private Label userStatusLabel;

    @FXML
    private Label usernameLabel;

    @FXML
    private Button addFileButton;

    @FXML
    private Button createDirButton;

    @FXML
    private Button connectionStatusButton;

    @FXML
    private Button switchPublicDirButton;
    
    @FXML
    private Button backButton;
    
    @FXML
    private TableView<FileDetails> fileListTable;

    @FXML
    private TableColumn<FileDetails, String> fileColumn;
    
    @FXML
    private VBox fileDetailsPane;

    @FXML
    private Label fileTypeLabel;

    @FXML
    private Label fileStatusLabel;

    @FXML
    private Label fileSizeLabel;

    @FXML
    private Button downloadButton;

    @FXML
    private Button changeFileStatusButton;

    @FXML
    private Button deleteFileButton;

    @FXML
    private Button registerButton;

    @FXML
    private Label currentDirLabel;
    
    private Status status;
    private Download download;

    @FXML
    void addFile(ActionEvent event) throws InterruptedException {
    	File fileToUpload = FilePicker.pickAFile();
    	if(fileToUpload != null && this.status.isConnected()) {
    		
    		Optional<String> name = CustomAlert.askForInput("Importation", "Nom du fichier : ");
    		
    		if(name.isPresent()) {
    			try {
					Query response = this.download.uploadFile(fileToUpload, this.status.fullPath() + "/" + name.get() + FileUtils.fileExtension(fileToUpload));
					if(response.getCode() == Download.SUCCESS) {
						CustomAlert.showInfo("Chargement réussi", "Le fichier " + fileToUpload.getPath() + " a été chargé avec succès");
						this.refresh();
					}else {
						CustomAlert.showError("Chargement échouée", "Le fichier " + fileToUpload.getPath() + " n'a pas pu être transféré au serveur");
					}
    			} catch (IOException e) {
					CustomAlert.showError("Erreur upload", "Une erreur est survenue lors de chargement du fichier vers le serveur");
				}
    		}
    		
    	}
    }
    
    @FXML
    void refresh() {
		try {
			this.status.refreshFileList();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    @FXML
    void switchPublicDir() {
    	this.status.setPublic(!this.status.isInPublic());
    	this.status.clearLevels();
    	try {
    		this.refresh();
    	}catch(Exception e) {
    		
    	}
    }
    
    
    @FXML
    void goBack() {
    	this.status.previousLevel();
    }
    
    @FXML
    void createDir(ActionEvent event) throws InterruptedException {
    	Optional<String> dirname = CustomAlert.askForInput("Création d'un répertoire", "Nom du nouveau répertoire : ");
    	if(dirname.isPresent()) {
    		try {
    			Query response = this.download.createDirectory(this.status.fullPath() + dirname.get());
    			if(response.getCode() == Download.SUCCESS) {
        			CustomAlert.showInfo("Création du répertoire réussie", "Le répertoire " + dirname.get() + " a bien été créé");
        			this.refresh();
    			}else {
        			CustomAlert.showError("Création du répertoire échouée", "Une erreur est survenue lors de la création du répertoire " + dirname.get());
    			}
    		}catch(IOException e) {
    			CustomAlert.showError("Erreur de création d'un répertoire", "Une erreur est survenue lors de la création du répertoire " + dirname.get());
    		}
    	}
    	
    }


    @FXML
    void changeConnectionStatus(ActionEvent event) throws InterruptedException {
    	this.status.clearLevels();

    	if(this.status.isConnected()) {
    		try {
    			this.download.disconnect();
    			this.status.setConnected(false);
    		}catch(IOException e) {
    			CustomAlert.showError("Erreur de déconnexion", "Une erreur est survenue lors de la déconnexion : " + e.getMessage());
    		}
    	}else {
    		try {
    			Optional<String> username = CustomAlert.askForInput("Connexion", "Nom d'utilisateur : ");
    			Optional<String> password = CustomAlert.askForInput("Connexion", "Mot de passe : ");
    			
    			if(username.isPresent() && password.isPresent()) {
    				int code = this.download.connect(username.get().toLowerCase(), password.get()).getCode();
    				
    				if(code == Download.SUCCESS) {
    					this.status.setConnected(true);
    					this.status.setUsername(username.get().toLowerCase());
    				}else {
        				CustomAlert.showError("Connexion échouée", "Mot de passe ou nom d'utilisateur incorrecte");
    				}
    			}else {
    				CustomAlert.showInfo("Connexion", "Vous devez fournir un nom d'utilisateur et un mot de passe valides");
    			}
    		}catch(IOException e) {
    			CustomAlert.showError("Erreur de connexion", "Une erreur est survenue lors de la connexion : " + e.getMessage());
    		}
    	}
    	
    	
    }
    
    @FXML 
    void register(ActionEvent event) throws InterruptedException {
    	if(!this.status.isConnected()) {
    		try {
    			Optional<String> username = CustomAlert.askForInput("Inscription", "Nom d'utilisateur : ");
    			Optional<String> password = CustomAlert.askForInput("Inscription", "Mot de passe : ");
    			
    			if(username.isPresent() && password.isPresent()) {
    				if(username.get().length() >= Download.USERNAME_MIN_LENGTH && password.get().length() >= Download.PASSWORD_MIN_LENGTH) {
    					Query registerResponse = this.download.register(username.get().toLowerCase(), password.get());
    					
    					if(registerResponse.getCode() == Download.SUCCESS) {
    						CustomAlert.showInfo("Inscription réussie", "Votre inscription est réussie, vous pouvez vous connecter dès maintenant avec : " + username.get());
    					}else if(registerResponse.getCode() == Download.USER_ALREADY_EXISTS) {
    						CustomAlert.showError("Inscription échouée", "L'utilisateur " + username.get() + " existe déjà. Veuillez choisir un autre nom d'utilisateur");
    					}else {
    						CustomAlert.showError("Erreur Inscription", registerResponse.dataAsString());
    					}
    				}else{
        				CustomAlert.showError("Inscription", "Votre nom d'utilisateur doit contenir au moins 2 caractères et votre mot de passe doit être composé de 8 caractères");
    				}
    			}else {
    				CustomAlert.showInfo("Inscription", "Vous devez fournir un nom d'utilisateur et un mot de passe valides");
    			}
    		}catch(IOException e) {
    			
    		}
    	}
    }

    @FXML
    void changeFileStatus(ActionEvent event) {
    	FileDetails details = this.status.getFileDetails();
    	
    	try {
			Query response = this.download.updatePublicState(this.status.fullPath() + details.getPath(), !details.isPublic());
			if(response.getCode() == Download.SUCCESS) {
				details.isPublic(!details.isPublic());
				this.updateFileDetails();
				CustomAlert.showInfo("Mise à jour réussie", "L'état du fichier " + details.getPath() + " a été met à jour avec succès");
			}else {
				CustomAlert.showError("Fichier introuvable", "Le fichier que vous voulez changer n'est plus disponible");
			}
		} catch (IOException e) {
			CustomAlert.showError("Erreur", "Une erreur est survenue lors de changement de l'état du fichier : " + details.getPath());
		}
    	
    }


    @FXML
    void deleteFile(ActionEvent event) {
    	FileDetails details = this.status.getFileDetails();
    	
    	try {
			Query response = this.download.deleteFile(this.status.fullPath() + details.getPath());
			if(response.getCode() == Download.SUCCESS) {
				this.refresh();
				CustomAlert.showInfo("Suppression réussie", "Le fichier " + details.getPath() + " a été supprimé avec succès");
			}else {
				CustomAlert.showError("Fichier introuvable", "Le fichier que vous voulez supprimer n'est plus disponible");
			}
		} catch (IOException e) {
			CustomAlert.showError("Erreur", "Une erreur est survenue lors de la suppression du fichier : " + details.getPath());
		}

    	
    }

    @FXML
    void download(ActionEvent event) throws InterruptedException {
    	FileDetails details = this.status.getFileDetails();

    	if(details != null && !details.isDirectory()) {
    		File f = FilePicker.pickAFile(true);
    		
    		try {
    			if(f != null) {
    				this.download.downloadFile(f.getPath() + FileUtils.fileExtension(details.getPath()), (this.status.inPublicProperty().get() ? this.status.fullPath() : "") + details.getPath());
    				CustomAlert.showInfo("Téléchargement réussi", "Le fichier " + details.getPath() + " a bien été téléchargé");
    			}
    		} catch(NoSuchFileException e) {
				CustomAlert.showError("Fichier introuvable", "Le fichier que vous essayez de télécharger n'est plus disponible : " + details.getPath());
			} catch (IOException e) {
				CustomAlert.showError("Erreur Téléchargement", "Une erreur est survenue lors du téléchargement du fichier " + details.getPath());
			} 
    		
    	}else if(details != null) {
			this.status.nextLevel(details.getPath());
    	}
    }
    
    private void switchToPrivate() {
    	this.addFileButton.setDisable(false);
    	this.createDirButton.setDisable(false);
    	this.changeFileStatusButton.setDisable(false);
    	this.deleteFileButton.setDisable(false);
    }
    
    private void switchToPublic() {
		this.addFileButton.setDisable(true);
		this.createDirButton.setDisable(true);
    	this.changeFileStatusButton.setDisable(true);
    	this.deleteFileButton.setDisable(true);
    }

    
    private void clearFileDetails() {
    	this.fileDetailsPane.setOpacity(0);
    }
    
    private void updateFileDetails() {
    	FileDetails details = this.status.getFileDetails();
    	if(details == null) {
    		this.clearFileDetails();
    	}else {
    		this.fileDetailsPane.setOpacity(1);
    		this.fileTypeLabel.setText(details.isDirectory() ? "dossier" : "fichier");
    		this.fileSizeLabel.setText(details.getSize() == 0 ? " < 1 ko"  : details.getSize() + " ko");
    		this.fileStatusLabel.setText(details.isPublic() ? "Public" : "Priv�");
    		
    		this.downloadButton.setText(details.isDirectory() ? "Parcourir" : "T�l�charger");
    		this.changeFileStatusButton.setText(details.isPublic() ? "Rendre priv�" : "Rendre public");
    		this.changeFileStatusButton.setDisable(details.isDirectory() || !this.status.isConnected());
    		
    	}
    }
    
    
	@Override
	public void initialize(URL location, ResourceBundle resources){
		this.download = new Download(new Connection(server, port));
		this.status = new Status(download);
		
		this.status.setOnLevelChange((newPath)->{
			if(newPath.equals("")) {
				this.currentDirLabel.setText("/");
				this.backButton.setDisable(true);
			}else {
				this.currentDirLabel.setText(newPath);
				this.backButton.setDisable(false);
			}
			this.refresh();
		});
		
		this.status.inPublicProperty().addListener((obs, oldVal, newVal)->{
			if(newVal.equals(true)) {
				this.switchToPublic();
				if(this.status.isConnected()) {
					this.switchPublicDirButton.setText("Espace priv�");
				}
			}else {
				this.switchToPrivate();
				this.switchPublicDirButton.setText("Espace public");
			}
		});
		
		
		this.status.connectedProperty().addListener((obs, oldVal, newVal)->{
			this.userStatusLabel.getStyleClass().clear();
			if(newVal.equals(true)) {
				this.switchPublicDirButton.setDisable(false);
				this.userStatusLabel.setText("Connecté");
				this.userStatusLabel.getStyleClass().add("succes-color");
				this.connectionStatusButton.setStyle("-fx-background-color : #fb3604;");
				this.connectionStatusButton.setText("Se déconnecter");
				this.switchToPrivate();
				this.registerButton.setDisable(true);
				
			}else {
				this.switchPublicDirButton.setDisable(true);
				this.userStatusLabel.setText("Non-connecté");
				this.usernameLabel.setText("Identifiez-vous");
				this.userStatusLabel.getStyleClass().add("danger-color");
				this.connectionStatusButton.setStyle("-fx-background-color: #03dac5;");
				this.connectionStatusButton.setText("Se connecter");
				this.switchToPublic();
				this.registerButton.setDisable(false);
			}
			
			this.refresh();
		});
		
		this.status.usernameProperty().addListener((obs, oldVal, newVal)->{
			if(newVal != null) {
				this.usernameLabel.setText(newVal);
			}else {
				this.usernameLabel.setText("Vous êtes déconnecter");
			}
		});
		
		this.fileListTable.setItems(this.status.getFileList());
		
		this.fileColumn.setCellValueFactory(v->new SimpleStringProperty(v.getValue().getPath()));
		
		this.fileListTable.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem)->{			
			try {
				try {
					this.status.setFileDetails(newItem);
				}catch(IllegalArgumentException e) {
					CustomAlert.showError("Fichier inéxistant", "Ce fichier n'existe plus ! Veuillez choisir un autre fichier");
					this.refresh();
				}
			} catch (IOException e) {
				CustomAlert.showError("Erreur", "Une erreur est survenue lors de la communication avec le serveur");
			} 			
			this.updateFileDetails();
		});
		
		
		this.refresh();
		this.switchToPublic();
		this.updateFileDetails();
	}
}
