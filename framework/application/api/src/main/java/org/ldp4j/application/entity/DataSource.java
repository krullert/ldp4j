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

import static com.google.common.base.Preconditions.checkNotNull;

import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class DataSource implements Iterable<Entity> {

	private final UUID identifier;
	private final Lock write;
	private final Lock read;
	private final Map<Identity,CompositeManagedEntity> entities;
	private final Map<UUID,Identity> entityIdentifiers;
	private final MergeStrategy mergeStrategy;

	private DataSource(UUID identifier, MergeStrategy mergeStrategy) {
		this.identifier = identifier;
		this.mergeStrategy = mergeStrategy;
		ReadWriteLock lock = new ReentrantReadWriteLock();
		this.write = lock.writeLock();
		this.read = lock.readLock();
		this.entities=Maps.newLinkedHashMap();
		this.entityIdentifiers=Maps.newLinkedHashMap();
	}

	private void unsafeRegisterEntity(CompositeManagedEntity newEntity) {
		newEntity.attach(this.nextEntityId(), this);
		this.entities.put(newEntity.identity(),newEntity);
		this.entityIdentifiers.put(newEntity.id(),newEntity.identity());
	}

	private UUID nextEntityId() {
		UUID identifier=null;
		do {
			identifier=UUID.randomUUID();
		} while(this.entityIdentifiers.containsKey(identifier));
		return identifier;
	}

	private CompositeManagedEntity deepMerge(Entity entity) {
		final Queue<Entity> pending=new LinkedList<Entity>();
		final List<Entity> merged=Lists.<Entity>newArrayList();
		Entity first=entity;
		while(first!=null) {
			if(!contains(first)) {
				CompositeManagedEntity surrogate=newEntity(first.identity());
				for(Property property:first) {
					mergeLiteralValues(surrogate, property);
					mergeEntityValues(surrogate, property, pending);
				}
				merged.add(first);
			}
			first=pending.poll();
		}
		return this.entities.get(entity.identity());
	}

	private void mergeLiteralValues(CompositeManagedEntity target, Property property) {
		URI predicate=property.predicate();
		for(Literal<?> value:property.literalValues()) {
			target.addProperty(predicate, value);
		}
	}

	private void mergeEntityValues(CompositeManagedEntity target, Property property, Queue<Entity> pendingMerges) {
		URI predicate=property.predicate();
		for(Entity value:property.entityValues()) {
			if(contains(value)) {
				target.addProperty(predicate,value);
			} else {
				CompositeManagedEntity valueSurrogate=newEntity(value.identity());
				target.addProperty(predicate, valueSurrogate);
				if(!pendingMerges.contains(value)) {
					pendingMerges.offer(value);
				}
			}
		}
	}

	// Implementation API

	private CompositeManagedEntity getOrCreate(Identity identity) {
		CompositeManagedEntity result=this.entities.get(identity);
		if(result==null) {
			result=CompositeManagedEntity.create(DefaultManagedEntity.create(identity));
			unsafeRegisterEntity(result);
		}
		return result;
	}
	private CompositeManagedEntity attachEntity(Entity entity) {
		CompositeManagedEntity target=findByIdentity(entity.identity());
		if(target==null) {
			target=newEntity(entity.identity());
		} else if(target!=entity) {
			target.join(CompositeManagedEntity.wrap(entity));
		}
		return target;
	}

	public UUID id() {
		return this.identifier;
	}

	/**
	 * Find the entity managed by the data source that has the specified identifier.
	 * If the data source does not manage an entity with such identity, null
	 * be returned.
	 *
	 * @param identity
	 * @return
	 */
	public CompositeManagedEntity findById(UUID id) {
		checkNotNull(id,"Entity identifier cannot be null");
		this.read.lock();
		try {
			Identity identity = this.entityIdentifiers.get(id);
			if(identity==null) {
				return null;
			}
			return this.entities.get(identity);
		} finally {
			this.read.unlock();
		}
	}

	/**
	 * Find the entity managed by the data source that has the specified identity.
	 * If the data source does not manage an entity with such identity, null
	 * be returned.
	 *
	 * @param identity
	 * @return
	 */
	public CompositeManagedEntity findByIdentity(Identity identity) {
		checkNotNull(identity,"Entity identity cannot be null");
		this.read.lock();
		try {
			return this.entities.get(identity);
		} finally {
			this.read.unlock();
		}
	}

	/**
	 * Create a new entity managed by the data source that has the specified
	 * identity. If the data source does not manage an entity with such
	 * identity, null entity will be returned.
	 *
	 * @param identity
	 *            The identity for the new entity
	 * @return The entity managed by the data source that has the specified
	 *         identity
	 * @throws IllegalArgumentException
	 *             if the data source already manages an entity with the
	 *             specified identity
	 */
	public CompositeManagedEntity newEntity(Identity identity) {
		this.write.lock();
		try {
			if(findByIdentity(identity)!=null) {
				throw new IllegalArgumentException("An entity with the same identity is already managed by the datasource");
			}
			// TODO: Instead of relying on a particular implementation class,
			// delegate on an EntityFactory (which should be configurable as it
			// is the IdentifierGenerator for the EntityFactory)
			CompositeManagedEntity newEntity = CompositeManagedEntity.create(new DefaultManagedEntity(identity));
			unsafeRegisterEntity(newEntity);
			return newEntity;
		} finally {
			this.write.unlock();
		}
	}

	public boolean contains(Entity entity) {
		boolean result=false;
		if(entity!=null) {
			result=findByIdentity(entity.identity())==entity;
		}
		return result;
	}

	/**
	 * Remove the entity from the data source. If the entity is not managed by
	 * the data source nothing happens. In other case, the removal cascades to
	 * the removal of the relationships to the entity with other managed
	 * entities.
	 *
	 * @param entity
	 */
	public void remove(Entity entity) {
		checkNotNull(entity,"Entity cannot be null");
		this.write.lock();
		try {
			if(!contains(entity)) {
				return;
			}
			CompositeManagedEntity removedEntity = this.entities.remove(entity.identity());
			assert removedEntity==entity;
			removedEntity.dettach(this);
			this.entityIdentifiers.remove(removedEntity.id());
			for(CompositeManagedEntity tmp:this.entities.values()) {
				tmp.removeProperties(removedEntity);
			}
		} finally {
			this.write.unlock();
		}
	}

	/**
	 * Merge the state of the given entity into the current data source.
	 *
	 * @param entity
	 *            Entity instance
	 * @return the input entity if it was already managed by the data source or
	 *         the entity managed by data source to which the input entity state
	 *         was merged to otherwise
	 */
	public CompositeManagedEntity merge(Entity entity) {
		checkNotNull(entity,"Entity cannot be null");
		this.write.lock();
		try {
			CompositeManagedEntity result=null;
			switch(this.mergeStrategy) {
			case BY_IDENTITY:
				result=getOrCreate(entity.identity());
				break;
			case BY_REFERENCE:
				result=attachEntity(entity);
				break;
			case BY_VALUE:
				result=deepMerge(entity);
				break;
			default:
				throw new IllegalStateException("Unsupported linking mechanism");
			}
			return result;
		} finally {
			this.write.unlock();
		}
	}

	@Override
	public Iterator<Entity> iterator() {
		this.read.lock();
		try {
			return
				ImmutableList.
					<Entity>copyOf(this.entities.values()).
						iterator();
		} finally {
			this.read.unlock();
		}
	}

	public static DataSource create() {
		return create(MergeStrategy.BY_VALUE);
	}

	public static DataSource create(MergeStrategy mergeStrategy) {
		return new DataSource(UUID.randomUUID(),mergeStrategy);
	}

}