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

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;
import net.shibboleth.idp.ui.context.RelyingPartyUIContext;
import net.shibboleth.shared.primitive.LoggerFactory;

/** Service PrivacyURL - directly from the metadata if present. */
public class ServicePrivacyURLTag extends ServiceTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = -5857694815588394787L;

    /** Class logger. */
    @Nonnull private static Logger log = LoggerFactory.getLogger(ServicePrivacyURLTag.class);

    /** Bean storage for the link text attribute. */
    @Nullable private static String linkText;

    /**
     * Bean setter for the link text attribute.
     * 
     * @param text the link text to put in
     */
    public void setLinkText(@Nullable final String text) {
        linkText = text;
    }

    /**
     * look for the &lt;PrivacyURL&gt; in the &lt;UIInfo&gt;.
     * 
     * @return null or an appropriate string.
     */
    @Nullable private String getPrivacyURLFromUIIinfo() {
        final RelyingPartyUIContext ctx = getRelyingPartyUIContext();
        if (ctx == null) {
            return null;
        }
        return ctx.getPrivacyStatementURL();
    }

    @Override public int doEndTag() throws JspException {

        final String privacyURL = getPrivacyURLFromUIIinfo();

        try {
            if (null == privacyURL) {
                final BodyContent bc = getBodyContent();
                if (null != bc) {
                    final JspWriter ew = bc.getEnclosingWriter();
                    if (ew != null) {
                        bc.writeOut(ew);
                    }
                }
            } else {
                pageContext.getOut().print(buildHyperLink(privacyURL, linkText));
            }
        } catch (final IOException e) {
            log.warn("Error generating PrivacyStatementURL: {}", e.getMessage());
            throw new JspException("EndTag", e);
        }
        return super.doEndTag();
    }

}