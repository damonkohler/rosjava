/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.node.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Maps;

import org.junit.Before;
import org.junit.Test;
import org.ros.internal.namespace.DefaultGraphName;
import org.ros.namespace.GraphName;

import java.util.Collection;
import java.util.Map;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ParameterServerTest {

  private ParameterServer server;

  @Before
  public void setup() {
    server = new ParameterServer();
  }

  @Test
  public void testGetNonExistent() {
    assertEquals(null, server.get(new DefaultGraphName("/foo")));
    assertEquals(null, server.get(new DefaultGraphName("/foo/bar")));
  }

  @Test
  public void testSetAndGetShallow() {
    server.set(new DefaultGraphName("/foo"), "bloop");
    assertEquals("bloop", server.get(new DefaultGraphName("/foo")));
  }

  @Test
  public void testSetAndGetDeep() {
    server.set(new DefaultGraphName("/foo/bar"), "bloop");
    assertEquals("bloop", server.get(new DefaultGraphName("/foo/bar")));
  }

  @Test
  public void testSetAndGet() {
    server.set(new DefaultGraphName("/foo"), "bloop");
    assertEquals("bloop", server.get(new DefaultGraphName("/foo")));
    server.set(new DefaultGraphName("/foo/bar"), "bloop");
    assertEquals("bloop", server.get(new DefaultGraphName("/foo/bar")));
    server.set(new DefaultGraphName("/foo/bar/baz"), "bloop");
    assertEquals("bloop", server.get(new DefaultGraphName("/foo/bar/baz")));
  }

  @Test
  public void testSetDeepAndGetShallow() {
    server.set(new DefaultGraphName("/foo/bar"), "bloop");
    Map<String, Object> expected = Maps.newHashMap();
    expected.put("bar", "bloop");
    assertEquals(expected, server.get(new DefaultGraphName("/foo")));
  }

  @Test
  public void testSetOverwritesMap() {
    server.set(new DefaultGraphName("/foo/bar"), "bloop");
    assertEquals("bloop", server.get(new DefaultGraphName("/foo/bar")));
    server.set(new DefaultGraphName("/foo"), "bloop");
    assertEquals("bloop", server.get(new DefaultGraphName("/foo")));
  }

  @Test
  public void testSetAndGetFloat() {
    GraphName name = new DefaultGraphName("/foo/bar");
    server.set(name, 0.42f);
    assertEquals(0.42, (Double) server.get(name), 0.1);
  }

  @Test
  public void testDeleteShallow() {
    GraphName name = new DefaultGraphName("/foo");
    server.set(name, "bloop");
    server.delete(name);
    assertEquals(null, server.get(name));
  }

  @Test
  public void testDeleteDeep() {
    GraphName name = new DefaultGraphName("/foo/bar");
    server.set(name, "bloop");
    server.delete(name);
    assertEquals(null, server.get(name));
  }

  @Test
  public void testHas() {
    server.set(new DefaultGraphName("/foo/bar/baz"), "bloop");
    assertTrue(server.has(new DefaultGraphName("/foo/bar/baz")));
    assertTrue(server.has(new DefaultGraphName("/foo/bar")));
    assertTrue(server.has(new DefaultGraphName("/foo")));
    assertTrue(server.has(new DefaultGraphName("/")));
  }

  @Test
  public void testGetNames() {
    GraphName name1 = new DefaultGraphName("/foo/bar/baz");
    server.set(name1, "bloop");
    GraphName name2 = new DefaultGraphName("/testing");
    server.set(name2, "123");
    Collection<GraphName> names = server.getNames();
    assertEquals(2, names.size());
    assertTrue(names.contains(name1));
    assertTrue(names.contains(name2));
  }

}
