/*
 * Copyright [2006] [University Corporation for Advanced Internet Development, Inc.]
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

import javax.xml.parsers.FactoryConfigurationError;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.xml.DOMConfigurator;
import org.opensaml.log.Level;
import org.opensaml.resource.Resource;
import org.opensaml.resource.ResourceChangeListener;

/**
 * Resource listener that watchs a Log4J configuration file and reconfigures Log4J if the file changes.
 * 
 * Note, while the configuration is being reloaded some logging messages may be missed.
 * 
 * Do <strong>not</strong> use this if logging is controlled by an external system such as a servlet container.
 */
public class Log4jConfigFileResourceListener implements ResourceChangeListener {

    /** Class logger. */
    private static Logger log = Logger.getLogger(Log4jConfigFileResourceListener.class);

    /** Default logging layout pattern for appenders. */
    private static String defaultLayoutPattern = "%d %-5p [%c] %m%n";

    /** {@inheritDoc} */
    public void onResourceCreate(Resource log4jConfig) {
        loadConfiguration(log4jConfig);
    }

    /** {@inheritDoc} */
    public void onResourceDelete(Resource log4jConfig) {
        loadConfiguration(log4jConfig);
    }

    /** {@inheritDoc} */
    public void onResourceUpdate(Resource log4jConfig) {
        loadDefaultConfiguration();
    }

    /**
     * Loads a Log4J configuration file and replaces the current configuration with the new one.
     * 
     * @param log4jConfig the Log4J configuration file
     */
    protected void loadConfiguration(Resource log4jConfig) {
        log.log(Level.CRITICAL, "Loading Log4J configuration file " + log4jConfig.getLocation());

        try {
            LogManager.resetConfiguration();
            DOMConfigurator.configure(log4jConfig.getLocation());
            log.log(Level.CRITICAL, "Log4J configuration file " + log4jConfig.getLocation() + " loaded");
        } catch (FactoryConfigurationError e) {
            log.error("Unable to read Log4J configuration file " + log4jConfig.getLocation(), e);
        }
    }

    /**
     * Loads a default Log4J configuration.
     */
    protected void loadDefaultConfiguration() {
        ConsoleAppender console = new ConsoleAppender(new PatternLayout(defaultLayoutPattern),
                ConsoleAppender.SYSTEM_OUT);
        Logger root = Logger.getRootLogger();
        root.setLevel(Level.WARN);
        root.addAppender(console);
        
        Logger auditLog = Logger.getLogger(AuditLogEntry.AUDIT_LOGGER_NAME);
        auditLog.setAdditivity(false);
        auditLog.setLevel(Level.CRITICAL);

        Logger shibLog = Logger.getLogger("edu.internet2.middleware.shibboleth");
        shibLog.setAdditivity(false);
        shibLog.setLevel(Level.INFO);

        Logger osLog = Logger.getLogger("org.opensaml");
        osLog.setAdditivity(false);
        osLog.setLevel(Level.INFO);
    }
}