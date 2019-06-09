package testing;

import java.io.File;
import java.io.IOException;

import model.Connection;
import model.Download;

public class TestingConnectionToCServer {
	public static void main(String[] args) throws Exception{
		
		Connection conn = new Connection("192.168.1.82", 9001);
		authenticate(conn);
		uploadFile(conn);
	}
	
	public static void uploadFile(Connection connection) throws Exception{
		Download d = new Download(connection);
		File f = new File("C:\\Users\\omafou\\Desktop\\test.pdf");
		System.out.println(d.uploadFile(f, "new-FILE.pdf"));
	}
	
	
	public static void displayFiles(Connection connection) throws Exception {
		Download d = new Download(connection);
		System.out.println(d.directoryFiles());

	}
	
	
	public static void downloadFile(Connection connection) throws Exception {
		Download d = new Download(connection);
		String path = "./TP3.pdf";
		
		d.downloadFile("C:\\Users\\omafou\\Desktop\\test.txt", "omar/TP3.pdf");
		d.downloadFile("C:\\Users\\omafou\\Desktop\\test.txt", path);
		
	}

	public static void register(Connection connection) throws Exception{
		Download d = new Download(connection);
		
		System.out.println("INVALID : " + d.register("SMALL", "***"));
		System.out.println("CORRECT : " + d.register("omar", "myPassword8"));
		System.out.println("ALREADY_EXISTS : " + d.register("omar", "RepeatPassword"));
		
	}
	
	public static void authenticate(Connection connection) throws Exception{
		Download d = new Download(connection);
		
		System.out.println("UNKNOWN : " + d.connect("unknown", "****"));
		System.out.println("WRONG : " + d.connect("addOmar", "Qmati@c!806"));
		System.out.println("KNOWN : " + d.connect("omar", "myPassword8"));
		
	}
	
	public static void deleteFile(Connection connection) throws Exception{
		Download d = new Download(connection);
		
		System.out.println("NOT FOUND : " + d.deleteFile("not-exists.pdf"));
		System.out.println("FOUND : " + d.deleteFile("new-FILE.pdf"));
	}
	
	public static void updateState(Connection connection) throws Exception{
		Download d = new Download(connection);
		
		System.out.println("NOT FOUND : " + d.updatePublicState("not-exists.pdf", true));
		System.out.println("UPDATE TO PUBLIC  : " + d.updatePublicState("TP3.pdf", true));
		System.out.println("UPDATE TO PUBLIC  : " + d.updatePublicState("TP3-2.pdf", true));
		System.out.println("UPDATE TO PRIVATE : " + d.updatePublicState("TP3-2.pdf", false));

	}
	
	public static void fileDetails(Connection connection) throws IOException {
		Download d = new Download(connection);
		
		System.out.println("NOT FOUND : " + d.getFileDetails("not-exists.pdf"));
		System.out.println("FILE  : " + d.getFileDetails("TP3.pdf"));
		System.out.println("DIR : " + d.getFileDetails("Dir"));

	}
}
