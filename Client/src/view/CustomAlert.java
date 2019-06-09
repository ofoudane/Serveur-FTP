package view;

import java.util.Optional;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class CustomAlert {
	private final static Alert		 error_alert = new Alert(AlertType.ERROR);
	private final static Alert 	     success_alert = new Alert(AlertType.INFORMATION);
	private final static TextInputDialog input_dialog = new TextInputDialog();
	private final static Stage			stage 		  = new Stage();
	
	static {
		error_alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
		error_alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);

		success_alert.getDialogPane().setMinWidth(Region.USE_PREF_SIZE);
		success_alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);

		stage.hide();
	}
	
    public static void showError(String title, String message) {
    	error_alert.setHeaderText(title);
    	error_alert.setContentText(message);
    	error_alert.showAndWait();
    }
    
    public static void showError(Throwable e) {
    	e.printStackTrace();
    	String message = e.getMessage();
    	String title   = e.getClass().getSimpleName();
		
		showError(title, message);

    }
    
    
    public static void showInfo(String title, String message) {
    	success_alert.setHeaderText(title);
    	success_alert.setContentText(message);
    	success_alert.showAndWait();
    }
    
    
    public static Optional<String> askForInput(String title, String message) {
    	input_dialog.setTitle("Text input");
    	input_dialog.setHeaderText(title);
    	input_dialog.setContentText(message);
    	input_dialog.getEditor().clear();
    	Optional<String> result = input_dialog.showAndWait();
    	return result;
    }
    
    
    
}
