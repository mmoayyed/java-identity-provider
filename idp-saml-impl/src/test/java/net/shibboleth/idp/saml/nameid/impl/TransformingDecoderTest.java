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

package net.shibboleth.idp.saml.nameid.impl;

import java.util.Arrays;

import net.shibboleth.shared.collection.CollectionSupport;
import net.shibboleth.shared.collection.Pair;
import net.shibboleth.shared.component.UninitializedComponentException;

import org.testng.Assert;
import org.testng.annotations.Test;

/** {@link BaseTransformingDecoder} unit test. */
@SuppressWarnings({"javadoc", "null"})
public class TransformingDecoderTest {

    private static final String PRINCIPAL="ThePrincipalName@foo.edu";

    @Test(expectedExceptions={UninitializedComponentException.class,})
    public void testNoinit() throws Exception {
        final MockTransformingDecoder decode = new MockTransformingDecoder();
        decode.setId("Decoder");
        
        decode.decode(PRINCIPAL);
    }
    
    @Test public void testEcho() throws Exception {
        final MockTransformingDecoder decode = new MockTransformingDecoder();
        decode.setId("Decoder");
        decode.initialize();
        
        Assert.assertEquals(decode.decode(PRINCIPAL), PRINCIPAL);
    }

    @Test public void testStrip() throws Exception {
        final MockTransformingDecoder decode = new MockTransformingDecoder();
        decode.setId("Decoder");
        decode.setTransforms(CollectionSupport.singletonList(new Pair<>("(.+)@foo.edu", "$1")));
        decode.initialize();
        
        Assert.assertEquals(decode.decode(PRINCIPAL), "ThePrincipalName");
    }

    @Test public void testStripAndExtract() throws Exception {
        final MockTransformingDecoder decode = new MockTransformingDecoder();
        decode.setId("Decoder");
        decode.setTransforms(Arrays.asList(
                new Pair<>("(.+)@foo.edu", "$1"),
                new Pair<>("([A-Z][a-z]+)([A-Z][a-z]+)([A-Z][a-z]+)", "$2")));
        decode.initialize();
        
        Assert.assertEquals(decode.decode(PRINCIPAL), "Principal");
    }
    
    private class MockTransformingDecoder extends BaseTransformingDecoder {
        
    }
    
}