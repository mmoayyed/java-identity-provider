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

package edu.internet2.middleware.shibboleth.idp;

/** Utility class that gives access to, and prints, the name and version of the software. */
public final class Version {
    
    /** Name of {@link org.slf4j.MDC} attribute that holds the IdP's version string: {@value}. */
    public static final String MDC_ATTRIBUTE = "idp.version";

    /**
     * Prints out the software name and version number.
     * 
     * @param args command line arguments (unused)
     */
    public static void main(String[] args){
        System.out.println(getSoftwareName() + " version " + getVersionString());
    }
    
    /**
     * Gets the software name from the {@link Package} metadata.
     * 
     * @return software name
     */
    public static String getSoftwareName(){
        Package pkg = Version.class.getPackage();
        return pkg.getImplementationTitle();
    }
    
    /**
     * Gets the version of the software as a string.
     * 
     * @return version of the software
     */
    public static String getVersionString(){
        Package pkg = Version.class.getPackage();
        return pkg.getImplementationVersion();
    }
}