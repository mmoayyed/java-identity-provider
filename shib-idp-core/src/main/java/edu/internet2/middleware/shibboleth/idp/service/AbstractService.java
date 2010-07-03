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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.jcip.annotations.ThreadSafe;

import org.opensaml.util.Assert;
import org.opensaml.xml.util.DatatypeHelper;


/** Base class for {@link Service} implementations. */
@ThreadSafe
public abstract class AbstractService implements Service {

    /** Unique name of this service. */
    private String id;

    /** Human readable display name for this service. */
    private String displayName;

    /** The current state of the service. */
    private State currentState;

    /** Lock for this service. */
    private ReentrantReadWriteLock serviceLock;

    /**
     * Constructor.
     * 
     * @param serviceId the unique ID of this service
     */
    public AbstractService(String serviceId) {
        this.id = DatatypeHelper.safeTrimOrNullString(serviceId);
        Assert.isNotNull(this.id, "Service ID may not be null of empty");

        currentState = State.NEW;
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
    public void setDisplayName(String name) {
        String temp = DatatypeHelper.safeTrimOrNullString(name);
        Assert.isNotNull(temp, "Service display name may not be null or empty");
        
        displayName = temp;
    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public State getCurrentState() {
        return currentState;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation will check to see if the service is already started or starting, if so it simply returns. If
     * the service is not currently starting or started the start will be set to {@link State#STARTING} and then
     * {@link #doPreStart()}, {@link #doStart()} and {@link #doPostStart()} will be invoked in that order. Finally,
     * assuming no {@link ServiceException} is thrown, the service's state will be set {@link State#STARTED}. If an
     * exception is thrown the service state is set to {@link State#STOPPED} and the exception is rethrown. All startup
     * work is performed within a service write lock.
     */
    public void start() throws ServiceException {
        Lock serviceWriteLock = getServiceLock().writeLock();
        serviceWriteLock.lock();

        if (getCurrentState() == State.STARTING || getCurrentState() == State.STARTED) {
            return;
        }

        try {
            setCurrentState(State.STARTING);
            doPreStart();
            doStart();
            doPostStart();
            setCurrentState(State.STARTED);
        } catch (ServiceException e) {
            setCurrentState(State.STOPPED);
            throw e;
        } finally {
            serviceWriteLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation will check to see if the service is already stopping or stopped, if so it simply returns. If
     * the service is not stopping or stopped the current state will be set to {@link State#STOPPING} then
     * {@link #doPreStop()} and {@link #doStop()} will be called. Regardless of whether an exception is thrown
     * {@link #doPostStop()} will be called and the current state will be set to {@link State#STOPPED}. If a
     * {@link ServiceException} is thrown during the stopping process it is rethrown.
     */
    public void stop() throws ServiceException {
        Lock serviceWriteLock = getServiceLock().writeLock();
        serviceWriteLock.lock();

        if (getCurrentState() == State.STOPPING || getCurrentState() == State.STOPPED) {
            return;
        }

        setCurrentState(State.STOPPING);
        try {
            doPreStop();
            doStop();
        } finally {
            doPostStop();
            setCurrentState(State.STOPPED);
            serviceWriteLock.unlock();
        }
    }

    /**
     * Gets the lock guarding operations that mutate this service.
     * 
     * @return lock guarding operations that mutate this service
     */
    protected ReadWriteLock getServiceLock() {
        return serviceLock;
    }

    /**
     * Sets the current state of the service.
     * 
     * @param state current state of the service
     */
    protected void setCurrentState(State state) {
        currentState = state;
    }

    /**
     * Runs prior to the main start up process.
     * 
     * Default implementation of this method simply checks to see if service is currently stopping or stopped and, if
     * so, throws an error.
     * 
     * @throws ServiceException thrown if the service is stopped or stopping
     */
    protected void doPreStart() throws ServiceException {
        if (getCurrentState() == State.STOPPING || getCurrentState() == State.STOPPED) {
            throw new ServiceException(getId() + " service has been stopped, it may not be started again.");
        }
    }

    /**
     * Performs the main start up process.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @throws ServiceException thrown if there is a problem starting the service
     */
    protected void doStart() throws ServiceException {

    }

    /**
     * Runs after the main start up process.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @throws ServiceException thrown if there is a problem starting the service
     */
    protected void doPostStart() throws ServiceException {

    }

    /**
     * Runs prior to the main stopping process.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @throws ServiceException thrown if there is a problem stopping the service
     */
    protected void doPreStop() throws ServiceException {

    }

    /**
     * Performs the main stopping process.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @throws ServiceException thrown if there is a problem stopping the service
     */
    protected void doStop() throws ServiceException {

    }

    /**
     * Runs after to the main stopping process.
     * 
     * Default implementation of this method does not do anything.
     * 
     * @throws ServiceException thrown if there is a problem stopping the service
     */
    protected void doPostStop() throws ServiceException {

    }
}