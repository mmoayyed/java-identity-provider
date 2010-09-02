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

package edu.internet2.middleware.shibboleth.idp.service;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.Lock;

import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;
import org.opensaml.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.internet2.middleware.shibboleth.idp.log.EventLogger;
import edu.internet2.middleware.shibboleth.idp.log.PerformanceEvent;

/**
 * Base class for {@link ReloadableService}. This base class will start a background thread that will perform a periodic
 * check, via {@link #shouldReload()}, and, if required, invoke the services {@link #reload()} method.
 */
public abstract class AbstractReloadableService extends AbstractService implements ReloadableService {

    /** Class logger. */
    private Logger log = LoggerFactory.getLogger(AbstractReloadableService.class);

    /** Milliseconds between one reload check and another. */
    private final long reloadCheckDelay;

    /** Watcher that monitors the set of configuration resources for this service for changes. */
    private final ServiceReloadTask reloadTask;

    /** The last time time the service was reloaded, whether successful or not. */
    private DateTime lastReloadInstant;

    /** The last time the service was reloaded successfully. */
    private DateTime lastSuccessfulReleaseIntant;

    /** The cause of the last reload failure, if the last reload failed. */
    private Throwable reloadFailureCause;

    /**
     * Constructor.
     * 
     * @param id unique service identifier
     * @param reloadTaskTimer timer used to schedule service reloading background task
     * @param reloadDelay milliseconds between one reload check and another
     */
    public AbstractReloadableService(final String id, final Timer reloadTaskTimer, final long reloadDelay) {
        super(id);

        if (reloadDelay > 0) {
            Assert.isNotNull(reloadTaskTimer, "Reload task timer may not be null");
            Assert.isGreaterThan(0, reloadDelay, "Reload delay must be greater than 0");
            reloadCheckDelay = reloadDelay;
            reloadTask = new ServiceReloadTask();
            reloadTaskTimer.schedule(reloadTask, reloadCheckDelay, reloadCheckDelay);
        } else {
            reloadCheckDelay = -1;
            reloadTask = null;
        }
    }

    /**
     * Gets the number of milliseconds between one reload check and another.
     * 
     * @return number of milliseconds between one reload check and another
     */
    public long getReloadCheckDelay() {
        return reloadCheckDelay;
    }

    /** {@inheritDoc} */
    public DateTime getLastReloadAttemptInstant() {
        return lastReloadInstant;
    }

    /** {@inheritDoc} */
    public DateTime getLastSuccessfulReloadInstant() {
        return lastSuccessfulReleaseIntant;
    }

    /** {@inheritDoc} */
    public Throwable getReloadFailureCause() {
        return reloadFailureCause;
    }

    /**
     * {@inheritDoc}
     * 
     * This implementation will set the current state to {@link ReloadableService#STATE_RELOADING}, call
     * {@link #doPreReload(HashMap)}, {@link #doReload(HashMap)}, and {@link #doPostRelaod(HashMap)} in turn. It will
     * manage the {@link #lastReloadInstant} and {@link #lastSuccessfulReleaseIntant} information and record the reload
     * performance event.
     */
    public final void reload() {
        PerformanceEvent perfEvent = new PerformanceEvent(getId() + ".reload");

        Lock serviceWriteLock = getServiceLock().writeLock();
        HashMap context = new HashMap();

        DateTime now = new DateTime(ISOChronology.getInstanceUTC());
        lastReloadInstant = now;

        try {
            serviceWriteLock.lock();

            doPreReload(context);
            doReload(context);
            doPostRelaod(context);

            lastSuccessfulReleaseIntant = now;
            perfEvent.stopTime(true);
        } catch (ServiceException e) {
            reloadFailureCause = e;
            perfEvent.stopTime(false);
        } finally {
            serviceWriteLock.unlock();
            EventLogger.log(perfEvent);
        }
    }

    /**
     * Called by the {@link ServiceReloadTask} to determine if the service should be reloaded.
     * 
     * @return true if the service should be reloaded, false if not
     */
    protected abstract boolean shouldReload();

    /** {@inheritDoc} */
    protected void doPreStop(final HashMap context) throws ServiceException {
        reloadTask.cancel();
        super.doPreStop(context);
    }

    /**
     * Prepares the service to reload its configuration.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * @param context Collection of data carried through {@link #doPreReload(HashMap)}, {@link #doReload(HashMap)}, and
     *            {@link #doPostRelaod(HashMap)}. This is an appropriate place to keep state as the reload process
     *            progresses.
     * 
     * @throws ServiceException thrown if there is a problem reloading the service
     */
    protected void doPreReload(final HashMap context) throws ServiceException {
        log.debug("Reloading service '{}'", getId());

    }

    /**
     * Performs the actual reload.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * @param context Collection of data carried through {@link #doPreReload(HashMap)}, {@link #doReload(HashMap)}, and
     *            {@link #doPostRelaod(HashMap)}. This is an appropriate place to keep state as the reload process
     *            progresses
     * 
     * @throws ServiceException thrown if there is a problem reloading the service
     */
    protected void doReload(final HashMap context) throws ServiceException {

    }

    /**
     * Performs any final tasks necessary for the reload.
     * 
     * This method is called within the service write lock and may change service state.
     * 
     * The default implementation of this method does not do anything.
     * 
     * @param context Collection of data carried through {@link #doPreReload(HashMap)}, {@link #doReload(HashMap)}, and
     *            {@link #doPostRelaod(HashMap)}. This is an appropriate place to keep state as the reload process
     *            progresses
     * 
     * @throws ServiceException thrown if there is a problem reloading the service
     */
    protected void doPostRelaod(final HashMap context) throws ServiceException {
        log.info("Service '{}' reloaded", getId());
    }

    /**
     * A watcher that determines if one or more of configuration files for a service has been created, changed, or
     * deleted.
     */
    protected class ServiceReloadTask extends TimerTask {

        /** {@inheritDoc} */
        public void run() {
            if (shouldReload()) {
                reload();
            }
        }
    }
}