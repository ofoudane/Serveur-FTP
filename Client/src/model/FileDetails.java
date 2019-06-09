package model;

public class FileDetails {
	private String type;
	private Boolean isPublic;
	private Integer size;
	private String path;
	
	
	public FileDetails(String path, Boolean isPublic) {
		this.path = path;
		this.isPublic = isPublic;
	}
	
	
	
	public FileDetails(String type, Boolean isPublic, Integer size, String path) {
		this.type = type;
		this.isPublic = isPublic;
		this.size = size;
		this.path = path;
	}


	public String getOwner() {
		return "";
	}
	

	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Boolean isPublic() {
		return isPublic;
	}
	public void isPublic(Boolean isPublic) {
		this.isPublic = isPublic;
	}
	public Integer getSize() {
		return size;
	}
	public void setSize(Integer size) {
		this.size = size;
	}
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}

	public boolean isDirectory() {
		return this.type.equalsIgnoreCase("directory");
	}
	
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileDetails other = (FileDetails) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}



	@Override
	public String toString() {
		return "FileDetails [type=" + type + ", isPublic=" + isPublic + ", size=" + size + ", path=" + path + "]";
	}
	
	
	
	
	
}
