/*
 * Copyright by Intland Software
 *
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Intland Software. ("Confidential Information"). You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Intland.
 */

package com.intland.codebeamer.custom.event.impl;

import java.net.URI;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import com.intland.codebeamer.config.PropertyInjectorConfig;

public class JenkinsServerConfig {

	private static final String JENKINS = "jenkins";

	private String url;
	private String userName;
	private String token;

	public JenkinsServerConfig(Configuration config) {
		try {
			new PropertyInjectorConfig(config, JENKINS, this);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public URI getUri() {
		Assert.hasText(getUrl(), "Url cannot be null or empty");
		return URI.create(getUrl());
	}

	public String getUrl() {
		return url;
	}

	public String getUserName() {
		return userName;
	}

	public String getToken() {
		return token;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public boolean isEmpty() {
		return StringUtils.isNotBlank(this.url) && StringUtils.isNotBlank(this.userName)
				&& StringUtils.isNotBlank(this.token);
	}

	public boolean isValid() {
		return !(StringUtils.isBlank(this.url) || StringUtils.isBlank(this.userName)
				|| StringUtils.isBlank(this.token));
	}

	@Override
	public String toString() {
		return "JenkinsServerConfig [url=" + url + ", userName=" + userName + ", token=" + token + "]";
	}

}