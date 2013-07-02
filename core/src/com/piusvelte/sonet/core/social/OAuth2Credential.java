package com.piusvelte.sonet.core.social;

import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.params.HttpParams;

public class OAuth2Credential extends Credential {

	private String token;
	
	protected OAuth2Credential(Builder builder) {
		super(builder);
		token = builder.getToken();
	}
	
	//TODO Foursquare uses oauth_token instead of access_token
	public static final String oauth_token = "oauth_token";
	public static final String access_token = "access_token";

	@Override
	HttpUriRequest sign(HttpUriRequest request) {
		HttpParams params = request.getParams();
		params.setParameter(access_token, token);
		request.setParams(params);
		return request;
	}

	@Override
	boolean hasCredentials() {
		return (token != null);
	}
	
	class Builder extends Credential.Builder {
		
		private String token;
		
		public String getToken() {
			return token;
		}

		public void setToken(String token) {
			this.token = token;
		}

		public Builder(String token) {
			this.token = token;
		}

		@Override
		public Credential build() {
			return new OAuth2Credential(this);
		}
		
		
		
	}

}
