/*
 * The MIT License
 * 
 * Copyright (c) 2016 IKEDA Yasuyuki
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package jp.ikedam.jenkins.plugins.redirect404;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import jenkins.model.Jenkins;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.security.AuthorizationMatrixProperty;
import hudson.security.ProjectMatrixAuthorizationStrategy;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule.WebClient;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.common.collect.Sets;

import jp.ikedam.jenkins.plugins.redirect404.testutils.Redirect404JenkinsRule;

/**
 *
 */
public class Redirect404FilterTest {
    @ClassRule
    public static Redirect404JenkinsRule j = new Redirect404JenkinsRule();
    
    private static ProjectMatrixAuthorizationStrategy auth;
    private static FreeStyleProject test1;
    private static FreeStyleProject test2;
    
    @BeforeClass
    public static void parepareJenkins() throws Exception {
        // |User     |Overall|test1|test2|
        // |:--------|:-----:|:---:|:---:|
        // |admin    |x      |x    |x    |
        // |user1    |x      |x    |     |
        // |anonymous|x      |x    |     |
        auth = new ProjectMatrixAuthorizationStrategy();
        
        auth.add(Jenkins.ADMINISTER, "admin");
        auth.add(Jenkins.READ,       "user1");
        auth.add(Jenkins.READ,       "anonymous");
        
        test1 = j.createFreeStyleProject();
        {
            Map<Permission, Set<String>> authMap = new HashMap<Permission, Set<String>>();
            authMap.put(Item.READ, Sets.newHashSet("user1", "anonymous"));
            test1.addProperty(new AuthorizationMatrixProperty(authMap));
        }
        
        test2 = j.createFreeStyleProject();
    }
    
    @Before
    public void setupJenkins() throws Exception {
        j.jenkins.setSecurityRealm(j.createDummySecurityRealm());
        j.jenkins.setAuthorizationStrategy(auth);
    }
    
    private Redirect404Filter getTarget() {
        return j.jenkins.getDescriptorByType(Redirect404Filter.class);
    }
    
    private void assertNotFound(HtmlPage p) {
        assertEquals(HttpServletResponse.SC_NOT_FOUND, p.getWebResponse().getStatusCode());
    }
    
    private void assertIsLoginPage(HtmlPage p) {
        assertEquals(HttpServletResponse.SC_OK, p.getWebResponse().getStatusCode());
        assertNotNull(p.getFormByName("login"));
    }
    
    @Test
    public void testFilterDisabled() throws Exception {
        getTarget().setEnabled(false);
        WebClient wc = j.createWebClientForFilterTest();
        
        assertNotFound(wc.getPage(test2));
    }
    
    @Test
    public void testAnonymousRedirect() throws Exception {
        getTarget().setEnabled(true);
        WebClient wc = j.createWebClientForFilterTest();
        
        assertIsLoginPage(wc.getPage(test2));
    }
    
    @Test
    public void testAnonymousRedirectEvenForUnknownPage() throws Exception {
        getTarget().setEnabled(true);
        WebClient wc = j.createWebClientForFilterTest();
        
        assertIsLoginPage(wc.goTo("nosuchpage"));
    }
    
    @Test
    public void testAnonymousNotRedirect() throws Exception {
        getTarget().setEnabled(true);
        WebClient wc = j.createWebClient();
        
        wc.getPage(test1);
    }
    
    @Test
    public void testUserNotRedirect() throws Exception {
        getTarget().setEnabled(true);
        WebClient wc = j.createWebClientForFilterTest();
        wc.login("user1");
        
        assertNotFound(wc.getPage(test2));
    }
    
    @Test
    public void testInsecured() throws Exception {
        j.jenkins.disableSecurity();
        getTarget().setEnabled(true);
        WebClient wc = j.createWebClientForFilterTest();
        
        assertNotFound(wc.goTo("nosuchpage"));
    }
    
    @Test
    public void testConfigurationEnabled() throws Exception {
        getTarget().setEnabled(true);
        WebClient wc = j.createWebClient();
        wc.login("admin");
        HtmlPage page = wc.getPage(j.jenkins, "configure");
        getTarget().setEnabled(false);
        j.submit(page.getFormByName("config"));
        assertTrue(getTarget().isEnabled());
    }
    
    @Test
    public void testConfigurationDisabled() throws Exception {
        getTarget().setEnabled(false);
        WebClient wc = j.createWebClient();
        wc.login("admin");
        HtmlPage page = wc.getPage(j.jenkins, "configure");
        getTarget().setEnabled(true);
        j.submit(page.getFormByName("config"));
        assertFalse(getTarget().isEnabled());
    }
}
