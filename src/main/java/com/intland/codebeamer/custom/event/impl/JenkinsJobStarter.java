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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.MapUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intland.codebeamer.event.BaseEvent;
import com.intland.codebeamer.event.impl.AbstractWorkflowActionPlugin;
import com.intland.codebeamer.manager.configuration.DaoBasedConfigurationManager;
import com.intland.codebeamer.manager.util.ActionData;
import com.intland.codebeamer.manager.workflow.ActionCall;
import com.intland.codebeamer.manager.workflow.ActionParam;
import com.intland.codebeamer.manager.workflow.ActionWarning;
import com.intland.codebeamer.manager.workflow.WorkflowAction;
import com.intland.codebeamer.manager.workflow.WorkflowPhase;
import com.intland.codebeamer.persistence.dto.ArtifactDto;
import com.intland.codebeamer.persistence.dto.TrackerItemDto;

@Component("jenkinsJobStarter")
@WorkflowAction(value = "jenkinsJobStarter", iconUrl = "/images/Snapshot.png")
public class JenkinsJobStarter extends AbstractWorkflowActionPlugin {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = LogManager.getLogger(JenkinsJobStarter.class);

	private static final String BODY_PARAMETER_NAME = "json";

	private static final String JENKINS_JOB_PARAMETERS = "jenkinsJobParameters";

	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private DaoBasedConfigurationManager configurationManager;
	
	public JenkinsJobStarter() {
	}

	@ActionCall(WorkflowPhase.After)
	public void startJenkinsJob(BaseEvent<ArtifactDto, TrackerItemDto, ActionData<?>> event, TrackerItemDto trackerItem,
			@ActionParam(value = JENKINS_JOB_PARAMETERS, width = 100) Map<String, Object> jenkinsJobParameters) throws Exception {
		
		try {
			
			JenkinsServerConfig jenkinsServerConfig = new JenkinsServerConfig(configurationManager.readConfigurationWithOverride());
			if (!jenkinsServerConfig.isEmpty()) {
				logger.debug("Jenkins workflow is not configured");
				return;
			}
			
			if (!jenkinsServerConfig.isValid()) {
				logger.warn("Jenkins configuration is invalid");
				logger.trace("JenkinsServerConfig: {}", jenkinsServerConfig);
				return;
			}
			
			URI uri = jenkinsServerConfig.getUri();
			HttpHost host = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
			CredentialsProvider credsProvider = createCredentialsProvider(host, jenkinsServerConfig);

			StringEntity entity = buildHttpEntity(jenkinsJobParameters);
			startJenkinsJob(entity, uri, host, credsProvider);
			
		} catch (Exception e) {
			logger.error("Jenkins job cannot be started", e);
			throw new ActionWarning("Jenkins job cannot be started", e);
		}

	}

	private void startJenkinsJob(StringEntity entity, URI uri, HttpHost host, CredentialsProvider credsProvider) throws Exception {
		HttpPost httpRequest = new HttpPost(uri);
		httpRequest.setEntity(entity);

		try (CloseableHttpClient httpClient = createHttpClient(credsProvider)) {

			HttpResponse response = httpClient.execute(host, httpRequest, createLocalContext(host));

			if (!HttpStatus.valueOf(response.getStatusLine().getStatusCode()).is2xxSuccessful()) {
				String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
				throw new IllegalStateException("Jenkins returned with a not 2xx code, response: " + responseBody);
			}

		} finally {
			EntityUtils.consume(httpRequest.getEntity());
		}
	}

	private CloseableHttpClient createHttpClient(CredentialsProvider credsProvider) {
		return HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
	}

	private UrlEncodedFormEntity buildHttpEntity(Map<String, Object> jenkinsJobParameters) throws JsonProcessingException, UnsupportedEncodingException {
		String parameters = getJsonParameterAsJson(jenkinsJobParameters);
		logger.info("Following parameters will be used: {}", parameters);
		return new UrlEncodedFormEntity(buildParameters(new BasicNameValuePair(BODY_PARAMETER_NAME, parameters)), StandardCharsets.UTF_8.name());
	}

	private ArrayList<NameValuePair> buildParameters(NameValuePair parameter) {
		ArrayList<NameValuePair> parameters = new ArrayList<NameValuePair>();
		parameters.add(parameter);
		return parameters;
	}

	private String getJsonParameterAsJson(Map<String, Object> jenkinsJobParameters) throws JsonProcessingException {
		return mapper.writeValueAsString(new JenkinsJobData(getParameters(jenkinsJobParameters)));
	}

	private List<JenkinsParameter> getParameters(Map<String, Object> jenkinsJobParameters) {
		if (MapUtils.isEmpty(jenkinsJobParameters)) {
			return Collections.emptyList();
		}
		
		return buildParameters(jenkinsJobParameters);
	}

	private List<JenkinsParameter> buildParameters(Map<String, Object> jenkinsJobParameters) {
		return jenkinsJobParameters.entrySet().stream()
				.filter(e -> e.getValue() != null)
				.map(e -> new JenkinsParameter(e.getKey(), e.getValue()))
				.collect(Collectors.toList());
	}
	
	private HttpClientContext createLocalContext(HttpHost host) {
		HttpClientContext localContext = HttpClientContext.create();
		localContext.setAuthCache(creatAuthCache(host));
		return localContext;
	}

	private AuthCache creatAuthCache(HttpHost host) {
		AuthCache authCache = new BasicAuthCache();
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(host, basicAuth);

		return authCache;
	}

	private CredentialsProvider createCredentialsProvider(HttpHost httpHost, JenkinsServerConfig jenkinsServerConfig) {
		return createCredentialsProvider(httpHost, jenkinsServerConfig.getUserName(), jenkinsServerConfig.getToken());
	}
	
	private CredentialsProvider createCredentialsProvider(HttpHost httpHost, String userName, String password) {
		UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(userName, password);

		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(httpHost.getHostName(), httpHost.getPort()), credentials);

		return credsProvider;
	}
	
}
