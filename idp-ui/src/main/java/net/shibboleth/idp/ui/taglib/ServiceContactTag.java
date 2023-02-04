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
import java.util.List;

import javax.annotation.Nullable;
import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.BodyContent;
import net.shibboleth.shared.codec.HTMLEncoder;

import org.opensaml.saml.saml2.metadata.ContactPerson;
import org.opensaml.saml.saml2.metadata.ContactPersonTypeEnumeration;
import org.opensaml.saml.saml2.metadata.EmailAddress;
import org.opensaml.saml.saml2.metadata.GivenName;
import org.opensaml.saml.saml2.metadata.SurName;
import org.slf4j.Logger;
import net.shibboleth.shared.primitive.LoggerFactory;

/** return the contactInfo for the SP or null. */
public class ServiceContactTag extends ServiceTagSupport {

    /** Serial ID. */
    private static final long serialVersionUID = 5437171915434315671L;

    /** Class logger. */
    private static Logger log = LoggerFactory.getLogger(ServiceContactTag.class);

    /** storage for the contactType bean. */
    private ContactPersonTypeEnumeration contactType = ContactPersonTypeEnumeration.SUPPORT;

    /** bean storage for the name attribute. */
    private String contactName;

    /**
     * Setter for the contactType bean.
     * 
     * @param type in value
     */
    public void setContactType(@Nullable final String type) {
        if (null == type || 0 == type.length()) {
            log.warn("no parameter provided to contactType");
            return;
        }
        if (type.equals(ContactPersonTypeEnumeration.ADMINISTRATIVE.toString())) {
            contactType = ContactPersonTypeEnumeration.ADMINISTRATIVE;
        } else if (type.equals(ContactPersonTypeEnumeration.BILLING.toString())) {
            contactType = ContactPersonTypeEnumeration.BILLING;
        } else if (type.equals(ContactPersonTypeEnumeration.OTHER.toString())) {
            contactType = ContactPersonTypeEnumeration.OTHER;
        } else if (type.equals(ContactPersonTypeEnumeration.SUPPORT.toString())) {
            contactType = ContactPersonTypeEnumeration.SUPPORT;
        } else if (type.equals(ContactPersonTypeEnumeration.TECHNICAL.toString())) {
            contactType = ContactPersonTypeEnumeration.TECHNICAL;
        } else {
            log.warn("parameter provided to contactType:" + type + " is invalid");
        }
    }

    /**
     * Set the contact name.
     * 
     * @param name new value
     */
    public void setName(@Nullable final String name) {
        contactName = name;
    }

    /**
     * Return either the name raw or garnished in a hyperlink.
     * 
     * @param email the email address (a url)
     * @param name the name to return.
     * @return either a hyperlink or a raw string, or null
     */
    @Nullable private String buildURL(@Nullable final String email, @Nullable final String name) {
        // We have emailAdress or null and a non empty fullName.
        if (null != email) {
            // Nonempty email. Construct an href
            log.debug("constructing hyperlink from name '{}' and email '{}'", name, email);
            return buildHyperLink(email, name);
        }
        log.debug("no email found, using name '{}' with no hyperlink", name);

        if (null != name) {
            return HTMLEncoder.encodeForHTML(name);
        }
        return null;
    }

    /**
     * Build an appropriate string from the &lt;Contact&gt;.
     * 
     * @param contact who we are interested in.
     * @return either an hyperlink or straight text or null
     */
    @Nullable private String getStringFromContact(final ContactPerson contact) {
        final List<EmailAddress> emails = contact.getEmailAddresses();
        String emailAddress = null;

        if (emails != null && !emails.isEmpty()) {
            emailAddress = emails.get(0).getURI();
        }

        if (null != contactName) {
            return buildURL(emailAddress, contactName);
        }
        final SurName surName = contact.getSurName();
        final GivenName givenName = contact.getGivenName();
        final StringBuilder fullName = new StringBuilder();
        if (null != givenName) {
            fullName.append(givenName.getValue()).append(" ");
        }
        if (null != surName) {
            fullName.append(surName.getValue()).append(" ");
        }
        if (0 == fullName.length()) {
            if (null == emails) {
                log.debug("No name and no email");
                return null;
            }
            log.debug("no names found, using email address as text");
            fullName.append(emailAddress);
        }
        return buildURL(emailAddress, fullName.toString());
    }

    /**
     * Build an appropriate string from the &lt;EntityDescriptor&gt;.
     * 
     * @return either a hyperlink or straight text or null.
     */
    @Nullable protected String getContactFromEntity() {

        if (getRelyingPartyUIContext() == null) {
            return null;
        }
        final ContactPerson contact = getRelyingPartyUIContext().getContactPerson(contactType);
        if (null == contact) {
            return null;
        }
        return getStringFromContact(contact);

    }

    /** {@inheritDoc} */
    @Override public int doEndTag() throws JspException {

        final String result = getContactFromEntity();

        try {
            if (null == result) {
                final BodyContent bc = getBodyContent();
                if (null != bc) {
                    final JspWriter ew = bc.getEnclosingWriter();
                    if (ew != null) {
                        bc.writeOut(ew);
                    }
                }
            } else {
                pageContext.getOut().print(result);
            }
        } catch (final IOException e) {
            log.warn("Error generating contact: {}", e.getMessage());
            throw new JspException("EndTag", e);
        }
        return super.doEndTag();
    }
}
