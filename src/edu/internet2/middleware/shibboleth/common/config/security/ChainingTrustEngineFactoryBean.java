/*
 * Copyright [2007] [University Corporation for Advanced Internet Development, Inc.]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.internet2.middleware.shibboleth.common.config.security;

import java.util.List;

import org.opensaml.xml.security.trust.ChainingTrustEngine;
import org.opensaml.xml.security.trust.TrustEngine;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Spring factory bean used to created {@link ChainingTrustEngine}s.
 */
public class ChainingTrustEngineFactoryBean extends AbstractFactoryBean {
    
    /** List of chain members. */
    private List<TrustEngine> chain;

    /**
     * Gets the chain member list.
     * 
     * @return chain member list
     */
    public List<TrustEngine> getChain() {
        return chain;
    }

    /**
     * Sets the chain member list.
     * 
     * @param newChain the new chain member list
     */
    public void setChain(List<TrustEngine> newChain) {
        chain = newChain;
    }

    /** {@inheritDoc} */
    public Class getObjectType() {
        return ChainingTrustEngine.class;
    }
    
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    protected Object createInstance() throws Exception {
        ChainingTrustEngine engine = new ChainingTrustEngine();
        if (chain != null) {
            engine.getChain().addAll(chain);
        }
        return engine;
    }
}