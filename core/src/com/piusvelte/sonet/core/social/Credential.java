package com.piusvelte.sonet.core.social;

import org.apache.http.client.methods.HttpUriRequest;

public abstract class Credential {
	
	protected Credential(Builder builder) {
		
	}
	
	abstract boolean hasCredentials();
	abstract HttpUriRequest sign(HttpUriRequest request);
	
	abstract class Builder {
		
		public abstract Credential build();
		
	}

}
