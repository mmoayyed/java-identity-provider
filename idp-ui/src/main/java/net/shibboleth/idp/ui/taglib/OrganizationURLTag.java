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

import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.shared.primitive.LoggerFactory;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;

/** Service OrganizationURL - directly from the metadata if present. */
public class OrganizationURLTag extends ServiceTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = 5633365955540356312L;

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(OrganizationURLTag.class);

    /** Bean storage for the link text attribute. */
    private static String linkText;

    /**
     * Bean setter for the link text attribute.
     * 
     * @param text the link text to put in
     */
    public void setLinkText(final String text) {
        linkText = text;
    }

    /**
     * look for the &lt;OrganizationURL&gt;.
     * 
     * @return null or an appropriate string
     */
    @Nullable private String getOrganizationURL() {
        final RelyingPartyUIContext ctx = getRelyingPartyUIContext();
        if (ctx == null) {
            return null;
        }
        return ctx.getOrganizationURL();
    }

    /** {@inheritDoc} */
    @Override public int doEndTag() throws JspException {

        final String orgURL = getOrganizationURL();

        try {
            if (null == orgURL) {
                final BodyContent bc = getBodyContent();
                if (null != bc) {
                    final JspWriter ew = bc.getEnclosingWriter();
                    if (ew != null) {
                        bc.writeOut(ew);
                    }
                }
            } else {
                pageContext.getOut().print(buildHyperLink(orgURL, linkText));
            }
        } catch (final IOException e) {
            log.warn("Error generating OrganizationURL: {}", e.getMessage());
            throw new JspException("EndTag", e);
        }
        return super.doEndTag();
    }
}