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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.shared.codec.HTMLEncoder;

import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

/** Service OrganizationName - directly from the metadata if present. */
public class OrganizationNameTag extends ServiceTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = -6896271567378071224L;

    /** Class logger. */
    @Nonnull private static Logger log = LoggerFactory.getLogger(OrganizationNameTag.class);

    /**
     * look for the &lt;OrganizationName&gt;.
     * 
     * @return null or an appropriate string
     */
    @Nullable private String getOrganizationName() {
        final RelyingPartyUIContext ctx = getRelyingPartyUIContext();
        if (ctx == null) {
            return null;
        }
        return ctx.getOrganizationName();
    }

    /** {@inheritDoc} */
    @Override public int doEndTag() throws JspException {

        final String name = getOrganizationName();

        try {
            if (null == name) {
                final BodyContent bc = getBodyContent();
                if (null != bc) {
                    final JspWriter ew = bc.getEnclosingWriter();
                    if (ew != null) {
                        bc.writeOut(ew);
                    }
                }
            } else {
                pageContext.getOut().print(HTMLEncoder.encodeForHTML(name));
            }
        } catch (final IOException e) {
            log.warn("Error generating OrganizationName: {}", e.getMessage());
            throw new JspException("EndTag", e);
        }
        return super.doEndTag();
    }

}