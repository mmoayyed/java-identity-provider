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

package net.shibboleth.idp.ui.taglib;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

import net.shibboleth.utilities.java.support.codec.HTMLEncoder;

import org.opensaml.saml.ext.saml2mdui.Logo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logo for the SP. */
public class ServiceLogoTag extends ServiceTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = 5309357312113020929L;

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(ServiceLogoTag.class);

    /** what to emit if the jsp has nothing. */
    private static final String DEFAULT_VALUE = "";

    /** what to emit as alt txt if all else fails. */
    private static final String DEFAULT_ALT_TXT = "SP Logo";

    /** Bean storage. Size constraint X */
    private int minWidth;

    /** Bean storage. Size constraint X */
    private int maxWidth = Integer.MAX_VALUE;

    /** Bean storage. Size constraint Y */
    private int minHeight;

    /** Bean storage. Size constraint Y */
    private int maxHeight = Integer.MAX_VALUE;

    /** Bean storage. alt text */
    private String altTxt;

    /**
     * Set the maximum width of the logo.
     * 
     * @param value what to set
     */
    public void setMaxWidth(Integer value) {
        maxWidth = value.intValue();
    }

    /**
     * Set the minimum width of the logo.
     * 
     * @param value what to set
     */
    public void setMinWidth(Integer value) {
        minWidth = value.intValue();
    }

    /**
     * Set the minimum height.
     * 
     * @param value what to set
     */
    public void setMinHeight(Integer value) {
        minHeight = value.intValue();
    }

    /**
     * Bean setter.
     * 
     * @param value what to set
     */
    public void setMaxHeight(Integer value) {
        maxHeight = value.intValue();
    }

    /**
     * Bean setter.
     * 
     * @param value what to set
     */
    public void setAlt(String value) {
        altTxt = value;
    }

    /**
     * Whether the provided logo fits inside the constraints.
     * 
     * @param logo the logo
     * @return whether it fits the provided max and mins
     */
    private boolean logoFits(Logo logo) {
        return logo.getHeight() <= maxHeight && logo.getHeight() >= minHeight && logo.getWidth() <= maxWidth
                && logo.getWidth() >= minWidth;
    }

    /**
     * get an appropriate logo by lanaguage from the UIInfo.
     * 
     * @param logos what to look through
     * @return an appropriate logo.
     */
    @Nullable private String getLogoFromUIInfo(final List<Logo> logos) {
        for (final String lang : getBrowserLanguages()) {
            // By language first
            for (final Logo logo : logos) {
                if (null == logo.getXMLLang()) {
                    continue;
                }
                log.debug("Found logo in UIInfo, language=" + logo.getXMLLang() + " width=" + logo.getWidth()
                        + " height=" + logo.getHeight());
                if (!logo.getXMLLang().equals(lang)) {
                    log.debug("Language mismatch against " + lang);
                    continue;
                }
                if (!logo.getXMLLang().equals(lang) || !logoFits(logo)) {
                    log.debug("Size Mismatch");
                    continue;
                }
                // Found it
                log.debug("returning logo from UIInfo " + logo.getURL());
                return logo.getURL();
            }
        }
        // Then by no language
        for (final Logo logo : getSPUIInfo().getLogos()) {
            if (null != logo.getXMLLang()) {
                continue;
            }
            log.debug("Found logo in UIInfo, width=" + logo.getWidth() + " height=" + logo.getHeight());
            if (logoFits(logo)) {
                // null language and it fits
                log.debug("returning logo from UIInfo " + logo.getURL());
                return logo.getURL();
            }
            log.debug("Mismatch for size");
        }
        return null;
    }

    /**
     * Get an appropriate Logo from UIInfo.
     * 
     * @return the URL for a logo
     * 
     */
    @Nullable private String getLogoFromUIInfo() {

        if (getSPUIInfo() != null && getSPUIInfo().getLogos() != null) {

            final String result = getLogoFromUIInfo(getSPUIInfo().getLogos());

            if (null != result) {
                return result;
            }
            log.debug("No appropriate logo in UIInfo");
        }
        return null;
    }

    /**
     * Find what the user specified for alt txt.
     * 
     * @return the text required
     */
    @Nullable private String getAltText() {

        //
        // First see what the user tried
        //
        String value = altTxt;
        if (null != value && 0 != value.length()) {
            return value;
        }

        //
        // Try the request
        //
        value = getServiceName();
        if (null != value && 0 != value.length()) {
            return value;
        }

        return DEFAULT_ALT_TXT;
    }

    /**
     * Given the url build an appropriate &lta href=...
     * 
     * @return the contrcuted hyperlink or null
     */
    @Nullable private String getHyperlink() {
        final String url = getLogoFromUIInfo();

        if (null == url) {
            return null;
        }

        try {
            final URI theUrl = new URI(url);
            final String scheme = theUrl.getScheme();

            if (!"http".equals(scheme) && !"https".equals(scheme) && !"data".equals(scheme)) {
                log.warn("The logo URL " + url + " contained an invalid scheme (expected http:, https: or data:)");
                return null;
            }
        } catch (URISyntaxException e) {
            //
            // Could not encode
            //
            log.warn("The logo URL " + url + " was not a URL " + e.toString());
            return null;
        }

        final String encodedURL = HTMLEncoder.encodeForHTMLAttribute(url);
        final String encodedAltTxt = HTMLEncoder.encodeForHTMLAttribute(getAltText());
        final StringBuilder sb = new StringBuilder("<img src=\"");
        sb.append(encodedURL).append('"');
        sb.append(" alt=\"").append(encodedAltTxt).append('"');
        addClassAndId(sb);
        sb.append("/>");
        return sb.toString();
    }

    /** {@inheritDoc} */
    @Override public int doEndTag() throws JspException {

        final String result = getHyperlink();

        try {
            if (null == result) {
                final BodyContent bc = getBodyContent();
                boolean written = false;
                if (null != bc) {
                    final JspWriter ew = bc.getEnclosingWriter();
                    if (ew != null) {
                        bc.writeOut(ew);
                        written = true;
                    }
                }
                if (!written) {
                    //
                    // No value provided put in our own hardwired default
                    //
                    pageContext.getOut().print(DEFAULT_VALUE);
                }
            } else {
                pageContext.getOut().print(result);
            }
        } catch (IOException e) {
            log.warn("Error generating Logo");
            throw new JspException("EndTag", e);
        }
        return super.doEndTag();
    }
}
