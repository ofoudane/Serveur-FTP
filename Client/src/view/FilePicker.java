package view;

import java.io.File;

import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class FilePicker {
	public final static FileChooser filePicker = new FileChooser();
	private final static Stage 		dialogStage = new Stage();
	
	public static File pickAFile() {
		return pickAFile(false);
	}
	
	public static File pickAFile(Boolean toSave) {
		File pickedFile ;
		if(toSave) pickedFile = filePicker.showSaveDialog(dialogStage);
		else	   pickedFile = filePicker.showOpenDialog(dialogStage);
		
		if(pickedFile != null) {
			filePicker.setInitialDirectory(pickedFile.getParentFile());
		}
		
		return pickedFile;

	}
}
