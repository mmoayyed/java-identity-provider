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

package edu.internet2.middleware.shibboleth.idp.tou;

import java.util.Random;
import java.util.UUID;

import org.joda.time.DateTime;
import org.testng.annotations.DataProvider;

import edu.vt.middleware.crypt.digest.SHA256;
import edu.vt.middleware.crypt.util.HexConverter;

public class TestData {
    
    private static final Random random = new Random();
    
    private static String getRandomUserId() {
        return String.valueOf(UUID.randomUUID());
    }
    
    private static String getRandomFingerprint() {
        byte[] bytes = new byte[4096];
        random.nextBytes(bytes);
        return new SHA256().digest(bytes, new HexConverter(true));
    }
    
    private static String getRandomVersion() {
        int i1 = random.nextInt(10)+1;
        int i2 = random.nextInt(10)+1;
        return i1 + "." + i2;
    }
    
    private static DateTime getRandomDate() {
        // Fri Jan 01 12:00:00 EST 2010 | Tue Feb 02 12:00:00 EST 2010 | Wed Mar 03 12:00:00 EST 2010
    	DateTime[] dates = {new DateTime(1262365200000L), new DateTime(1265130000000L), new DateTime(1267635600000L)};
        return dates[random.nextInt(dates.length)];
    }
        
    private static ToU createToU() {
        return new ToU(getRandomVersion(), getRandomFingerprint());
    }
    
    @DataProvider(name = "createAcceptedToU")
    public static Object[][] createAcceptedToU() {      
        return new Object[][] {
        new Object[] {getRandomUserId(), createToU(), getRandomDate(), getRandomDate()}
      };
    }
    
    @DataProvider(name = "containsAcceptedToU")
    public static Object[][] containsAcceptedToU() {      
        return new Object[][] {
        new Object[] {getRandomUserId(), createToU(), getRandomDate()}
      };
    }
    
}   