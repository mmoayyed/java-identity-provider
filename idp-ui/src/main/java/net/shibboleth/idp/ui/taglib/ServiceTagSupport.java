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

package net.shibboleth.idp.ui.taglib;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.servlet.jsp.tagext.BodyTagSupport;

import org.opensaml.messaging.context.navigate.ChildContextLookup;
import org.opensaml.profile.context.ProfileRequestContext;
import org.slf4j.Logger;

import net.shibboleth.shared.primitive.DeprecationSupport;
import net.shibboleth.shared.primitive.DeprecationSupport.ObjectType;
import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import net.shibboleth.idp.authn.context.AuthenticationContext;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.shared.codec.HTMLEncoder;

/**
 * Display the serviceName.
 * 
 * This is taken in order 1) From the mdui 2) AttributeConsumeService 3) HostName from the EntityId 4) EntityId.
 */
public class ServiceTagSupport extends BodyTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = 4405207268569727209L;

    /** Class logger. */
    @Nonnull private static Logger log = LoggerFactory.getLogger(ServiceTagSupport.class);

    /** Strategy function for access to {@link RelyingPartyUIContext} for input to resolver. */
    @SuppressWarnings("null")
    @Nonnull private static Function<ProfileRequestContext, RelyingPartyUIContext> uiContextLookupStrategy =
            new ChildContextLookup<>(RelyingPartyUIContext.class).compose(
                    new ChildContextLookup<>(AuthenticationContext.class));

    /** Bean storage. class reference */
    @Nullable private String cssClass;

    /** Bean storage. id reference */
    @Nullable private String cssId;

    /** Bean storage. style reference */
    @Nullable private String cssStyle;

    /** Cached RelyingPartyUIContext. */
    @Nullable private RelyingPartyUIContext relyingPartyUIContext;
    
    /** Constructor. */
    public ServiceTagSupport() {
        super();
        final String classname = getClass().getName();
        assert classname!=null;
        DeprecationSupport.atRiskOnce(ObjectType.CLASS, classname, "a jsp file");
    }

    /**
     * Sets the {@link RelyingPartyUIContext}.
     * @deprecated this has to be calculated every time
     * @param value what to set
     */
    @Deprecated public void setUiContext(@Nullable final RelyingPartyUIContext value) {
        relyingPartyUIContext = value;
    }

    /**
     * Set the Css class to use.
     * 
     * @param value what to set
     */
    public void setCssClass(@Nullable final String value) {
        cssClass = value;
    }

    /**
     * Get the Css class to use.
     * 
     * @param value what to set
     */
    public void setCssId(@Nullable final String value) {
        cssId = value;
    }

    /**
     * Set the Css style to use.
     * 
     * @param value what to set
     */
    public void setCssStyle(@Nullable final String value) {
        cssStyle = value;
    }

    /**
     * Add the class and Id (if present) to the string under construction.
     * 
     * @param sb the {@link StringBuilder} to add to.
     */
    protected void addClassAndId(@Nonnull final StringBuilder sb) {
        if (cssClass != null) {
            sb.append(" class=\"").append(cssClass).append('"');
        }
        if (cssId != null) {
            sb.append(" id=\"").append(cssId).append('"');
        }
        if (cssStyle != null) {
            sb.append(" style=\"").append(cssStyle).append('"');
        }
    }

    /**
     * Build a hyperlink from the parameters.
     * 
     * @param url the URL
     * @param text what to embed
     * @return the hyperlink.
     */
    @Nonnull protected String buildHyperLink(@Nonnull final String url, @Nullable final String text) {
        final String encodedUrl;

        try {
            final URI theUrl = new URI(url);
            final String scheme = theUrl.getScheme();

            if (!"http".equals(scheme) && !"https".equals(scheme) && !"mailto".equals(scheme)) {
                log.warn("The URL '{}' contained an invalid scheme.", url);
                return "";
            }
            encodedUrl = HTMLEncoder.encodeForHTMLAttribute(url);
        } catch (final URISyntaxException e) {
            //
            // It wasn't an URI.
            //
            log.warn("The URL '{}' was invalid: ", url, e);
            return "";
        }

        final StringBuilder sb = new StringBuilder("<a href=\"");
        sb.append(encodedUrl).append('"');
        addClassAndId(sb);
        sb.append(">").append(HTMLEncoder.encodeForHTML(text)).append("</a>");
        final String result = sb.toString();
        assert result != null;
        return result;
    }

    /**
     * Get the {@link RelyingPartyUIContext} for the request. We cache this if it exists (the usual case).
     * 
     * @return the context
     */
    @Nullable protected RelyingPartyUIContext getRelyingPartyUIContext() {

        if (null != relyingPartyUIContext) {
            return relyingPartyUIContext;
        }

        final HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        if (request == null) {
            return null;
        }

        final ProfileRequestContext pc = (ProfileRequestContext) request.getAttribute("profileRequestContext");
        return uiContextLookupStrategy.apply(pc);
    }

    /**
     * Get the identifier for the service name as per the rules above.
     * 
     * @return something sensible for display.
     */
    @Nullable protected String getServiceName() {

        final RelyingPartyUIContext ctx = getRelyingPartyUIContext();
        if (ctx == null) {
            return null;
        }
        return ctx.getServiceName();
    }

}