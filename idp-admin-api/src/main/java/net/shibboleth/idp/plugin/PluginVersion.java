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

package net.shibboleth.idp.plugin;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A version string (Major.minor.patch) as a handy class.
 */
public final class PluginVersion implements Comparable<PluginVersion>{

    /** arbitrary maximum (to help hashing and sanity). */
    private static final int MAX_VNO = 10000;

    /** Major version. */
    private int major;
    
    /** Minor version. */
    private int minor;
    
    /** Patch version. */
    private int patch;
    
    /**
     * Constructor.
     *
     * @param version what to build from 
     * @throws NumberFormatException if it doesn't fit a 1.2.3 format or if the values are
     * out of range
     */
    public PluginVersion(final String version) throws NumberFormatException {
        
        final String versionStr = StringSupport.trimOrNull(version);
        if (null == versionStr) {
            throw new NumberFormatException("Empty Version not allowed");
        }
        
        final String[] components  = versionStr.split("\\.|\\+|-");
        if (components.length >= 1) {
            major = parseValue(components[0]);
        }
        if (components.length >= 2) {
            minor = parseValue(components[1]);
        }
        if (components.length >= 3) {
            patch = parseValue(components[2]);
        }
    }
    
    /**
     * Constructor.
     *
     * @param plugin what to get the version of.
     * @throws NumberFormatException if the values are out of range
     */
    public PluginVersion(@Nonnull final IdPPlugin plugin) throws NumberFormatException {
        this(plugin.getMajorVersion(), plugin.getMinorVersion(), plugin.getPatchVersion());
    }

    /**
     * Constructor.
     *
     * @param maj Major Version 
     * @param min Minor Version 
     * @param pat Patch Version
     * @throws NumberFormatException if the values are out of range
     */
    public PluginVersion(final int maj, final int min, final int pat) throws NumberFormatException {
        major = maj;
        if (maj < 0 || maj >= MAX_VNO) {
            throw new NumberFormatException("Improbable major version number : " + maj);
        }
        minor = min;
        if (min < 0 || min >= MAX_VNO) {
            throw new NumberFormatException("Improbable minor version number : " + min);
        }
        patch = pat;
        if (pat < 0 || pat >= MAX_VNO) {
            throw new NumberFormatException("Improbable patch version number : " + pat);
        }
    }

    /** Get the major version.
     * @return Returns the major version.
     */
    public int getMajor() {
        return major;
    }

    /** Get the minor version.
     * @return Returns the minor version.
     */
    public int getMinor() {
        return minor;
    }

    /** Get the patch version.
     * @return Returns the patch version.
     */
    public int getPatch() {
        return patch;
    }
    
    /** Is this version all zeros (usually as a result of a parsing issue.
     * @return if all nulls */
    public boolean isNull() {
        return major == 0 && minor ==0 && patch == 0;
    }
    
    /** Helper function for the constructor.
     *
     * Parse a string into an int with a range check.
     * @param valueAsString what to parse
     * @return the value as an int
     * @throws NumberFormatException if {@link Integer#parseInt(String, int)} does
     * or if the value is less than 0 or &gt; {@link #MAX_VNO}.
     */
    private int parseValue(final String valueAsString) throws NumberFormatException{
        final int value = Integer.parseInt(valueAsString);
        if (value < 0 || value >= MAX_VNO) {
            throw new NumberFormatException("Improbable version number : " + value);
        }
        return value;
    }

    /** {@inheritDoc} */
    public boolean equals(final Object obj) {
        if (obj instanceof PluginVersion) {
            final PluginVersion other= (PluginVersion)obj;
            return other.major == major && other.minor == minor && other.patch == other.patch;
        }
        return false;
    }
    
    /** {@inheritDoc} */
    public int hashCode() {
        long l = major*MAX_VNO*MAX_VNO;
        l += minor * MAX_VNO;
        l += patch;
        return Long.hashCode(l);
    }

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * {@inheritDoc} */
    public int compareTo(final PluginVersion other) {
        if (major == other.major) {
            if (minor == other.minor) {
                return patch - other.patch;
            }
            return minor - other.minor;
        }
        return major - other.major;
    }
    
    /** {@inheritDoc} */
    public String toString() {
        return new StringBuffer(8).append(Integer.toString(major))
                .append('.')
                .append(Integer.toString(minor))
                .append('.')
                .append(Integer.toString(patch)).toString();
    }
}
