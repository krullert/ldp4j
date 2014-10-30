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
package org.ldp4j.application.entity;

import java.net.URI;

import org.junit.Test;
import org.ldp4j.application.domain.RDFS;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class DataSourceUtilTest {

	@Test
		public void testFormatDataSource() {
			DataSource dataSource=DataSource.create();
	
			URI ind1 = URI.create("http://www.ldp4j.org/ind1/");
			Entity ent1 = new Entity(ind1);
			ent1.addProperty(RDFS.LABEL.as(URI.class), Literal.create("Individual 1 label"));
			ent1.addProperty(RDFS.COMMENT.as(URI.class), Literal.create("Comment 1"));
	
			Entity ent2 = new Entity(DataSource.class,123);
			ent2.addProperty(RDFS.LABEL.as(URI.class), Literal.create("Individual 2 label"));
			ent2.addProperty(RDFS.COMMENT.as(URI.class), Literal.create("Comment 2"));
	
			Entity ent3 = new Entity(dataSource);
			ent3.addProperty(RDFS.LABEL.as(URI.class), Literal.create("Individual 3 label"));
			ent3.addProperty(RDFS.COMMENT.as(URI.class), Literal.create("Comment 3"));
	
			URI linkedTo = URI.create("http://www.example.org/vocab#linkedTo");
			ent2.addProperty(linkedTo, ent3);
			ent1.addProperty(linkedTo, ent2);
	
			dataSource.add(ent1);
	
			Property property = ent2.getProperty(linkedTo);
	
			Value value = property.iterator().next();
			assertThat(value,is(instanceOf(Entity.class)));
			Entity sEnt3 = (Entity)value;
			assertThat(sEnt3.identity(),equalTo(ent3.identity()));
			assertThat(sEnt3.identifier(),equalTo(ent3.identifier()));
			assertThat(sEnt3.dataSource(),equalTo(ent3.dataSource()));
	
			System.out.println(DataSourceUtil.formatDataSource(dataSource));
			ent3.addProperty(linkedTo, ent1);
			sEnt3.addProperty(linkedTo, ent2);
			System.out.println(DataSourceUtil.formatDataSource(dataSource));
		}

}