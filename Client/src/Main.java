
	
import controller.MainController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.query.Query;


public class Main extends Application {
	
	
	@Override
	public void start(Stage primaryStage) {
		try {
			Parent root = FXMLLoader.load(Main.class.getResource("/view/View.fxml"));
			Scene scene = new Scene(root,1200, 600);
			scene.getStylesheets().add(getClass().getResource("/view/main.css").toExternalForm());
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		if(args.length > 2) {
			Query.STRING_SIZE = Integer.parseInt(args[2]);
		}
		
		if(args.length >= 2) {
			MainController.server = args[0];
			MainController.port = Integer.parseInt(args[1]);
			launch(args);
		}else {
			System.out.println("USAGE : [EXECUTABLE] [IP] [PORT] {[STRING_SIZE]=100}");
			System.exit(1);
		}
		
		
	}
}

