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
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.h2.command.ddl.TruncateTable;
import org.junit.After;
import com.linagora.pnv.GenericMaxQuotaManagerTest;
import com.linagora.pnv.MaxQuotaManager;

public class JPAPerUserMaxQuotaTest extends GenericMaxQuotaManagerTest {

    @Override
    protected MaxQuotaManager provideMaxQuotaManager() {
        return new JPAPerUserMaxQuotaManager(Persistence.createEntityManagerFactory("global"));
    }

    @After
    public void cleanUp() {
    	EntityManager entityManager = Persistence.createEntityManagerFactory("global").createEntityManager();
    	
    	entityManager.getTransaction().begin();    	
    	entityManager.createNativeQuery("TRUNCATE TABLE JAMES_MAXDEFAULTMESSAGECOUNT").executeUpdate();
    	entityManager.createNativeQuery("TRUNCATE TABLE JAMES_MAXDEFAULTSTORAGE").executeUpdate();
    	entityManager.createNativeQuery("TRUNCATE TABLE JAMES_MAXDUSERMESSAGECOUNT").executeUpdate();
    	entityManager.createNativeQuery("TRUNCATE TABLE JAMES_MAXUSERSTORAGE").executeUpdate();	
    	entityManager.getTransaction().commit();
    	entityManager.clear();
    	
    }
}	
