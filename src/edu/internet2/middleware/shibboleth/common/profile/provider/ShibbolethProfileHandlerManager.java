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

import org.apache.log4j.Logger;
import org.opensaml.resource.Resource;
import org.opensaml.resource.ResourceException;
import org.springframework.context.ApplicationContext;

import edu.internet2.middleware.shibboleth.common.config.BaseReloadableService;
import edu.internet2.middleware.shibboleth.common.profile.AbstractErrorHandler;
import edu.internet2.middleware.shibboleth.common.profile.AbstractProfileHandler;
import edu.internet2.middleware.shibboleth.common.profile.ProfileHandlerManager;
import edu.internet2.middleware.shibboleth.common.profile.RequestHandler;
import edu.internet2.middleware.shibboleth.common.relyingparty.RelyingPartyConfigurationManager;
import edu.internet2.middleware.shibboleth.common.session.Session;

/**
 * Implementation of a {@link ProfileHandlerManager} that maps the request path, without the servlet context, to a
 * profile handler.
 */
public class ShibbolethProfileHandlerManager extends BaseReloadableService implements ProfileHandlerManager {

    /** Class logger. */
    private final Logger log = Logger.getLogger(ShibbolethProfileHandlerManager.class);

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
     */
    public ShibbolethProfileHandlerManager(List<Resource> configurations, Timer timer, long pollingFrequency) {
        super(timer, configurations, pollingFrequency);
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
    public RequestHandler getProfileHandler(ServletRequest request) {
        RequestHandler handler;

        String requestPath = ((HttpServletRequest) request).getPathInfo();
        if (log.isDebugEnabled()) {
            log.debug("Looking up profile handler for request path: " + requestPath);
        }
        Lock readLock = getReadWriteLock().readLock();
        readLock.lock();
        handler = profileHandlers.get(requestPath);
        readLock.unlock();

        if (handler != null) {
            if (log.isDebugEnabled()) {
                log.debug("Located profile handler of the following type for request path " + requestPath + ": "
                        + handler.getClass().getName());
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No profile handler registered for request path " + requestPath);
            }
        }
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
        if (log.isDebugEnabled()) {
            log.debug("Loading new configuration into service");
        }
        String[] errorBeanNames = newServiceContext.getBeanNamesForType(AbstractErrorHandler.class);
        String[] profileBeanNames = newServiceContext.getBeanNamesForType(AbstractRequestBoundProfileHandler.class);

        Lock writeLock = getReadWriteLock().writeLock();
        writeLock.lock();

        errorHandler = (AbstractErrorHandler) newServiceContext.getBean(errorBeanNames[0]);
        if (log.isDebugEnabled()) {
            log.debug("Loaded new error handler of type: " + errorHandler.getClass().getName());
        }

        profileHandlers.clear();
        if(log.isDebugEnabled()){
            log.debug(profileBeanNames.length + " profile handlers loaded");
        }
        AbstractRequestBoundProfileHandler<RelyingPartyConfigurationManager, Session> profileHandler;
        for (String profileBeanName : profileBeanNames) {
            profileHandler = (AbstractRequestBoundProfileHandler) newServiceContext.getBean(profileBeanName);
            for (String requestPath : profileHandler.getRequestPaths()) {
                profileHandlers.put(requestPath, profileHandler);
                if (log.isDebugEnabled()) {
                    log.debug("Request path " + requestPath + " mapped to profile handler of type: "
                            + profileHandler.getClass().getName());
                }
            }
        }

        writeLock.unlock();
    }
}