/*
 * Copyright 2008 University Corporation for Advanced Internet Development, Inc.
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

package edu.internet2.middleware.shibboleth.common.config.service;

import java.util.Collection;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

import edu.internet2.middleware.shibboleth.common.service.Service;
import edu.internet2.middleware.shibboleth.common.service.ServiceException;

/** A simple service that exports Spring beans into the Servlet context as an attribute. */
public class ServletContextAttributeExporter implements Service, ApplicationContextAware, BeanNameAware {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(ServletContextAttributeExporter.class);

    /** Application context into which we're loaded. */
    private ApplicationContext appCtx;

    /** ID of this service. */
    private String id;

    /** Whether this service has been initialized. */
    private boolean initialized;

    /** ID of beans exported into the servlet context. */
    private Collection<String> exportedBeans;

    /**
     * Constructor.
     * 
     * @param beans ID of beans exported into the servlet context
     */
    public ServletContextAttributeExporter(Collection<String> beans) {
        exportedBeans = beans;
    }

    /** {@inheritDoc} */
    public void destroy() throws ServiceException {

    }

    /** {@inheritDoc} */
    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public void initialize() throws ServiceException {
        if (!(appCtx instanceof WebApplicationContext)) {
            log.warn("This service may only be used when services are loaded within a web application context.");
            return;
        }

        Object bean;
        if (exportedBeans != null) {
            WebApplicationContext webAppCtx = (WebApplicationContext) appCtx;
            ServletContext servletCtx = webAppCtx.getServletContext();
            for (String beanId : exportedBeans) {
                bean = webAppCtx.getBean(beanId);
                if(bean != null){
                    log.debug("Exporting bean {} to servlet context.", beanId);
                    servletCtx.setAttribute(beanId, bean);
                }else{
                    log.warn("No {} bean located, unable to export it to the servlet context", beanId);
                }
            }
        }

        initialized = true;
    }

    /** {@inheritDoc} */
    public boolean isInitialized() {
        return initialized;
    }

    /** {@inheritDoc} */
    public void setApplicationContext(ApplicationContext context) {
        appCtx = context;
    }

    /** {@inheritDoc} */
    public void setBeanName(String name) {
        id = name;
    }

    /** {@inheritDoc} */
    public boolean isDestroyed() {
        return false;
    }
}