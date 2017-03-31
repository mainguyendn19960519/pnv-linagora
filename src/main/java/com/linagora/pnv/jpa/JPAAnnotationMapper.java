/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package com.linagora.pnv.jpa;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceException;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.linagora.pnv.AnnotationMapper;
import com.linagora.pnv.MailboxAnnotation;
import com.linagora.pnv.MailboxAnnotationKey;
import com.linagora.pnv.MailboxException;
import com.linagora.pnv.MailboxId;

public class JPAAnnotationMapper implements AnnotationMapper {

	public static final Function<JPAMailboxAnnotation, MailboxAnnotation> READ_ROW = 
			jpaMailboxAnnotation -> MailboxAnnotation.newInstance(
					new MailboxAnnotationKey(jpaMailboxAnnotation.getKey()), jpaMailboxAnnotation.getValue());

	private final EntityManager entityManager;

	public JPAAnnotationMapper(EntityManagerFactory entityManagerFactory) {
		this.entityManager = entityManagerFactory.createEntityManager();
	}

	@Override
	public void endRequest() {

	}

	@Override
	public <T> T execute(Transaction<T> transaction) throws MailboxException {
		return null;
	}

	@Override
	public List<MailboxAnnotation> getAllAnnotations(MailboxId mailboxId) {
		JPAId jpaId = (JPAId) mailboxId;
		return entityManager.createNamedQuery("retrieveAllAnnotations", JPAMailboxAnnotation.class)
				.setParameter("idParam", jpaId.getRawId()).getResultList().stream()
				.map(input -> MailboxAnnotation.newInstance(new MailboxAnnotationKey(input.getKey()), input.getValue()))
				.collect(Collectors.toList());
	}

	@Override
	public List<MailboxAnnotation> getAnnotationsByKeys(MailboxId mailboxId, Set<MailboxAnnotationKey> keys) {
		try {
			final JPAId jpaId = (JPAId) mailboxId;

			return keys.stream().map(
					key -> READ_ROW.apply(entityManager.createNamedQuery("retrieveByKey", JPAMailboxAnnotation.class)
							.setParameter("idParam", jpaId.getRawId())
							.setParameter("keyParam", key.asString())
							.getSingleResult()))
							.collect(Collectors.toList());
		} catch (NoResultException e) {
			return ImmutableList.of();
		}
	}

	@Override
	public List<MailboxAnnotation> getAnnotationsByKeysWithOneDepth(MailboxId mailboxId,
			Set<MailboxAnnotationKey> keys) {
		return getFilteredLikes((JPAId) mailboxId, keys, 
				key -> 
					input -> key.isParentOrIsEqual(input.getKey()));
	}

	@Override
	public List<MailboxAnnotation> getAnnotationsByKeysWithAllDepth(MailboxId mailboxId,
			Set<MailboxAnnotationKey> keys) {
		
		return getFilteredLikes((JPAId) mailboxId, keys, 
				key -> 
					input -> key.isAncestorOrIsEqual(input.getKey()));
	}

	private List<MailboxAnnotation> getFilteredLikes(final JPAId jpaId, Set<MailboxAnnotationKey> keys,
			final Function<MailboxAnnotationKey, Predicate<MailboxAnnotation>> predicateFunction) {
		try {
			return keys.stream()
					.flatMap(key -> entityManager.createNamedQuery("retrieveByKeyLike", JPAMailboxAnnotation.class)
							.setParameter("idParam", jpaId.getRawId())
							.setParameter("keyParam", key.asString() + '%')
							.getResultList().stream().map(jpaMailboxAnnotation -> READ_ROW.apply(jpaMailboxAnnotation))
							.filter(annotation -> predicateFunction.apply(key).apply(annotation)))
							.collect(Collectors.toList());

		} catch (NoResultException e) {
			return ImmutableList.of();
		}
	}

	@Override
	public void deleteAnnotation(MailboxId mailboxId, MailboxAnnotationKey key) {
		try {
			entityManager.getTransaction().begin();
			JPAId jpaId = (JPAId) mailboxId;
			JPAMailboxAnnotation jpaMailboxAnnotation = entityManager.find(JPAMailboxAnnotation.class,
					new JPAMailboxAnnotation.JPAMailboxAnnotationId(jpaId.getRawId(), key.asString()));
			entityManager.remove(jpaMailboxAnnotation);
			entityManager.getTransaction().commit();
		} catch (NoResultException e) {

		} catch (PersistenceException pe) {
			throw Throwables.propagate(pe);
		}
	}

	@Override
	public void insertAnnotation(MailboxId mailboxId, MailboxAnnotation mailboxAnnotation) {
		Preconditions.checkArgument(!mailboxAnnotation.isNil());
		JPAId jpaId = (JPAId) mailboxId;
		entityManager.getTransaction().begin();
		JPAMailboxAnnotation jpaMailboxAnnotation = entityManager.find(JPAMailboxAnnotation.class,
				new JPAMailboxAnnotation.JPAMailboxAnnotationId(jpaId.getRawId(),
						mailboxAnnotation.getKey().asString()));
		if (jpaMailboxAnnotation == null) {
			entityManager.persist(new JPAMailboxAnnotation(jpaId.getRawId(), mailboxAnnotation.getKey().asString(),
					mailboxAnnotation.getValue().orNull()));
		} else {
			jpaMailboxAnnotation.setValue(mailboxAnnotation.getValue().orNull());
		}
		entityManager.getTransaction().commit();
	}

	@Override
	public boolean exist(MailboxId mailboxId, MailboxAnnotation mailboxAnnotation) {
		JPAId jpaId = (JPAId) mailboxId;
		Optional<JPAMailboxAnnotation> row = Optional.fromNullable(
				entityManager.find(JPAMailboxAnnotation.class, new JPAMailboxAnnotation.JPAMailboxAnnotationId(
						jpaId.getRawId(), mailboxAnnotation.getKey().asString())));
		return row.isPresent();
	}

	@Override
	public int countAnnotations(MailboxId mailboxId) {
		try {
			JPAId jpaId = (JPAId) mailboxId;
			return ((Long) entityManager.createNamedQuery("countAnnotationsInMailbox")
					.setParameter("idParam", jpaId.getRawId()).getSingleResult()).intValue();
		} catch (PersistenceException pe) {
			throw Throwables.propagate(pe);
		}
	}
}
