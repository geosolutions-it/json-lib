/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2006-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kordamp.json.util;

import junit.framework.TestCase;
import org.kordamp.json.JSONFunction;
import org.kordamp.json.JSONException;
import org.kordamp.json.JSONObject;

/**
 * @author Andres Almiray
 */
public class TestJSONStringer extends TestCase {
    private String originalMaxDepth;

    public TestJSONStringer(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestJSONStringer.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        originalMaxDepth = System.getProperty("json.maxDepth");
        JSONBuilder.reloadMaxDepth();
    }

    @Override
    protected void tearDown() throws Exception {
        if (originalMaxDepth == null) {
            System.clearProperty("json.maxDepth");
        } else {
            System.setProperty("json.maxDepth", originalMaxDepth);
        }
        JSONBuilder.reloadMaxDepth();
        super.tearDown();
    }

    public void testCreateArray() {
        JSONBuilder b = new JSONStringer().array()
            .value(true)
            .value(1.1d)
            .value(2L)
            .value("text")
            .endArray();
        assertEquals("[true,1.1,2,\"text\"]", b.toString());
    }

    public void testCreateEmptyArray() {
        JSONBuilder b = new JSONStringer().array()
            .endArray();
        assertEquals("[]", b.toString());
    }

    public void testCreateEmptyArrayWithNullObjects() {
        JSONBuilder b = new JSONStringer().array()
            .value(null)
            .value(null)
            .endArray();
        assertEquals("[null,null]", b.toString());
    }

    public void testCreateEmptyObject() {
        JSONBuilder b = new JSONStringer().object()
            .endObject();
        assertEquals("{}", b.toString());
    }

    public void testCreateFunctionArray() {
        JSONBuilder b = new JSONStringer().array()
            .value(new JSONFunction("var a = 1;"))
            .value(new JSONFunction("var b = 2;"))
            .endArray();
        assertEquals("[function(){ var a = 1; },function(){ var b = 2; }]", b.toString());
    }

    public void testCreateSimpleObject() {
        JSONBuilder b = new JSONStringer().object()
            .key("bool")
            .value(true)
            .key("numDouble")
            .value(1.1d)
            .key("numInt")
            .value(2)
            .key("text")
            .value("text")
            .key("func")
            .value(new JSONFunction("var a = 1;"))
            .endObject();
        JSONObject jsonObj = JSONObject.fromObject(b.toString());
        assertEquals(Boolean.TRUE, jsonObj.get("bool"));
        assertEquals(new Double(1.1d), jsonObj.get("numDouble"));
        assertEquals(new Long(2).longValue(), ((Number) jsonObj.get("numInt")).longValue());
        assertEquals("text", jsonObj.get("text"));
        assertTrue(JSONUtils.isFunction(jsonObj.get("func")));
        assertEquals("function(){ var a = 1; }", jsonObj.get("func")
            .toString());
    }

    public void testConfiguredMaxDepthIsApplied() {
        System.setProperty("json.maxDepth", "30");
        JSONBuilder.reloadMaxDepth();

        JSONBuilder builder = new JSONStringer();
        for (int i = 0; i < 30; i++) {
            builder.array();
        }
        builder.value(1);
        for (int i = 0; i < 30; i++) {
            builder.endArray();
        }

        try {
            JSONBuilder overflow = new JSONStringer();
            for (int i = 0; i < 31; i++) {
                overflow.array();
            }
            fail("Expected depth overflow at 31");
        } catch (JSONException e) {
            assertEquals("Nesting too deep.", e.getMessage());
        }
    }
}
