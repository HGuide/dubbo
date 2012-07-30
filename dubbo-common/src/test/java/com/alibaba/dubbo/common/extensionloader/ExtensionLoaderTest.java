/*
 * Copyright 1999-2011 Alibaba Group.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.dubbo.common.extensionloader;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.containsString;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extensionloader.activate.ActivateExt1;
import com.alibaba.dubbo.common.extensionloader.activate.impl.ActivateExt1Impl1;
import com.alibaba.dubbo.common.extensionloader.activate.impl.GroupActivateExtImpl;
import com.alibaba.dubbo.common.extensionloader.activate.impl.OrderActivateExtImpl1;
import com.alibaba.dubbo.common.extensionloader.activate.impl.OrderActivateExtImpl2;
import com.alibaba.dubbo.common.extensionloader.activate.impl.ValueActivateExtImpl;
import com.alibaba.dubbo.common.extensionloader.ext1.impl.SimpleExtImpl1;
import com.alibaba.dubbo.common.extensionloader.ext1.impl.SimpleExtImpl_ManualAdd;
import com.alibaba.dubbo.common.extensionloader.ext6_wrap.WrappedExt;
import com.alibaba.dubbo.common.extensionloader.ext7.InitErrorExt;
import junit.framework.Assert;

import org.junit.Test;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.extension.ExtensionLoader;
import com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt;
import com.alibaba.dubbo.common.extensionloader.ext1.impl.SimpleExtImpl2;
import com.alibaba.dubbo.common.extensionloader.ext2.Ext2;
import com.alibaba.dubbo.common.extensionloader.ext6_wrap.impl.Ext5Wrapper1;
import com.alibaba.dubbo.common.extensionloader.ext6_wrap.impl.Ext5Wrapper2;

/**
 * @author ding.lid
 */
public class ExtensionLoaderTest {

    @Test
    public void test_getExtensionLoader_NotInterface() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(ExtensionLoaderTest.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    containsString("Extension type(class com.alibaba.dubbo.common.extensionloader.ExtensionLoaderTest) is not interface"));

        }
    }

    @Test
    public void test_getExtensionLoader_NotSpiAnnotation() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(NoSpiExt.class);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(),
                    allOf(containsString("com.alibaba.dubbo.common.extensionloader.NoSpiExt"),
                            containsString("is not extension"),
                            containsString("WITHOUT @SPI Annotation")));

        }
    }

    @Test
    public void test_getDefaultExtension() throws Exception {
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getDefaultExtension();
        assertThat(ext, instanceOf(SimpleExtImpl1.class));
        
        String name = ExtensionLoader.getExtensionLoader(SimpleExt.class).getDefaultExtensionName();
        assertEquals("impl1", name);
    }
    
    @Test
    public void test_getDefaultExtension_NULL() throws Exception {
        Ext2 ext = ExtensionLoader.getExtensionLoader(Ext2.class).getDefaultExtension();
        assertNull(ext);
        
        String name = ExtensionLoader.getExtensionLoader(Ext2.class).getDefaultExtensionName();
        assertNull(name);
    }
    
    @Test
    public void test_getExtension() throws Exception {
        assertTrue(ExtensionLoader.getExtensionLoader(SimpleExt.class).getExtension("impl1") instanceof SimpleExtImpl1);
        assertTrue(ExtensionLoader.getExtensionLoader(SimpleExt.class).getExtension("impl2") instanceof SimpleExtImpl2);
    }
    
    @Test
    public void test_getExtension_WithWrapper() throws Exception {
        WrappedExt impl1 = ExtensionLoader.getExtensionLoader(WrappedExt.class).getExtension("impl1");
        assertThat(impl1, anyOf(instanceOf(Ext5Wrapper1.class), instanceOf(Ext5Wrapper2.class)));
        
        WrappedExt impl2 = ExtensionLoader.getExtensionLoader(WrappedExt.class).getExtension("impl2") ;
        assertThat(impl2, anyOf(instanceOf(Ext5Wrapper1.class), instanceOf(Ext5Wrapper2.class)));
        
        
        URL url = new URL("p1", "1.2.3.4", 1010, "path1");
        int echoCount1 = Ext5Wrapper1.echoCount.get();
        int echoCount2 = Ext5Wrapper2.echoCount.get();

        assertEquals("Ext5Impl1-echo", impl1.echo(url, "ha"));
        assertEquals(echoCount1 + 1, Ext5Wrapper1.echoCount.get());
        assertEquals(echoCount2 + 1, Ext5Wrapper2.echoCount.get());
    }
    
    @Test
    public void test_getExtension_ExceptionNoExtension() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(SimpleExt.class).getExtension("XXX");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt by name XXX"));
        }
    }
    
    @Test
    public void test_getExtension_ExceptionNoExtension_WrapperNotAffactName() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(WrappedExt.class).getExtension("XXX");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension com.alibaba.dubbo.common.extensionloader.ext6_wrap.WrappedExt by name XXX"));
        }
    }
    
    @Test
    public void test_getExtension_ExceptionNullArg() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(SimpleExt.class).getExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }
    
    @Test
    public void test_hasExtension() throws Exception {
        assertTrue(ExtensionLoader.getExtensionLoader(SimpleExt.class).hasExtension("impl1"));
        assertFalse(ExtensionLoader.getExtensionLoader(SimpleExt.class).hasExtension("impl1,impl2"));
        assertFalse(ExtensionLoader.getExtensionLoader(SimpleExt.class).hasExtension("xxx"));
        
        try {
            ExtensionLoader.getExtensionLoader(SimpleExt.class).hasExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }

    @Test
    public void test_hasExtension_wrapperIsNotExt() throws Exception {
        assertTrue(ExtensionLoader.getExtensionLoader(WrappedExt.class).hasExtension("impl1"));
        assertFalse(ExtensionLoader.getExtensionLoader(WrappedExt.class).hasExtension("impl1,impl2"));
        assertFalse(ExtensionLoader.getExtensionLoader(WrappedExt.class).hasExtension("xxx"));

        assertFalse(ExtensionLoader.getExtensionLoader(WrappedExt.class).hasExtension("wrapper1"));

        try {
            ExtensionLoader.getExtensionLoader(WrappedExt.class).hasExtension(null);
            fail();
        } catch (IllegalArgumentException expected) {
            assertThat(expected.getMessage(), containsString("Extension name == null"));
        }
    }
    
    @Test
    public void test_getSupportedExtensions() throws Exception {
        Set<String> exts = ExtensionLoader.getExtensionLoader(SimpleExt.class).getSupportedExtensions();
        
        Set<String> expected = new HashSet<String>();
        expected.add("impl1");
        expected.add("impl2");
        expected.add("impl3");
        
        assertEquals(expected, exts);
    }

    @Test
    public void test_getSupportedExtensions_wrapperIsNotExt() throws Exception {
        Set<String> exts = ExtensionLoader.getExtensionLoader(WrappedExt.class).getSupportedExtensions();

        Set<String> expected = new HashSet<String>();
        expected.add("impl1");
        expected.add("impl2");

        assertEquals(expected, exts);
    }

    @Test
    public void test_AddExtension() throws Exception {
        try {
            ExtensionLoader.getExtensionLoader(SimpleExt.class).getExtension("Manual");
            fail();
        }
        catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("No such extension com.alibaba.dubbo.common.extensionloader.ext1.SimpleExt by name Manual"));
        }

        ExtensionLoader.getExtensionLoader(SimpleExt.class).addExtension("Manual", SimpleExtImpl_ManualAdd.class);
        SimpleExt ext = ExtensionLoader.getExtensionLoader(SimpleExt.class).getExtension("Manual");

        assertThat(ext, instanceOf(SimpleExtImpl_ManualAdd.class));
        assertEquals("Manual", ExtensionLoader.getExtensionLoader(SimpleExt.class).getExtensionName(SimpleExtImpl_ManualAdd.class));
    }

    @Test
    public void test_InitError() throws Exception {
        ExtensionLoader<InitErrorExt> loader = ExtensionLoader.getExtensionLoader(InitErrorExt.class);
        
        loader.getExtension("ok");
        
        try {
            loader.getExtension("error");
            fail();
        } catch (IllegalStateException expected) {
            assertThat(expected.getMessage(), containsString("Failed to load extension class(interface: interface com.alibaba.dubbo.common.extensionloader.ext7.InitErrorExt"));
            assertThat(expected.getCause(), instanceOf(ExceptionInInitializerError.class));
        }
    }

    @Test
    public void testLoadActivateExtension() throws Exception {
        // test default
        URL url = URL.valueOf("test://localhost/test");
        List<ActivateExt1> list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "default_group");
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.get(0).getClass() == ActivateExt1Impl1.class);

        // test group
        url = url.addParameter(Constants.GROUP_KEY, "group1");
        list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "group1");
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.get(0).getClass() == GroupActivateExtImpl.class);

        // test value
        url = url.removeParameter(Constants.GROUP_KEY);
        url = url.addParameter(Constants.GROUP_KEY, "value");
        url = url.addParameter("value", "value");
        list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "value");
        Assert.assertEquals(1, list.size());
        Assert.assertTrue(list.get(0).getClass() == ValueActivateExtImpl.class);

        // test order
        url = URL.valueOf("test://localhost/test");
        url = url.addParameter(Constants.GROUP_KEY, "order");
        list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, new String[]{}, "order");
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.get(0).getClass() == OrderActivateExtImpl1.class);
        Assert.assertTrue(list.get(1).getClass() == OrderActivateExtImpl2.class);
    }

    @Test
    public void testLoadDefaultActivateExtension() throws Exception {
        // test default
        URL url = URL.valueOf("test://localhost/test?ext=order1,default");
        List<ActivateExt1> list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, "ext", "default_group");
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.get(0).getClass() == OrderActivateExtImpl1.class);
        Assert.assertTrue(list.get(1).getClass() == ActivateExt1Impl1.class);
        
        url = URL.valueOf("test://localhost/test?ext=default,order1");
        list = ExtensionLoader.getExtensionLoader(ActivateExt1.class)
                .getActivateExtension(url, "ext", "default_group");
        Assert.assertEquals(2, list.size());
        Assert.assertTrue(list.get(0).getClass() == ActivateExt1Impl1.class);
        Assert.assertTrue(list.get(1).getClass() == OrderActivateExtImpl1.class);
    }

}