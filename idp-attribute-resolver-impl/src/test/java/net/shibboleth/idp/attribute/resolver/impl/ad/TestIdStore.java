/*
 * Licensed to the University Corporation for Advanced Internet Development, 
 * Inc. (UCAID) under one or more contributor license agreements.  See the 
 * NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The UCAID licenses this file to You under the Apache 
 * License, Version 2.0 (the "License"); you may not use this file except in 
 * compliance with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.attribute.resolver.impl.ad;

import java.util.Map;

import javax.annotation.Nullable;

import net.shibboleth.idp.persistence.PersistenceManager;
import net.shibboleth.utilities.java.support.component.ComponentValidationException;

import org.apache.commons.collections.map.HashedMap;
import org.testng.Assert;

public class TestIdStore implements PersistenceManager<TransientIdEntry> {

    private TransientIdEntry lastValueAdded;

    private String lastIdAdded;

    private Map<String, TransientIdEntry> theMap;

    protected TestIdStore() {
        theMap = new HashedMap();
    }

    protected TransientIdEntry getLastValue() {
        return lastValueAdded;
    }

    protected String getLastId() {
        return lastIdAdded;
    }

    /** {@inheritDoc} */
    @Nullable public String getId() {
        return "TestStore";
    }

    /** {@inheritDoc} */
    public void validate() throws ComponentValidationException {
        throw new ComponentValidationException();
    }

    /** {@inheritDoc} */
    public boolean contains(String id) {
        return theMap.containsKey(id);
    }

    /** {@inheritDoc} */
    public boolean contains(TransientIdEntry item) {
        return theMap.containsValue(item);
    }

    /** {@inheritDoc} */
    public TransientIdEntry get(String id) {
        return theMap.get(id);
    }

    /** {@inheritDoc} */
    public TransientIdEntry persist(String id, TransientIdEntry item) {
        lastValueAdded = item;
        lastIdAdded = id;
        theMap.put(id, item);
        return item;
    }

    /** {@inheritDoc} */
    public TransientIdEntry remove(String id) {
        return theMap.remove(id);
    }

    /** {@inheritDoc} */
    public TransientIdEntry remove(TransientIdEntry item) {
        Assert.fail("Not implemented");
        return item;
    }
}