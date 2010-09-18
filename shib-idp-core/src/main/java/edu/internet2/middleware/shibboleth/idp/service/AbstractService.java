/*
 * Copyright 2009 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.idp.service;

import java.util.HashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.util.ObjectSupport;
import org.opensaml.util.StringSupport;
import org.opensaml.xml.util.DatatypeHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.log.EventLogger;
import edu.internet2.middleware.shibboleth.idp.log.PerformanceEvent;

/** Base class for {@link Service} implementations. */
@ThreadSafe
public abstract class AbstractService implements Service {

    /** Suffix appended to service ID to form the starting performance event ID.  Value: {@value} */
    public static final String START_PERF_EVENT_ID_SUFFIX = ".start";
    
    /** Suffix appended to service ID to form the stopping performance event ID.  Value: {@value} */
    public static final String STOP_PERF_EVENT_ID_SUFFIX = ".stop";
    
    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AbstractService.class);

    /** Unique name of this service. */
    private final String id;

    /** Human readable display name for this service. */
    private String displayName;

    /** The current state of the service. */
    private String currentState;

    /** Lock for this service. */
    private ReentrantReadWriteLock serviceLock;

    /**
     * Constructor.
     * 
     * @param serviceId the unique ID of this service
     */
    public AbstractService(final String serviceId) {
        this.id = DatatypeHelper.safeTrimOrNullString(serviceId);
        Assert.isNotNull(this.id, "Service ID may not be null of empty");

        currentState = STATE_NEW;
        serviceLock = new ReentrantReadWriteLock(true);
    }

    /** {@inheritDoc} */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Sets the display name of this service.
     * 
     * @param name display name of this service, may not be null or empty
     */
    public void setDisplayName(final String name) {
        String temp = DatatypeHelper.safeTrimOrNullString(name);
        Assert.isNotNull(temp, "Service display name may not be null or empty");

        displayName = temp;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public final String getCurrentState() {
        return currentState;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation will check to see if the service is already started or starting, if so it simply returns. If
     * the service is not currently starting or started the start will be set to {@link Service#STATE_STARTING} and then
     * {@link #doPreStart()}, {@link #doStart()} and {@link #doPostStart()} will be invoked in that order. Finally,
     * assuming no {@link ServiceException} is thrown, the service's state will be set {@link Service#STATE_RUNNING}. If
     * an exception is thrown the service state is set to {@link Service#STATE_STOPPED} and the exception is rethrown.
     * All startup work is performed within a service write lock. A startup performance event will be recorded.
     */
    public final void start() throws ServiceException {
        PerformanceEvent perfEvent = new PerformanceEvent(getId() + START_PERF_EVENT_ID_SUFFIX);

        Lock serviceWriteLock = getServiceLock().writeLock();
        HashMap context = new HashMap();

        try {
            serviceWriteLock.lock();

            if (ObjectSupport.equalsAny(getCurrentState(), STATE_STARTING, STATE_RUNNING)) {
                return;
            }
            setCurrentState(STATE_STARTING);

            doPreStart(context);
            doStart(context);
            doPostStart(context);

            setCurrentState(STATE_RUNNING);
            perfEvent.stopTime(true);
        } catch (ServiceException e) {
            setCurrentState(STATE_STOPPED);
            perfEvent.stopTime(false);
            throw e;
        } finally {
            serviceWriteLock.unlock();
            EventLogger.log(perfEvent);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation will check to see if the service is already stopping or stopped, if so it simply returns. If
     * the service is not stopping or stopped the current state will be set to {@link Service#STATE_STOPPING} then
     * {@link #doPreStop()} and {@link #doStop()} will be called. Regardless of whether an exception is thrown
     * {@link #doPostStop()} will be called and the current state will be set to {@link Service#STATE_STOPPED}. If a
     * {@link ServiceException} is thrown during the stopping process it is rethrown. A stopping performance event will
     * also be recorded.
     */
    public final void stop() throws ServiceException {
        PerformanceEvent perfEvent = new PerformanceEvent(getId() + STOP_PERF_EVENT_ID_SUFFIX);

        Lock serviceWriteLock = getServiceLock().writeLock();
        HashMap context = new HashMap();

        try {
            serviceWriteLock.lock();

            if (ObjectSupport.equalsAny(getCurrentState(), STATE_STOPPING, STATE_STOPPED)) {
                return;
            }
            setCurrentState(STATE_STOPPING);

            doPreStop(context);
            doStop(context);
            perfEvent.stopTime(true);
        } catch (ServiceException e) {
            perfEvent.stopTime(false);
            throw e;
        } finally {
            doPostStop(context);
            setCurrentState(STATE_STOPPED);
            serviceWriteLock.unlock();
            EventLogger.log(perfEvent);
        }
    }

    /**
     * Gets the lock guarding operations that mutate this service.
     * 
     * @return lock guarding operations that mutate this service
     */
    protected final ReadWriteLock getServiceLock() {
        return serviceLock;
    }

    /**
     * Sets the current state of the service.
     * 
     * @param state current state of the service
     */
    protected final void setCurrentState(final String state) {
        currentState = StringSupport.trimOrNull(state);
        Assert.isNotNull(currentState, "State indicator may not be null or empty");
    }

    /**
     * Runs prior to the main start up process.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * Default implementation of this method simply checks to see if service is currently stopping or stopped and, if
     * so, throws an error.
     * 
     * @param context Collection of data carried through {@link #doPreStart(HashMap)}, {@link #doStart(HashMap)}, and
     *            {@link #doPostStart(HashMap)}. This is an appropriate place to keep state as the startup process
     *            progresses.
     * 
     * @throws ServiceException thrown if the service is stopped or stopping
     */
    protected void doPreStart(final HashMap context) throws ServiceException {
        if (ObjectSupport.equalsAny(getCurrentState(), STATE_STOPPING, STATE_STOPPED)) {
            throw new ServiceException(getId() + " service has been stopped, it may not be started again.");
        }
        log.debug("Loading configuration for service '{}'", getId());
    }

    /**
     * Performs the main start up process.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @param context Collection of data carried through {@link #doPreStart(HashMap)}, {@link #doStart(HashMap)}, and
     *            {@link #doPostStart(HashMap)}. This is an appropriate place to keep state as the startup process
     *            progresses.
     * 
     * @throws ServiceException thrown if there is a problem starting the service
     */
    protected void doStart(final HashMap context) throws ServiceException {

    }

    /**
     * Runs after the main start up process.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @param context Collection of data carried through {@link #doPreStart(HashMap)}, {@link #doStart(HashMap)}, and
     *            {@link #doPostStart(HashMap)}. This is an appropriate place to keep state as the startup process
     *            progresses.
     * 
     * @throws ServiceException thrown if there is a problem starting the service
     */
    protected void doPostStart(final HashMap context) throws ServiceException {
        log.info("Loaded configuration for service '{}'", getId());
    }

    /**
     * Runs prior to the main stopping process.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @param context Collection of data carried through {@link #doPostStop(HashMap)}, {@link #doStop(HashMap)}, and
     *            {@link #doPostStop(HashMap)}. This is an appropriate place to keep state as the shutdown process
     *            progresses.
     * 
     * @throws ServiceException thrown if there is a problem stopping the service
     */
    protected void doPreStop(final HashMap context) throws ServiceException {
        log.debug("Stopping service '{}'", getId());
    }

    /**
     * Performs the main stopping process.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @param context Collection of data carried through {@link #doPostStop(HashMap)}, {@link #doStop(HashMap)}, and
     *            {@link #doPostStop(HashMap)}. This is an appropriate place to keep state as the shutdown process
     *            progresses.
     * 
     * @throws ServiceException thrown if there is a problem stopping the service
     */
    protected void doStop(final HashMap context) throws ServiceException {

    }

    /**
     * Runs after to the main stopping process.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * @param context Collection of data carried through {@link #doPostStop(HashMap)}, {@link #doStop(HashMap)}, and
     *            {@link #doPostStop(HashMap)}. This is an appropriate place to keep state as the shutdown process
     *            progresses.
     * 
     *            Default implementation of this method does not do anything.
     * 
     * @throws ServiceException thrown if there is a problem stopping the service
     */
    protected void doPostStop(final HashMap context) throws ServiceException {
        log.info("Service '{}' has been stopped", getId());
    }
}