/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.shibboleth.idp.profile.impl;

import javax.annotation.Nonnull;

import org.opensaml.profile.action.ProfileAction;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.webflow.execution.Action;

import net.shibboleth.shared.component.ComponentInitializationException;

/**
 * Post-processes {@link ProfileAction} beans by wrapping them in a Spring Web Flow adaptor.
 */
public class ProfileActionBeanPostProcessor implements BeanPostProcessor {

    /** {@inheritDoc} */
    @Override
    public Object postProcessBeforeInitialization(final @Nonnull Object bean, final @Nonnull String beanName) {
        return bean;
    }

    /** {@inheritDoc} */
    @Override
    public Object postProcessAfterInitialization(final @Nonnull Object bean, final @Nonnull String beanName) {
        if (bean instanceof ProfileAction && !(bean instanceof Action)) {
            final WebFlowProfileActionAdaptor wrapper = new WebFlowProfileActionAdaptor((ProfileAction) bean);
            try {
                wrapper.initialize();
            } catch (final ComponentInitializationException e) {
                throw new BeanCreationException("WebFlowProfileActionAdaptor failed to initialize around ProfileAction "
                        + beanName, e);
            }
            return wrapper;
        }
        return bean;
    }
}