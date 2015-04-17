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
 *   Artifact    : org.ldp4j.framework:ldp4j-application-core:1.0.0-SNAPSHOT
 *   Bundle      : ldp4j-application-core-1.0.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.application;

import static com.google.common.base.Preconditions.checkState;

import java.util.Date;

import org.ldp4j.application.constraints.ConstraintReport;
import org.ldp4j.application.constraints.ConstraintReportId;
import org.ldp4j.application.constraints.ConstraintReportTransformer;
import org.ldp4j.application.data.DataSet;
import org.ldp4j.application.data.ManagedIndividualId;
import org.ldp4j.application.endpoint.Endpoint;
import org.ldp4j.application.engine.ApplicationContextCreationException;
import org.ldp4j.application.engine.context.ApplicationContext;
import org.ldp4j.application.engine.context.ApplicationContextException;
import org.ldp4j.application.engine.context.ApplicationContextOperation;
import org.ldp4j.application.engine.context.ApplicationExecutionException;
import org.ldp4j.application.engine.context.Capabilities;
import org.ldp4j.application.engine.context.HttpRequest;
import org.ldp4j.application.engine.context.PublicResource;
import org.ldp4j.application.engine.lifecycle.ApplicationLifecycleListener;
import org.ldp4j.application.ext.Application;
import org.ldp4j.application.ext.Configuration;
import org.ldp4j.application.ext.Deletable;
import org.ldp4j.application.ext.InvalidContentException;
import org.ldp4j.application.ext.Modifiable;
import org.ldp4j.application.ext.ResourceHandler;
import org.ldp4j.application.resource.Container;
import org.ldp4j.application.resource.FeatureExecutionException;
import org.ldp4j.application.resource.Resource;
import org.ldp4j.application.resource.ResourceId;
import org.ldp4j.application.session.WriteSessionConfiguration;
import org.ldp4j.application.spi.Transaction;
import org.ldp4j.application.template.ResourceTemplate;
import org.ldp4j.application.template.TemplateIntrospector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DefaultApplicationContext implements ApplicationContext {

	private final class DefaultApplicationOperation implements ApplicationContextOperation {

		private final HttpRequest request;

		private DefaultApplicationOperation(HttpRequest request) {
			this.request = request;
			getContext().operationController.beginTransaction();
		}

		HttpRequest getRequest() {
			return this.request;
		}

		@Override
		public DefaultApplicationContext getContext() {
			return DefaultApplicationContext.this;
		}

		@Override
		public PublicResource findResource(String path) {
			return DefaultApplicationContext.this.findResource(path);
		}

		@Override
		public PublicResource resolveResource(String path) {
			return DefaultApplicationContext.this.resolveResource(path);
		}

		@Override
		public PublicResource resolveResource(ManagedIndividualId id) {
			return DefaultApplicationContext.this.resolveResource(id);
		}

		@Override
		public void dispose() {
			try {
				getContext().operationController.endTransaction();
			} finally {
				getContext().currentOperation.remove();
			}
		}

	}

	private final class ApplicationContextOperationController {

		private ApplicationContextOperationController() {
		}

		private Transaction currentTransaction() {
			return engine().persistencyManager().currentTransaction();
		}

		public void beginTransaction() {
			Transaction transaction = currentTransaction();
			transaction.begin();
			LOGGER.
				info("Started transaction {}.{},",
					Thread.currentThread().getName(),
					transaction);
		}

		public void endTransaction() {
			Transaction transaction = currentTransaction();
			if(!transaction.isCompleted() && transaction.isStarted()) {
				transaction.rollback();
			}
			LOGGER.
				info("Completed transaction {}.{},",
					Thread.currentThread().getName(),
					transaction);
		}

	}

	private static Logger LOGGER=LoggerFactory.getLogger(DefaultApplicationContext.class);

	private Application<Configuration> application;

	private final DefaultPublicResourceFactory factory;

	private final DefaultApplicationEngine engine;

	private final ApplicationContextOperationController operationController;

	private final ThreadLocal<DefaultApplicationOperation> currentOperation;

	DefaultApplicationContext(DefaultApplicationEngine engine) {
		this.engine=engine;
		this.factory=DefaultPublicResourceFactory.newInstance(this);
		this.operationController=new ApplicationContextOperationController();
		this.currentOperation=new ThreadLocal<DefaultApplicationOperation>();
	}

	private static <T> T checkNotNull(T object, String message) {
		if(object==null) {
			throw new ApplicationContextException(message);
		}
		return object;
	}

	private String applicationFailureMessage(String message, Object... objects) {
		return "[" + this.application.getName() + "] " + String.format(message,objects);
	}

	private Application<Configuration> application() {
		return this.application;
	}

	private ApplicationExecutionException createException(String errorMessage, Exception e) {
		LOGGER.error(errorMessage,e);
		if(e instanceof FeatureExecutionException) {
			return new ApplicationExecutionException(errorMessage,e.getCause());
		}
		throw new ApplicationContextException(errorMessage,e);
	}

	private PublicResource findResource(final String path) {
		return resolveResource(path);
	}

	private PublicResource resolveResource(final String path) {
		checkNotNull(path,"Endpoint path cannot be null");
		Endpoint endpoint=
				engine().
					endpointManagementService().
						resolveEndpoint(path);
		return this.factory.createResource(endpoint);
	}

	private PublicResource resolveResource(ManagedIndividualId id) {
		checkNotNull(id,"Individual identifier cannot be null");
		return this.factory.createResource(ResourceId.createId(id.name(), id.managerId()));
	}

	private void processConstraintValidationFailure(Resource resource, Throwable failure) {
		if(failure.getCause() instanceof InvalidContentException) {
			InvalidContentException cause=(InvalidContentException)failure.getCause();
			registerConstraintReport(resource, cause);
		}
	}

	private void registerConstraintReport(Resource resource, InvalidContentException error) {
		ConstraintReport report=
			this.engine().
				persistencyManager().
					createConstraintReport(
						resource,
						error.getConstraints(),
						new Date(),
						currentRequest());
		this.engine().persistencyManager().add(report);
		LOGGER.debug("Constraint validation failed. Registered constraint report {}",report.id());
		error.setConstraintsId(report.id().constraintsId());
	}

	private HttpRequest currentRequest() {
		DefaultApplicationOperation result = this.currentOperation.get();
		checkState(result!=null,"No in-flight operation");
		return result.getRequest();
	}

	DataSet getResource(Endpoint endpoint) throws ApplicationExecutionException {
		ResourceId resourceId=endpoint.resourceId();
		Resource resource = this.engine().persistencyManager().resourceOfId(resourceId,Resource.class);
		if(resource==null) {
			String errorMessage = applicationFailureMessage("Could not find resource for endpoint '%s'",endpoint);
			LOGGER.error(errorMessage);
			throw new ApplicationExecutionException(errorMessage);
		}
		try {
			return this.engine().resourceControllerService().getResource(resource);
		} catch (Exception e) {
			String errorMessage = applicationFailureMessage("Resource '%s' retrieval failed ",endpoint);
			throw createException(errorMessage,e);
		}
	}

	void registerContentFailure(Endpoint endpoint, InvalidContentException error) {
		ResourceId resourceId=endpoint.resourceId();
		registerConstraintReport(this.engine().persistencyManager().resourceOfId(resourceId),error);
	}

	Resource resolveResource(Endpoint endpoint) {
		return this.engine().persistencyManager().resourceOfId(endpoint.resourceId(), Resource.class);
	}

	Endpoint resolveResource(ResourceId id) {
		return this.engine().persistencyManager().endpointOfResource(id);
	}

	Resource createResource(Endpoint endpoint, DataSet dataSet, String desiredPath) throws ApplicationExecutionException {
		ResourceId resourceId=endpoint.resourceId();
		Container resource = this.engine().persistencyManager().resourceOfId(resourceId,Container.class);
		if(resource==null) {
			String errorMessage = applicationFailureMessage("Could not find container for endpoint '%s'",endpoint);
			LOGGER.error(errorMessage);
			throw new ApplicationExecutionException(errorMessage);
		}
		try {
			return this.engine().resourceControllerService().createResource(resource,dataSet,desiredPath);
		} catch (FeatureExecutionException e) {
			processConstraintValidationFailure(resource, e);
			String errorMessage = applicationFailureMessage("Resource create failed at '%s'",endpoint);
			throw createException(errorMessage,e);
		} catch (Exception e) {
			String errorMessage = applicationFailureMessage("Resource create failed at '%s'",endpoint);
			throw createException(errorMessage,e);
		}
	}

	void deleteResource(Endpoint endpoint) throws ApplicationExecutionException {
		ResourceId resourceId=endpoint.resourceId();
		Resource resource = this.engine().persistencyManager().resourceOfId(resourceId,Resource.class);
		if(resource==null) {
			String errorMessage = applicationFailureMessage("Could not find container for endpoint '%s'",endpoint);
			LOGGER.error(errorMessage);
			throw new ApplicationExecutionException(errorMessage);
		}
		try {
			this.engine().resourceControllerService().deleteResource(resource, WriteSessionConfiguration.builder().build());
		} catch (Exception e) {
			String errorMessage = applicationFailureMessage("Resource deletion failed at '%s'",endpoint);
			throw createException(errorMessage,e);
		}
	}

	void modifyResource(Endpoint endpoint, DataSet dataSet) throws ApplicationExecutionException {
		ResourceId resourceId=endpoint.resourceId();
		Resource resource = this.engine().persistencyManager().resourceOfId(resourceId,Resource.class);
		if(resource==null) {
			String errorMessage = applicationFailureMessage("Could not find resource for endpoint '%s'",endpoint);
			LOGGER.error(errorMessage);
			throw new ApplicationExecutionException(errorMessage);
		}
		try {
			this.engine().resourceControllerService().updateResource(resource,dataSet, WriteSessionConfiguration.builder().build());
		} catch (FeatureExecutionException e) {
			processConstraintValidationFailure(resource, e);
			String errorMessage = applicationFailureMessage("Resource modification failed at '%s'",endpoint);
			throw createException(errorMessage,e);
		} catch (Exception e) {
			String errorMessage = applicationFailureMessage("Resource modification failed at '%s'",endpoint);
			throw createException(errorMessage,e);
		}
	}

	DataSet getConstraintReport(Endpoint endpoint, String constraintsId) throws ApplicationExecutionException {
		ResourceId resourceId=endpoint.resourceId();
		Resource resource = this.engine().persistencyManager().resourceOfId(resourceId,Resource.class);
		if(resource==null) {
			String errorMessage = applicationFailureMessage("Could not find resource for endpoint '%s'",endpoint);
			LOGGER.error(errorMessage);
			throw new ApplicationExecutionException(errorMessage);
		}

		ConstraintReport report=
			this.engine().
				persistencyManager().
					constraintReportOfId(
						ConstraintReportId.
							create(resource.id(),constraintsId));
		if(report==null) {
			return null;
		}

		return
			ConstraintReportTransformer.
				create(resource, report).
					transform(endpoint);
	}


	Capabilities endpointCapabilities(Endpoint endpoint) {
		MutableCapabilities result=new MutableCapabilities();
		Resource resource = resolveResource(endpoint);
		ResourceTemplate template=resourceTemplate(resource);
		Class<? extends ResourceHandler> handlerClass = template.handlerClass();
		result.setModifiable(Modifiable.class.isAssignableFrom(handlerClass));
		result.setDeletable(Deletable.class.isAssignableFrom(handlerClass) && !resource.isRoot());
		// TODO: Analyze how to provide patch support
		result.setPatchable(false);
		TemplateIntrospector introspector = TemplateIntrospector.newInstance(template);
		result.setFactory(introspector.isContainer());
		return result;
	}

	ResourceTemplate resourceTemplate(Resource resource) {
		return this.engine().persistencyManager().templateOfId(resource.id().templateId());
	}

	void initialize(String applicationClassName) throws ApplicationContextCreationException {
		try {
			this.application = this.engine().applicationLifecycleService().initialize(applicationClassName);
		} catch (ApplicationContextCreationException e) {
			String errorMessage = "Application '"+applicationClassName+"' initilization failed";
			LOGGER.error(errorMessage,e);
			throw e;
		}
	}

	boolean shutdown() {
		return true;
	}

	DefaultApplicationEngine engine() {
		return this.engine;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String applicationName() {
		return application().getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String applicationClassName() {
		return this.application.getClass().getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ApplicationContextOperation createOperation(HttpRequest request) {
		checkNotNull(request,"Http request cannot be null");
		DefaultApplicationOperation operation=this.currentOperation.get();
		checkState(operation==null,"An operation is ongoing on the current thread");
		operation=new DefaultApplicationOperation(request);
		this.currentOperation.set(operation);
		return operation;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerApplicationLifecycleListener(ApplicationLifecycleListener listener) {
		checkNotNull(listener,"Application lifecycle listener cannot be null");
		this.engine().applicationLifecycleService().registerApplicationLifecycleListener(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deregisterApplicationLifecycleListener(ApplicationLifecycleListener listener) {
		checkNotNull(listener,"Application lifecycle listener cannot be null");
		this.engine().applicationLifecycleService().deregisterApplicationLifecycleListener(listener);
	}

}
