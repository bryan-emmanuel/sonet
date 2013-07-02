package com.piusvelte.sonet.core.social;

import java.util.List;

public class Status {
	
	public Status(String id, String message, Entity entity, List<Link> links, String imageURL, long created) {
		this.id = id;
		this.message = message;
		this.entity = entity;
		this.name = entity.getName();
		this.links = links;
		this.imageURL = imageURL;
		this.created = created;
	}
	
	public Status setComments(List<Comment> comments) {
		this.comments = comments;
		return this;
	}
	public List<Comment> getComments() {
		return comments;
	}
	
	String id;
	String message;
	Entity entity;
	String name;
	public String getName() {
		return name;
	}
	public Status setName(String name) {
		this.name = name;
		return this;
	}
	List<Comment> comments;
	List<Link> links;
	String imageURL;
	long created;
	public String getImageURL() {
		return imageURL;
	}
	public void setImageURL(String imageURL) {
		this.imageURL = imageURL;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public Entity getEntity() {
		return entity;
	}
	public void setEntity(Entity entity) {
		this.entity = entity;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<Link> getLinks() {
		return links;
	}
	public void setLinks(List<Link> links) {
		this.links = links;
	}
	public long getCreated() {
		return created;
	}
	public void setCreated(long created) {
		this.created = created;
	}

}
