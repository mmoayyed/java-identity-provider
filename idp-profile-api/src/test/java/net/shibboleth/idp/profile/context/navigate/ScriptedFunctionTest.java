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

package net.shibboleth.idp.profile.context.navigate;

import java.lang.reflect.InvocationTargetException;

import javax.script.ScriptException;

import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 */
@SuppressWarnings("javadoc")
public class ScriptedFunctionTest {
    
    static final String STRING_RETURN = "JavaString=Java.type(\"java.lang.String\"); new JavaString(\"String\");";
    static final String INTEGER_RETURN = "JavaInteger=Java.type(\"java.lang.Integer\"); new JavaInteger(37);";
       
    @Test public void simpleScript() throws ScriptException {
        final ProfileRequestContext prc = new ProfileRequestContext();
        
        final Object string = ScriptedContextLookupFunction.inlineScript(STRING_RETURN).apply(prc);

        String s = (String) string;
        Assert.assertEquals(s, "String");
        
        final Integer integer = (Integer) ScriptedContextLookupFunction.inlineScript(INTEGER_RETURN).apply(prc);
        Assert.assertEquals(integer.intValue(), 37);
    }
    
    @Test public void custom() throws ScriptException {
        final ProfileRequestContext prc = new ProfileRequestContext();
        
        final ScriptedContextLookupFunction<ProfileRequestContext> script = ScriptedContextLookupFunction.inlineScript("custom;");
        script.setCustomObject("String");
        Assert.assertEquals(script.apply(prc), "String");
 
        script.setCustomObject(Integer.valueOf(37));
        Assert.assertEquals(script.apply(prc), Integer.valueOf(37));
    }    
    
    
    @Test public void withType() throws ScriptException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        final ProfileRequestContext prc = new ProfileRequestContext();

        final ScriptedContextLookupFunction<ProfileRequestContext> script1 = ScriptedContextLookupFunction.inlineScript(STRING_RETURN, Object.class);
        
        final String string = (String) script1.apply(prc);
        Assert.assertEquals(string, "String");
        
        Assert.assertEquals(ScriptedContextLookupFunction.inlineScript(STRING_RETURN, String.class).apply(prc), "String");
        
        Assert.assertNull(ScriptedContextLookupFunction.inlineScript(STRING_RETURN, Integer.class).apply(prc));
        
        final Integer integer = (Integer) ScriptedContextLookupFunction.inlineScript(INTEGER_RETURN).apply(prc);
        Assert.assertEquals(integer.intValue(), 37);
        
    }

    @Test public void messageContext() throws ScriptException {
        final ScriptedContextLookupFunction<MessageContext> script1 = ScriptedContextLookupFunction.inlineMessageContextScript(STRING_RETURN, Object.class);
        
        Assert.assertEquals(script1.apply(new MessageContext()), "String");
        Assert.assertEquals(script1.apply(null), "String");
    }
}
