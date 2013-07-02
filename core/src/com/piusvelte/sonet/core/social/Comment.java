package com.piusvelte.sonet.core.social;

import com.piusvelte.sonet.core.Sonet;

public class Comment {
	
	private String id;
	private String message;
	private String status;
	private long created;
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public long getCreated() {
		return created;
	}
	public void setCreated(long created) {
		this.created = created;
	}
	public String getCreated(boolean is24hr) {
		return Sonet.getCreatedText(created, is24hr);
	}
	private Entity entity;
	public Entity getEntity() {
		return entity;
	}
	public void setEntity(Entity entity) {
		this.entity = entity;
	}
	
	public Comment(String id, String message, Entity entity, String status, long created) {
		this.id = id;
		this.message = message;
		this.entity = entity;
		this.status = status;
		this.created = created;
	}

}
