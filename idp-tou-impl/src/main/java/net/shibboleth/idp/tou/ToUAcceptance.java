/*
 * Copyright 2010 University Corporation for Advanced Internet Development, Inc.
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

package net.shibboleth.idp.tou;
import org.joda.time.DateTime;

import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

/**
 *
 */
public class ToUAcceptance {
    private final String version;
    private final String fingerprint;
    private final DateTime acceptanceDate;
    
    public ToUAcceptance(final String version, final String fingerprint, final DateTime acceptanceDate) {
        this.version = version;
        this.fingerprint = fingerprint;
        this.acceptanceDate = acceptanceDate;
    }
    
    private ToUAcceptance(final ToU tou, final DateTime acceptanceDate) {
        this.version = tou.getVersion();
        this.fingerprint = fingerprint(tou.getText());
        this.acceptanceDate = acceptanceDate;
    }
    
    public String getVersion() {
        return version;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public DateTime getAcceptanceDate() {
        return acceptanceDate;
    }

    public static ToUAcceptance createToUAcceptance(final ToU tou, final DateTime acceptanceDate) {
        return new ToUAcceptance(tou, acceptanceDate);
    }
    
    public static ToUAcceptance emptyToUAcceptance() {
        return new ToUAcceptance("", "", null);
    }

    public boolean contains(final ToU tou) {
        return version.equals(tou.getVersion()) && 
            fingerprint.equals(fingerprint(tou.getText()));
    }
    
    static String fingerprint(String text) {
        return new SHA256().digest(text.getBytes(), new HexConverter(true));
    }
}
