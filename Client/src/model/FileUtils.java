package model;

import java.io.File;

public class FileUtils {
	public static String fileExtension(String filePath) {
		int dotInd = filePath.lastIndexOf('.');
		String ext = "";
		
		if(dotInd != -1) {
			ext = filePath.substring(dotInd);
		}
		
		return ext;
	}
	
	public static String fileExtension(File f) {
		return fileExtension(f.getPath());
	}
}
