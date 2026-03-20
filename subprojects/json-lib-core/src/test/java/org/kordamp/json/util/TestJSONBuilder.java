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

import java.io.StringWriter;

/**
 * @author Andres Almiray
 */
public class TestJSONBuilder extends TestCase {
    private String originalMaxDepth;

    public TestJSONBuilder(String testName) {
        super(testName);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(TestJSONBuilder.class);
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
        StringWriter w = new StringWriter();
        new JSONBuilder(w).array()
            .value(true)
            .value(1.1d)
            .value(2L)
            .value("text")
            .endArray();
        assertEquals("[true,1.1,2,\"text\"]", w.toString());
    }

    public void testCreateEmptyArray() {
        StringWriter w = new StringWriter();
        new JSONBuilder(w).array()
            .endArray();
        assertEquals("[]", w.toString());
    }

    public void testCreateEmptyArrayWithNullObjects() {
        StringWriter w = new StringWriter();
        new JSONBuilder(w).array()
            .value(null)
            .value(null)
            .endArray();
        assertEquals("[null,null]", w.toString());
    }

    public void testCreateEmptyObject() {
        StringWriter w = new StringWriter();
        new JSONBuilder(w).object()
            .endObject();
        assertEquals("{}", w.toString());
    }

    public void testCreateFunctionArray() {
        StringWriter w = new StringWriter();
        new JSONBuilder(w).array()
            .value(new JSONFunction("var a = 1;"))
            .value(new JSONFunction("var b = 2;"))
            .endArray();
        assertEquals("[function(){ var a = 1; },function(){ var b = 2; }]", w.toString());
    }

    public void testCreateSimpleObject() {
        StringWriter w = new StringWriter();
        new JSONBuilder(w).object()
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
        JSONObject jsonObj = JSONObject.fromObject(w.toString());
        assertEquals(Boolean.TRUE, jsonObj.get("bool"));
        assertEquals(new Double(1.1d), jsonObj.get("numDouble"));
        assertEquals(new Long(2).longValue(), ((Number) jsonObj.get("numInt")).longValue());
        assertEquals("text", jsonObj.get("text"));
        assertTrue(JSONUtils.isFunction(jsonObj.get("func")));
        assertEquals("function(){ var a = 1; }", jsonObj.get("func")
            .toString());
    }

    public void testDefaultMaxDepthIs100() {
        System.clearProperty("json.maxDepth");
        JSONBuilder.reloadMaxDepth();
        assertEquals(100, JSONBuilder.getMaxDepth());

        assertCanBuildArrayDepth(100);
        assertCannotBuildArrayDepth(101);
    }

    public void testConfiguredMaxDepth() {
        System.setProperty("json.maxDepth", "30");
        JSONBuilder.reloadMaxDepth();
        assertEquals(30, JSONBuilder.getMaxDepth());

        assertCanBuildArrayDepth(30);
        assertCannotBuildArrayDepth(31);
    }

    public void testConfiguredMaxDepthFallsBackForNonPositiveValues() {
        System.setProperty("json.maxDepth", "0");
        JSONBuilder.reloadMaxDepth();
        assertEquals(100, JSONBuilder.getMaxDepth());
    }

    public void testConfiguredMaxDepthFallsBackForBadFormat() {
        System.setProperty("json.maxDepth", "bad-value");
        JSONBuilder.reloadMaxDepth();
        assertEquals(100, JSONBuilder.getMaxDepth());
    }

    public void testConfiguredMaxDepthIsClamped() {
        System.setProperty("json.maxDepth", "2000000000");
        JSONBuilder.reloadMaxDepth();
        assertEquals(10_000, JSONBuilder.getMaxDepth());
    }

    public void testConfiguredLargeMaxDepthIsClamped() {
        System.setProperty("json.maxDepth", "9999999");
        JSONBuilder.reloadMaxDepth();
        assertEquals(10_000, JSONBuilder.getMaxDepth());
    }

    private void assertCanBuildArrayDepth(int depth) {
        StringWriter w = new StringWriter();
        JSONBuilder builder = new JSONBuilder(w);
        for (int i = 0; i < depth; i++) {
            builder.array();
        }
        builder.value(1);
        for (int i = 0; i < depth; i++) {
            builder.endArray();
        }
    }

    private void assertCannotBuildArrayDepth(int depth) {
        StringWriter w = new StringWriter();
        JSONBuilder builder = new JSONBuilder(w);

        try {
            for (int i = 0; i < depth; i++) {
                builder.array();
            }
            fail("Expected depth overflow at " + depth);
        } catch (JSONException e) {
            assertEquals("Nesting too deep.", e.getMessage());
        }
    }
}
