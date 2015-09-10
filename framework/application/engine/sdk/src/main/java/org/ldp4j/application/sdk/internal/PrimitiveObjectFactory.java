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
 *   Artifact    : org.ldp4j.framework:ldp4j-application-engine-sdk:0.2.0-SNAPSHOT
 *   Bundle      : ldp4j-application-engine-sdk-0.2.0-SNAPSHOT.jar
 * #-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=#
 */
package org.ldp4j.application.sdk.internal;

import static com.google.common.base.Preconditions.checkArgument;

import org.ldp4j.application.sdk.spi.ObjectFactory;
import org.ldp4j.application.sdk.spi.ObjectParseException;

public final class PrimitiveObjectFactory<T> implements ObjectFactory<T> {

	private final Class<? extends T> valueClass;

	private PrimitiveObjectFactory(Class<? extends T> valueClass) {
		this.valueClass = valueClass;
	}

	@Override
	public Class<? extends T> targetClass() {
		return this.valueClass;
	}

	@Override
	@SuppressWarnings("unchecked")
	public T fromString(String rawValue) {
		try {
			Object result=null;
			if (byte.class.equals(valueClass)) {
				result=Byte.decode(rawValue);
			} else if(short.class.equals(valueClass)) {
				result=Short.decode(rawValue);
			} else if(int.class.equals(valueClass)) {
				result=Integer.decode(rawValue);
			} else if (long.class.equals(valueClass)) {
				result=Long.decode(rawValue);
			} else if (double.class.equals(valueClass)) {
				result=Double.valueOf(rawValue);
			} else if (float.class.equals(valueClass)) {
				result=Float.valueOf(rawValue);
			} else if (boolean.class.equals(valueClass)) {
				result=Boolean.valueOf(rawValue);
			} else { // Must be char
				if(rawValue.length()!=1) {
					throw new IllegalArgumentException("Raw value has more than one character");
				}
				result=Character.valueOf(rawValue.charAt(0));
			}
			return (T)result;
		} catch(Exception e) {
			throw new ObjectParseException(e,valueClass,rawValue);
		}
	}

	@Override
	public String toString(T value) {
		return value.toString();
	}

	public static <T> PrimitiveObjectFactory<T> create(Class<? extends T> valueClass) {
		checkArgument(valueClass.isPrimitive(),"Class '"+valueClass.getName()+"' is not primitive");
		return new PrimitiveObjectFactory<T>(valueClass);
	}
}