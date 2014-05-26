/**
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   This file is part of the LDP4j Project:
 *     http://www.ldp4j.org/
 *
 *   Center for Open Middleware
 *     http://www.centeropenmiddleware.com/
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Copyright (C) 2014 Center for Open Middleware.
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 *   Artifact    : org.ldp4j.framework:ldp4j-server-impl:1.0.0-SNAPSHOT
 *   Bundle      : ldp4j-server-impl-1.0.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.server.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cxf.helpers.IOUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldp4j.server.Format;
import org.ldp4j.server.impl.ILinkedDataPlatformContainerManager;
import org.ldp4j.server.impl.LinkUtil;
import org.ldp4j.server.testing.TestingApplicationBuilder;
import org.ldp4j.server.testing.TestingUtil;
import org.ldp4j.server.testing.stubs.FailingContainer;
import org.ldp4j.server.testing.stubs.WorkingContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Arquillian.class)
public class LinkedDataPlatformContainerManagerITest {

	private static final String DEPLOYMENT = "ldp-server-container";
	private static final String CONTROL_PHRASE = "Hello from TomEE Plus 1.5.0 ("+DEPLOYMENT+")";
	private static final Logger LOGGER=LoggerFactory.getLogger(LinkedDataPlatformContainerManagerITest.class);

	@Deployment(name=DEPLOYMENT, testable=false)
	@TargetsContainer("tomee-plus")
	public static WebArchive createLinkedDataPlatformServerWar() {
		return 
			new TestingApplicationBuilder().
				withControlPhrase(CONTROL_PHRASE).
				withDeployableName(DEPLOYMENT.concat(".war")).
				withStubs().
				build();
	}

	@Test
	@OperateOnDeployment(DEPLOYMENT)
	public void testDeployment(@ArquillianResource final URL url) throws IOException {
		LOGGER.debug(String.format("* Checking %s Deployment (%s)",DEPLOYMENT,url));
		InputStream is = url.openStream();
		try {
			String content = IOUtils.readStringFromStream(is);
			LOGGER.debug("\t- Content: " + content);
			assertThat(content,equalTo(CONTROL_PHRASE));
		} finally {
			is.close();
		}
	}

	@Test
	@OperateOnDeployment(DEPLOYMENT)
	public void testGetList(@ArquillianResource final URL url) throws IOException {
		LOGGER.debug("* Get Containers: " + url);
		ILinkedDataPlatformContainerManager server = TestingUtil.createServiceClient(url,ILinkedDataPlatformContainerManager.class);
		Response response = server.getContainerIdentifiers();
		assertThat(response,notNullValue());
		LOGGER.debug("\t- Status.....: " + response.getStatus());
		LOGGER.debug("\t- Location...: " + response.getMetadata().get("Location"));
		assertThat(response.getStatus(),equalTo(Status.OK.getStatusCode()));
		String body = IOUtils.readStringFromStream((InputStream)response.getEntity());
		LOGGER.debug("\t- Body.......: " + body);
		assertThat(body,containsString(WorkingContainer.CONTAINER_ID));
		assertThat(body,containsString(FailingContainer.CONTAINER_ID));
	}

	@Test
	@OperateOnDeployment(DEPLOYMENT)
	public void testCreateResource$validContents(@ArquillianResource final URL url) throws IOException {
		String expectedLocation = url.toString().concat("ldp/resources/WorkingContainer/"+Integer.toHexString("example".hashCode()));
		LOGGER.debug("* Create resource: " + url);
		ILinkedDataPlatformContainerManager server = TestingUtil.createServiceClient(url,ILinkedDataPlatformContainerManager.class);
		Response response = server.createResource(WorkingContainer.CONTAINER_ID, "example",Format.Turtle.getMime());
		assertThat(response,notNullValue());
		LOGGER.debug("\t- Status.....: " + response.getStatus());
		assertThat(response.getStatus(),equalTo(Status.CREATED.getStatusCode()));
		String body = IOUtils.readStringFromStream((InputStream)response.getEntity());
		LOGGER.debug("\t- Body.......: " + body);
		assertThat(body,equalTo(expectedLocation));
		LOGGER.debug("\t- Location...: " + response.getMetadata().get("Location"));
		assertThat(response.getMetadata().get("Location"),hasItem((Object)expectedLocation));
	}

	@Test
	@OperateOnDeployment(DEPLOYMENT)
	public void testCreateResource$invalidContents(@ArquillianResource final URL url) throws IOException {
		LOGGER.debug("* Create resource: " + url);
		ILinkedDataPlatformContainerManager server = TestingUtil.createServiceClient(url,ILinkedDataPlatformContainerManager.class);
		Response response = server.createResource(FailingContainer.CONTAINER_ID, FailingContainer.INVALID_CONTENT,Format.Turtle.getMime());
		assertThat(response,notNullValue());
		LOGGER.debug("\t- Status.....: " + response.getStatus());
		assertThat(response.getStatus(),equalTo(Status.CONFLICT.getStatusCode()));
		String body = IOUtils.readStringFromStream((InputStream)response.getEntity());
		LOGGER.debug("\t- Body.......: " + body);
		assertThat(body,notNullValue());
		LOGGER.debug("\t- Link.......: " + response.getMetadata().get("Link"));
		assertThat(response.getMetadata().get("Link"),hasItem(LinkUtil.create(url.toString()+"ldp/containers/"+FailingContainer.CONTAINER_ID+"?describedBy", "describedBy")));
	}

	@Test
	@OperateOnDeployment(DEPLOYMENT)
	public void testSearch(@ArquillianResource final URL url) throws IOException {
		LOGGER.debug("* Search container: " + url);
		ILinkedDataPlatformContainerManager server = TestingUtil.createServiceClient(url,ILinkedDataPlatformContainerManager.class);
		Response response = server.search(null,WorkingContainer.CONTAINER_ID, Format.Turtle.getMime());
		assertThat(response,notNullValue());
		LOGGER.debug("\t- Status.....: " + response.getStatus());
		assertThat(response.getStatus(),equalTo(Status.OK.getStatusCode()));
		String body = IOUtils.readStringFromStream((InputStream)response.getEntity());
		LOGGER.debug("\t- Body.......: " + body);
	}

}