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

package net.shibboleth.idp.installer.impl;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.event.Level;

/**
 * Shimmed logger.
 * If debug is enabled there is no difference, otherwise warn and above
 * goes to stdout.
 * If debug is enabled everything is logged but warn and above still go
 * to stdout (to allow the log to go to a file).
 */
public final class InstallationLogger implements Logger {

    /** The 'real' logger. */
    private final Logger encapsulated;

    /** Pattern to change {{} to %s.*/
    private final Pattern pat = Pattern.compile("\\{\\}");

    /**
     * Constructor.
     * @param parent - logger to encapsulate.
     */
    private InstallationLogger(final Logger parent) {
        encapsulated = parent;
    }

    /**
     * Convert format and output.
     * 
     * @param level logging level
     * @param format the format to use
     * @param arg the argument
     */
    private void format(@Nullable final Level level, @Nonnull final String format, final Object arg) {
        if (level != null) {
            System.out.print(level.toString() + "  - ");
        }
        System.out.format(pat.matcher(format).replaceAll("%s"), arg);
        System.out.println();
    }

    /**
     * Convert format and output.
     * 
     * @param level logging level
     * @param format the format to use
     * @param arg1 the first argument
     * @param arg2 the second argument
     */
    private void format(@Nullable final Level level, final String format, final Object arg1, final Object arg2) {
        if (level != null) {
            System.out.print(level.toString() + "  - ");
        }
        System.out.format(pat.matcher(format).replaceAll("%s"), arg1, arg2);
        System.out.println();
    }

    /**
     * Convert format and output.
     * 
     * @param level logging level
     * @param format the format to use
     * @param arguments the arguments
     */
    private void format(@Nullable final Level level, final String format, final Object... arguments) {
        if (level != null) {
            System.out.print(level.toString() + "  - ");
        }
        System.out.format(pat.matcher(format).replaceAll("%s"), arguments);
        System.out.println();
    }

    /** {@inheritDoc} */
    public String getName() {
        return encapsulated.getName();
    }

    /** {@inheritDoc} */
    public boolean isTraceEnabled() {
        return encapsulated.isTraceEnabled();
    }

    /** {@inheritDoc} */
    public void trace(final String msg) {
        encapsulated.trace(msg);
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg) {
        encapsulated.trace(format, arg);
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object arg1, final Object arg2) {
        encapsulated.trace(format, arg1, arg2);
    }

    /** {@inheritDoc} */
    public void trace(final String format, final Object... arguments) {
        encapsulated.trace(format, arguments);
    }

    /** {@inheritDoc} */
    public void trace(final String msg, final Throwable t) {
        encapsulated.trace(msg,t);
    }

    /** {@inheritDoc} */
    public boolean isTraceEnabled(final Marker marker) {
        return encapsulated.isTraceEnabled(marker);
    }

    /** {@inheritDoc} */
    public void trace(final Marker marker, final String msg) {
        encapsulated.trace(marker,msg);
    }

    /** {@inheritDoc} */
    public void trace(final Marker marker, final String format, final Object arg) {
        encapsulated.trace(marker,format,arg);
    }

    /** {@inheritDoc} */
    public void trace(final Marker marker, final String format, final Object arg1, final Object arg2) {
        encapsulated.trace(marker,format,arg1,arg2);
    }

    /** {@inheritDoc} */
    public void trace(final Marker marker, final String format, final Object... argArray) {
        encapsulated.trace(marker,format, argArray);
    }

    /** {@inheritDoc} */
    public void trace(final Marker marker, final String msg, final Throwable t) {
        encapsulated.trace(marker,msg,t);
    }

    /** {@inheritDoc} */
    public boolean isDebugEnabled() {
        return encapsulated.isDebugEnabled();
    }

    /** {@inheritDoc} */
    public void debug(final String msg) {
        encapsulated.debug(msg);
    }

    /** {@inheritDoc} */
    public void debug(final String format, final Object arg) {
        encapsulated.debug(format,arg);
    }

    /** {@inheritDoc} */
    public void debug(final String format, final Object arg1, final Object arg2) {
        encapsulated.debug(format,arg1,arg2);
    }

    /** {@inheritDoc} */
    public void debug(final String format, final Object... arguments) {
        encapsulated.debug(format, arguments);
    }

    /** {@inheritDoc} */
    public void debug(final String msg, final Throwable t) {
        encapsulated.debug(msg,t);
    }

    /** {@inheritDoc} */
    public boolean isDebugEnabled(final  Marker marker) {
        return encapsulated.isDebugEnabled(marker);
    }

    /** {@inheritDoc} */
    public void debug(final Marker marker, final String msg) {
        encapsulated.debug(marker,msg);
    }

    /** {@inheritDoc} */
    public void debug(final Marker marker, final String format, final Object arg) {
        encapsulated.debug(marker,format,arg);
    }

    /** {@inheritDoc} */
    public void debug(final Marker marker, final String format, final Object arg1, final Object arg2) {
        encapsulated.debug(marker,format,arg1,arg2);
    }

    /** {@inheritDoc} */
    public void debug(final Marker marker, final String format, final Object... arguments) {
        encapsulated.debug(marker, format, arguments);
    }
    /** {@inheritDoc} */

    public void debug(final Marker marker, final String msg, final Throwable t) {
        encapsulated.debug(marker,msg,t);
    }

    /** {@inheritDoc} */
    public boolean isInfoEnabled() {
        return encapsulated.isInfoEnabled();
    }

    /** {@inheritDoc} */
    public void info(final String msg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(msg);
        }
        System.out.println(msg);
    }

    /** {@inheritDoc} */
    public void info(final String format, final Object arg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(format,arg);
        }
        format(null, format, arg);
    }

    /** {@inheritDoc} */
    public void info(final String format, final Object arg1, final Object arg2) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(format, arg1, arg2);
        }
        format(null, format, arg1, arg2);
    }

    /** {@inheritDoc} */
    public void info(final String format, final Object... arguments) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(format, arguments);
        }
        format(null, format, arguments);
    }

    /** {@inheritDoc} */
    public void info(final String msg, final Throwable t) {
        if (encapsulated.isDebugEnabled()) {
        encapsulated.info(msg,t);
        }
        format(null, "%s %s", msg, t);
    }

    /** {@inheritDoc} */
    public boolean isInfoEnabled(final Marker marker) {
        return encapsulated.isInfoEnabled(marker);
    }

    /** {@inheritDoc} */
    public void info(final Marker marker, final String msg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(marker, msg);
        }
        System.out.println(msg);
    }

    /** {@inheritDoc} */
    public void info(final Marker marker, final String format, final Object arg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(marker, format, arg);
        }
        format(null, format, arg);
    }

    /** {@inheritDoc} */
    public void info(final Marker marker, final String format, final Object arg1, final Object arg2) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(marker, format, arg1, arg2);
        }
    }

    /** {@inheritDoc} */
    public void info(final Marker marker, final String format, final Object... arguments) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(marker, format, arguments);
        }
        format(null, format, arguments);
    }

    /** {@inheritDoc} */
    public void info(final Marker marker, final String msg, final Throwable t) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.info(marker, msg, t);
        } else {
            System.out.format("%s %s", msg, t);
            System.out.println();
        }
    }

    /** {@inheritDoc} */
    public boolean isWarnEnabled() {
        return encapsulated.isWarnEnabled();
    }

    /** {@inheritDoc} */
    public void warn(final String msg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(msg);
        }
        System.out.print("WARN  -  ");
        System.out.println(msg);
    }

    /** {@inheritDoc} */
    public void warn(final String format, final Object arg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(format, arg);
        }
        format(Level.WARN, format, arg);
    }

    /** {@inheritDoc} */
    public void warn(final String format, final Object... arguments) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(format, arguments);
        }
        format(Level.WARN, format, arguments);
    }

    /** {@inheritDoc} */
    public void warn(final String format, final Object arg1, final Object arg2) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(format, arg1, arg2);
        }
        format(Level.WARN, format, arg1, arg2);
    }

    /** {@inheritDoc} */
    public void warn(final String msg, final Throwable t) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(msg, t);
        }
        System.out.print("WARN  -  ");
        System.out.format("%s %s", msg, t);
        System.out.println();
    }

    /** {@inheritDoc} */
    public boolean isWarnEnabled(final Marker marker) {
        return encapsulated.isWarnEnabled(marker);
    }

    /** {@inheritDoc} */
    public void warn(final Marker marker, final String msg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(marker, msg);
        }
        System.out.print("WARN  -  ");
        System.out.println(msg);
    }

    /** {@inheritDoc} */
    public void warn(final Marker marker, final String format, final Object arg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(marker, format, arg);
        }
        format(Level.WARN, format, arg);
    }

    /** {@inheritDoc} */
    public void warn(final Marker marker, final String format, final Object arg1, final Object arg2) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(marker, format, arg1, arg2);
        }
        format(Level.WARN, format, arg1, arg2);
    }

    /** {@inheritDoc} */
    public void warn(final Marker marker, final String format, final Object... arguments) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(marker, format, arguments);
        }
        format(Level.WARN, format, arguments);
    }

    /** {@inheritDoc} */
    public void warn(final Marker marker, final String msg, final Throwable t) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.warn(marker, msg, t);
        }
        System.out.print("WARN  -  ");
        System.out.format("%s %s", msg, t);
        System.out.println();
    }

    /** {@inheritDoc} */
    public boolean isErrorEnabled() {
        return encapsulated.isErrorEnabled();
    }

    /** {@inheritDoc} */
    public void error(final String msg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(msg);
        }
        System.out.print("ERROR  -  ");
        System.out.println(msg);
    }

    /** {@inheritDoc} */
    public void error(final String format, final Object arg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(format, arg);
        }
        format(Level.ERROR, format, arg);
    }

    /** {@inheritDoc} */
    public void error(final String format, final Object arg1, final Object arg2) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(format, arg1, arg2);
        }
        format(Level.ERROR, format, arg1, arg2);
    }

    /** {@inheritDoc} */
    public void error(final String format, final Object... arguments) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(format, arguments);
        }
        format(Level.ERROR, format, arguments);
    }

    /** {@inheritDoc} */
    public void error(final String msg, final Throwable t) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(msg, t);
        } else {
            System.out.print("ERROR  -  ");
            System.out.format("%s %s", msg, t);
            System.out.println();
        }
    }

    /** {@inheritDoc} */
    public boolean isErrorEnabled(final Marker marker) {
        return encapsulated.isErrorEnabled(marker);
    }

    /** {@inheritDoc} */
    public void error(final Marker marker, final String msg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(marker, msg);
        } else {
            System.out.print("ERROR  -  ");
            System.out.println(msg);
        }
    }

    /** {@inheritDoc} */
    public void error(final Marker marker, final String format, final Object arg) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(marker, format, arg);
        } else {
            format(Level.ERROR, format, arg);
        }
    }

    /** {@inheritDoc} */
    public void error(final Marker marker, final String format, final Object arg1, final Object arg2) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(marker, format, arg1, arg2);
        }
        format(Level.ERROR, format, arg1, arg2);
    }

    /** {@inheritDoc} */
    public void error(final Marker marker, final String format, final Object... arguments) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(marker, format, arguments);
        }
        format(Level.ERROR, format, arguments);
    }

    /** {@inheritDoc} */
    public void error(final Marker marker, final String msg, final Throwable t) {
        if (encapsulated.isDebugEnabled()) {
            encapsulated.error(marker, msg, t);
        }
        format(Level.ERROR, "%s %s", msg, t);
    }

    /** Replacement for {@link LoggerFactory#getLogger(Class)}.
     * @param clazz what to log
     * @return a logger
     */
    public static Logger getLogger(final Class<?> clazz) {
        return new InstallationLogger(LoggerFactory.getLogger(clazz));
    }

}