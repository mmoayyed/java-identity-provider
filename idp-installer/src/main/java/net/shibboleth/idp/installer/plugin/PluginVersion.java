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

package net.shibboleth.idp.installer.plugin;

import net.shibboleth.utilities.java.support.primitive.StringSupport;

/**
 * A version string (Major.minor.patch) as a handy class.
 */
public final class PluginVersion implements Comparable<PluginVersion>{

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
     * @throws NumberFormatException if it doesn't fit a 1.2.3 format.
     */
    public PluginVersion(final String version) throws NumberFormatException {
        
        final String versionStr = StringSupport.trimOrNull(version);
        if (null == versionStr) {
            throw new NumberFormatException("Empty Version not allowed");
        }
        
        final String[] components  = versionStr.split("\\.|\\+|-");
        if (components.length >= 1) {
            major = Integer.parseInt(components[0]);
        }
        if (components.length >= 2) {
            minor = Integer.parseInt(components[1]);
        }
        if (components.length >= 3) {
            patch = Integer.parseInt(components[2]);
        }
    }
    
    /**
     * Constructor.
     *
     * @param maj Major Version 
     * @param min Minor Version 
     * @param pat Patch Version 
     */
    public PluginVersion(final int maj, final int min, final int pat) {
        major = maj;
        minor = min;
        patch = pat;
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
        return Integer.valueOf(major*100000 + minor*1000 +patch).hashCode();
    }

    /** {@inheritDoc} */
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
