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

package net.shibboleth.idp.spring;

import javax.annotation.Nonnull;

import net.shibboleth.utilities.java.support.component.IdentifiableComponent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Pre-processes {@link IdentifiableComponent} beans by setting the bean ID to the bean name.
 */
public class IdentifiableBeanPostProcessor implements BeanPostProcessor {

    /** Logger. */
    @Nonnull private final Logger log = LoggerFactory.getLogger(IdentifiableBeanPostProcessor.class);

    /** {@inheritDoc} */
    @Override public Object postProcessBeforeInitialization(Object bean, String beanName) {
        if (bean instanceof IdentifiableComponent) {
            final IdentifiableComponent component = (IdentifiableComponent) bean;
            if (component.getId() == null) {
                component.setId(beanName);
            } else if (log.isDebugEnabled()) {
                if (component.getId().equals(beanName)) {
                    log.debug("The 'id' property is redundant for bean with 'id' attribute '{}'", beanName);
                } else {
                    log.debug("The 'id' property is not the same as the 'id' attribute for bean '{}'", beanName);
                }
            }
        }
        return bean;
    }

    /** {@inheritDoc} */
    @Override public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

}
