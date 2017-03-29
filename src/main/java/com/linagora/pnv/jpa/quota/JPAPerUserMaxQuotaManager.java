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

package com.linagora.pnv.jpa.quota;

import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang.NotImplementedException;

import com.linagora.pnv.MailboxException;
import com.linagora.pnv.MaxQuotaManager;
import com.linagora.pnv.Quota;
import com.linagora.pnv.QuotaRoot;
import com.linagora.pnv.jpa.quota.model.MaxDefaultMessageCount;
import com.linagora.pnv.jpa.quota.model.MaxDefaultStorage;
import com.linagora.pnv.jpa.quota.model.MaxUserMessageCount;
import com.linagora.pnv.jpa.quota.model.MaxUserStorage;


public class JPAPerUserMaxQuotaManager implements MaxQuotaManager {

    private final EntityManager entityManager;

    public JPAPerUserMaxQuotaManager(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
    }

    @Override
    public void setMaxStorage(QuotaRoot quotaRoot, long maxStorageQuota) throws MailboxException {  	
    	entityManager.getTransaction().begin();
    	entityManager.merge(new MaxUserStorage(quotaRoot.getValue(), maxStorageQuota));
    	entityManager.getTransaction().commit();
    }

    @Override
    public void setMaxMessage(QuotaRoot quotaRoot, long maxMessageCount) throws MailboxException {
    	entityManager.getTransaction().begin();
    	entityManager.merge(new MaxUserMessageCount(quotaRoot.getValue(), maxMessageCount));
    	entityManager.getTransaction().commit();
    }

    @Override
    public void setDefaultMaxStorage(long defaultMaxStorage) throws MailboxException {
    	entityManager.getTransaction().begin();
    	entityManager.merge(new MaxDefaultStorage(defaultMaxStorage));
    	entityManager.getTransaction().commit();
    }

    @Override
    public void setDefaultMaxMessage(long defaultMaxMessageCount) throws MailboxException {
    	entityManager.getTransaction().begin();
    	entityManager.merge(new MaxDefaultMessageCount(defaultMaxMessageCount));
    	entityManager.getTransaction().commit();
    }

    @Override
    public long getDefaultMaxStorage() throws MailboxException {
    	MaxDefaultStorage storedValue = entityManager.find(MaxDefaultStorage.class, MaxDefaultStorage.DEFAULT_KEY);
    	
    	return Optional.ofNullable(storedValue)
    			.map(value -> value.getValue())
    			.orElse(Quota.UNLIMITED);
    }

    @Override
    public long getDefaultMaxMessage() throws MailboxException {
    	MaxDefaultMessageCount storedValue = entityManager.find(MaxDefaultMessageCount.class, MaxDefaultMessageCount.DEFAULT_KEY);
    	
    	return Optional.ofNullable(storedValue)
    			.map(value -> value.getValue())
    			.orElse(Quota.UNLIMITED);
    }

    @Override
    public long getMaxStorage(QuotaRoot quotaRoot) throws MailboxException {   	
    	MaxUserStorage storedValue = entityManager.find(MaxUserStorage.class, quotaRoot.getValue());
    	
    	return Optional.ofNullable(storedValue)
    			.map(value -> value.getValue())
    			.orElse(getDefaultMaxStorage());
    }

    @Override
    public long getMaxMessage(QuotaRoot quotaRoot) throws MailboxException {
        MaxUserMessageCount storedValue = entityManager.find(MaxUserMessageCount.class, quotaRoot.getValue());
        
        return Optional.ofNullable(storedValue)
    			.map(value -> value.getValue())
    			.orElse(getDefaultMaxMessage());
    }
}
