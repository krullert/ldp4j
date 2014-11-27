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
 *   Artifact    : org.ldp4j.framework:ldp4j-application-api:1.0.0-SNAPSHOT
 *   Bundle      : ldp4j-application-api-1.0.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.application.config.core;

import org.ldp4j.application.config.Configuration;
import org.ldp4j.application.config.ImmutableConfiguration;
import org.ldp4j.application.config.Setting;

public class DefaultImmutableConfiguration extends BaseConfiguration implements ImmutableConfiguration {

	/**
	 *
	 */
	private static final long serialVersionUID = -1969151343231953574L;

	protected DefaultImmutableConfiguration(DefaultImmutableConfiguration configuration) {
		super(configuration);
	}

	public DefaultImmutableConfiguration(Configuration config) {
		super(config);
	}

	public DefaultImmutableConfiguration() {
		super();
	}

	/**
	 * Create a new immutable configuration from the current configuration with
	 * a new value for the specified {@link Setting}. If the value is null, the
	 * setting is removed and the default will be used instead.
	 *
	 * @param setting
	 *            The setting to set a new value for.
	 * @param value
	 *            The value for the setting, or null to reset the setting to use
	 *            the default value.
	 * @return A copy of the configuration with the specified setting updated.
	 */
	@Override
	public <T> ImmutableConfiguration set(Setting<T> setting, T value) {
		DefaultImmutableConfiguration result=new DefaultImmutableConfiguration(this);
		result.update(setting,value);
		return result;
	}

}