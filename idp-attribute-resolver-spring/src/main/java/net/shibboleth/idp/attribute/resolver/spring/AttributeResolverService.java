
package net.shibboleth.idp.attribute.resolver.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.annotation.Nonnull;

import net.shibboleth.idp.attribute.resolver.AttributeResolutionContext;
import net.shibboleth.idp.attribute.resolver.AttributeResolver;
import net.shibboleth.idp.attribute.resolver.BaseAttributeDefinition;
import net.shibboleth.idp.attribute.resolver.ResolutionException;
import net.shibboleth.idp.service.AbstractSpringService;
import net.shibboleth.idp.service.ServiceException;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.resource.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

// TODO incomplete
/**
 * The attribute resolver service manages the lifecycle of an attribute resolver, where the lifecycle comprises
 * starting, stopping, and configuration reloading.
 */
public class AttributeResolverService extends AbstractSpringService {

    /** Class logger. */
    private final Logger log = LoggerFactory.getLogger(AttributeResolverService.class);

    /** The attribute resolver. */
    private AttributeResolver attributeResolver;

    /**
     * TODO finish
     * 
     * The service proxies the underlying components functionality.
     * 
     * @param resolutionContext
     * @throws ResolutionException
     */
    public void resolveAttributes(@Nonnull final AttributeResolutionContext resolutionContext)
            throws ResolutionException {
        attributeResolver.resolveAttributes(resolutionContext);
    }

    protected void doPreStart(HashMap context) throws ServiceException {

        // TODO probably wrong

        // TODO do we have to init resources here ?
        log.debug("getServiceConfigurations() '{}'", getServiceConfigurations());
        for (Resource resource : this.getServiceConfigurations()) {
            try {
                log.debug("initializing resource '{}'", resource);
                resource.initialize();
            } catch (ComponentInitializationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new ServiceException(e);
            }
        }

        super.doPreStart(context);

        GenericApplicationContext appCtx = (GenericApplicationContext) context.get(APP_CTX_CTX_KEY);

        Collection<BaseAttributeDefinition> definitions = new ArrayList<BaseAttributeDefinition>();
        String[] beanNames = appCtx.getBeanNamesForType(BaseAttributeDefinition.class);
        log.debug("Loading {} attribute definitions", beanNames.length);
        for (String beanName : beanNames) {
            BaseAttributeDefinition aDefinition = (BaseAttributeDefinition) appCtx.getBean(beanName);
            aDefinition.setId(beanName);
            definitions.add(aDefinition);
        }

        attributeResolver = new AttributeResolver("resolverId", definitions, null);

        try {
            attributeResolver.initialize();
        } catch (ComponentInitializationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
