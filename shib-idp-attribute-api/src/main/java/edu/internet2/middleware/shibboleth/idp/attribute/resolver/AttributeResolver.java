/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.attribute.resolver;

import java.util.List;
import java.util.Timer;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.resource.Resource;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.idp.service.AbstractSpringReloadableService;
import edu.internet2.middleware.shibboleth.idp.service.ServiceException;

//TODO perf metrics

/** A service that resolves the attributes for a particular subject. */
@ThreadSafe
public class AttributeResolver extends AbstractSpringReloadableService {
    
    

    /**
     * Constructor.
     * 
     * @param id the unique ID for this service
     * @param parent the parent application context for this context, may be null if there is no parent
     * @param configs configuration resources for the service
     * @param backgroundTaskTimer timer used to schedule background processes
     * @param pollingFrequency frequency, in milliseconds, that the configuration resources are polled for changes
     */
    public AttributeResolver(String id, ApplicationContext parent, List<Resource> configs, Timer backgroundTaskTimer,
            long pollingFrequency) {
        super(id, parent, configs, backgroundTaskTimer, pollingFrequency);
        // TODO Auto-generated constructor stub
    }

    /**
     * Gets all the attributes for a given subject.
     * 
     * @param requestContext the attribute resolution context that identifies the request subject and accumulates the
     *            resolved attributes
     * 
     * @throws AttributeResolutionException thrown if there is a problem resolving the attributes for the subject
     */
    public void resolveAttributes(final AttributeResolutionContext requestContext) throws AttributeResolutionException {
        // TODO Auto-generated method stub
    }

    /** {@inheritDoc} */
    public void validate() throws ServiceException {
        // TODO Auto-generated method stub

    }
}