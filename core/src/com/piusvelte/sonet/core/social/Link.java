package com.piusvelte.sonet.core.social;

public class Link {
	
	public Link (String type, String location) {
		this.type = type;
		this.location = location;
	}
	
	private String type;
	private String location;
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}

}
