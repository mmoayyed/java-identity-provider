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

package net.shibboleth.idp.profile.spring.relyingparty.metadata;

import java.util.List;

import javax.xml.namespace.QName;

import net.shibboleth.utilities.java.support.xml.ElementSupport;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.parsing.Location;
import org.springframework.beans.factory.parsing.Problem;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.w3c.dom.Element;

/**
 * Helper functions for dealing with legacy &lt;Resource&gt; statements.
 */
public final class ResourcesHelper {

    /** Namespace. */
    public static final String NAMESPACE = "urn:mace:shibboleth:2.0:resource";

    /** The Resource element. */
    public static final QName ELEMENT_NAME = new QName(NAMESPACE, "Resource");

    /** The ResourceFilter element. */
    public static final QName FILTER_NAME = new QName(NAMESPACE, "ResourceFilter");

    /** The local name for a Classpath Resource. */
    public static final String CLASSPATH_LOCAL_NAME = "ClasspathResource";

    /** The QName for a Classpath Resource. */
    public static final QName CLASSPATH_ELEMENT_NAME = new QName(NAMESPACE, CLASSPATH_LOCAL_NAME);

    /** The local name for a Filesystem Resource. */
    public static final String FILESYSTEM_LOCAL_NAME = "FilesystemResource";

    /** The QName for a Filesystem Resource. */
    public static final QName FILESYSTEM_ELEMENT_NAME = new QName(NAMESPACE, FILESYSTEM_LOCAL_NAME);

    /** The local name for a HTTP Resource. */
    public static final String HTTP_LOCAL_NAME = "HttpResource";

    /** The QName for a HTTP Resource. */
    public static final QName HTTP_ELEMENT_NAME = new QName(NAMESPACE, HTTP_LOCAL_NAME);

    /** The local name for a file backed HTTP Resource. */
    public static final String FILE_HTTP_LOCAL_NAME = "FileBackedHttpResource";

    /** The QName for a file backed HTTP Resource. */
    public static final QName FILE_HTTP_ELEMENT_NAME = new QName(NAMESPACE, FILE_HTTP_LOCAL_NAME);

    /** The local name for a SVN Resource. */
    public static final String SVN_LOCAL_NAME = "SVNResource";

    /** The QName for a SVN Resource. */
    public static final QName SVN_ELEMENT_NAME = new QName(NAMESPACE, SVN_LOCAL_NAME);

    /** (private) Constructor. */
    private ResourcesHelper() {
    }

    /** Check that there are no filters on the resource.
     * @param resourceElement the element to look at
     * @param readerContext the reader context
     * @throws BeanDefinitionParsingException if we encounter a filter
     */
    public static void noFilters(Element resourceElement, XmlReaderContext readerContext) {
        List<Element> filters = ElementSupport.getChildElements(resourceElement, FILTER_NAME);

        if (null == filters || filters.isEmpty()) {
            return;
        }
        throw new BeanDefinitionParsingException(new Problem("Resource filters are not supported", new Location(
                readerContext.getResource())));
    }

}
