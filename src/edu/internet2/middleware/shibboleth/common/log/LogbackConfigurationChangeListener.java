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

package edu.internet2.middleware.shibboleth.common.log;

import org.opensaml.util.resource.Resource;
import org.opensaml.util.resource.ResourceChangeListener;
import org.opensaml.util.resource.ResourceException;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.status.ErrorStatus;
import ch.qos.logback.core.status.InfoStatus;
import ch.qos.logback.core.status.StatusManager;

/**
 * Callback that may be registered for a watch logback configuration file.
 */
public class LogbackConfigurationChangeListener implements ResourceChangeListener {

    /** {@inheritDoc} */
    public void onResourceCreate(Resource resource) {
        configureLogback(resource);
    }

    /** {@inheritDoc} */
    public void onResourceDelete(Resource resource) {
        // do nothing
    }

    /** {@inheritDoc} */
    public void onResourceUpdate(Resource resource) {
        configureLogback(resource);
    }

    /**
     * Configures logback using the given resource as the Joran configuration file.
     * 
     * @param configuration logback configuration file
     */
    protected void configureLogback(Resource configuration) {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        StatusManager statusManager = loggerContext.getStatusManager();
        statusManager.add(new InfoStatus("Loading logging configuration file: " + configuration.getLocation(), this));
        try {
            loggerContext.shutdownAndReset();
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(loggerContext);
            configurator.doConfigure(configuration.getInputStream());
            loggerContext.start();
        } catch (JoranException e) {
            statusManager.add(new ErrorStatus("Error loading logging configuration file: "
                    + configuration.getLocation(), this, e));
        } catch (ResourceException e) {
            statusManager.add(new ErrorStatus("Error loading logging configuration file: "
                    + configuration.getLocation(), this, e));
        }
    }
}
