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

package edu.internet2.middleware.shibboleth.common.profile.provider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.locks.Lock;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.opensaml.resource.Resource;
import org.opensaml.resource.ResourceException;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;
import edu.internet2.middleware.shibboleth.common.profile.AbstractErrorHandler;
import edu.internet2.middleware.shibboleth.common.profile.AbstractProfileHandler;
import edu.internet2.middleware.shibboleth.common.profile.ProfileHandlerManager;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Implementation of a {@link ProfileHandlerManager} that maps the request path, without the servlet context, to a
 * profile handler.
 */
public class ShibbolethProfileHandlerManager extends BaseReloadableService implements ProfileHandlerManager {

    /** Handler used for errors. */
    private AbstractErrorHandler errorHandler;

    /** Map of request paths to profile handlers. */
    private Map<String, AbstractProfileHandler> profileHandlers;

    /**
     * Constructor. Configuration resources are not monitored for changes.
     * 
     * @param configurations configuration resources for this service
     */
    public ShibbolethProfileHandlerManager(List<Resource> configurations) {
        super(configurations);
        profileHandlers = new HashMap<String, AbstractProfileHandler>();
    }

    /**
     * Constructor.
     * 
     * @param timer timer resource polling tasks are scheduled with
     * @param configurations configuration resources for this service
     * @param pollingFrequency the frequency, in milliseconds, to poll the policy resources for changes, must be greater
     *            than zero
     * @param pollingRetryAttempts maximum number of poll attempts before a policy resource is considered inaccessible,
     *            must be greater than zero
     */
    public ShibbolethProfileHandlerManager(List<Resource> configurations, Timer timer, long pollingFrequency,
            int pollingRetryAttempts) {
        super(timer, configurations, pollingFrequency, pollingRetryAttempts);
        profileHandlers = new HashMap<String, AbstractProfileHandler>();
    }

    /** {@inheritDoc} */
    public AbstractErrorHandler getErrorHandler() {
        return errorHandler;
    }

    /**
     * Sets the error handler.
     * 
     * @param handler error handler
     */
    public void setErrorHandler(AbstractErrorHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Error handler may not be null");
        }
        errorHandler = handler;
    }

    /** {@inheritDoc} */
    public AbstractProfileHandler getProfileHandler(ServletRequest request) {
        AbstractProfileHandler handler;

        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        handler = profileHandlers.get(((HttpServletRequest) request).getPathInfo());
        readLock.unlock();
        return handler;
    }

    /**
     * Gets the registered profile handlers.
     * 
     * @return registered profile handlers
     */
    public Map<String, AbstractProfileHandler> getProfileHandlers() {
        return profileHandlers;
    }

    /** {@inheritDoc} */
    protected void newContextCreated(ApplicationContext newServiceContext) throws ResourceException {
        String[] errorBeanNames = newServiceContext.getBeanNamesForType(AbstractErrorHandler.class);
        String[] profileBeanNames = newServiceContext.getBeanNamesForType(AbstractRequestBoundProfileHandler.class);

        Lock writeLock = getReadWriteLock().writeLock();
        writeLock.lock();

        errorHandler = (AbstractErrorHandler) newServiceContext.getBean(errorBeanNames[0]);

        profileHandlers.clear();
        AbstractRequestBoundProfileHandler<RelyingPartyConfigurationManager, Session> profileHandler;
        for (String profileBeanName : profileBeanNames) {
            profileHandler = (AbstractRequestBoundProfileHandler) newServiceContext.getBean(profileBeanName);
            for (String requestPath : profileHandler.getRequestPaths()) {
                profileHandlers.put(requestPath, profileHandler);
            }
        }

        writeLock.unlock();
    }
}