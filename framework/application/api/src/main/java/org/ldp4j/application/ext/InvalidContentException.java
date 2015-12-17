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
 *   Artifact    : org.ldp4j.framework:ldp4j-application-api:0.2.0
 *   Bundle      : ldp4j-application-api-0.2.0.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.application.ext;

import java.util.concurrent.atomic.AtomicReference;

import org.ldp4j.application.data.constraints.Constraints;

/**
 * This exception may be thrown by an LDP4j Application if the data to be used
 * in an operation is not valid.
 */
public class InvalidContentException extends ApplicationUsageException {

	private static final long serialVersionUID = 1090034112299823596L;
	private final Constraints constraints;

	private final AtomicReference<String> id;

	/**
	 * Create a new instance with a message, a cause, and constraints.
	 *
	 * @param message
	 *            the description of the failure.
	 * @param cause
	 *            the underlying cause of the failure.
	 * @param constraints
	 *            the constraints that are not satisfied by the input data.
	 */
	public InvalidContentException(String message, Throwable cause, Constraints constraints) {
		super(message, cause);
		this.constraints = constraints;
		this.id=new AtomicReference<String>(null);
	}

	/**
	 * Create a new instance with a message and constraints.
	 *
	 * @param message
	 *            the description of the failure.
	 * @param constraints
	 *            the constraints that are not satisfied by the input data.
	 */
	public InvalidContentException(String message, Constraints constraints) {
		this(message,null,constraints);
	}

	/**
	 * Create a new instance with a cause, constraints, and a default message.
	 *
	 * @param cause
	 *            the underlying cause of the failure.
	 * @param constraints
	 *            the constraints that are not satisfied by the input data.
	 */
	public InvalidContentException(Throwable cause, Constraints constraints) {
		this("Invalid content",cause,constraints);
	}

	/**
	 * Return the constaints that were not satisfied by the input data.
	 *
	 * @return the unsatisfied constraints
	 */
	public Constraints getConstraints() {
		return constraints;
	}

	/**
	 * Enrich the exception with the identifier of the report that will be used
	 * to acknowledge the users about the constraint failure.
	 *
	 * @param id
	 *            the identifier of the report.
	 */
	public final void setConstraintsId(String id) {
		this.id.set(id);
	}

	/**
	 * Return the identifier of the report that will be used to acknowledge the
	 * users about the failed constraints.
	 *
	 * @return the identifier of the constraint failure report.
	 */
	public final String getConstraintsId() {
		return this.id.get();
	}

}