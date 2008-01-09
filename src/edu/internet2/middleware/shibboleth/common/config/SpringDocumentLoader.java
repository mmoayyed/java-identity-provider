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

package edu.internet2.middleware.shibboleth.common.config;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.opensaml.xml.parse.ClasspathResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.xml.DocumentLoader;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A document loader for Spring that uses a {@link ClasspathResolver} for resolving schema information.
 */
public class SpringDocumentLoader implements DocumentLoader {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(SpringDocumentLoader.class);

    /** {@inheritDoc} */
    public Document loadDocument(InputSource inputSource, EntityResolver entityResolver, ErrorHandler errorHandler,
            int validationMode, boolean namespaceAware) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                "http://www.w3.org/2001/XMLSchema");
        factory.setCoalescing(true);
        factory.setIgnoringComments(true);
        factory.setNamespaceAware(true);
        factory.setValidating(true);

        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new LoggingErrorHandler(log));
        builder.setEntityResolver(new ClasspathResolver());
        return builder.parse(inputSource);
    }
    
    /**
     * A SAX error handler that logs errors a {@link Logger} before rethrowing them.
     */
    public class LoggingErrorHandler implements ErrorHandler{
        
        /** Error logger. */
        private Logger log;
        
        /**
         * Constructor.
         *
         * @param logger logger errors will be written to
         */
        public LoggingErrorHandler(Logger logger){
            log = logger;
        }

        /** {@inheritDoc} */
        public void error(SAXParseException exception) throws SAXException {
            log.trace("Error parsing XML", exception);
            throw exception;
        }

        /** {@inheritDoc} */
        public void fatalError(SAXParseException exception) throws SAXException {
            log.trace("Fatal XML parsing XML error", exception);
            throw exception;
        }

        /** {@inheritDoc} */
        public void warning(SAXParseException exception) throws SAXException {
            log.trace("XML parsing warning", exception);
            throw exception;
        }
    }
}