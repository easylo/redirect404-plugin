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

import hudson.Extension;
import hudson.Functions;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.security.AccessDeniedException2;
import hudson.security.Permission;
import hudson.util.PluginServletFilter;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

/**
 * Redirect to the login page when 404 without login.
 */
@Extension
public final class Redirect404Filter extends Descriptor<Redirect404Filter> implements Describable<Redirect404Filter>, Filter {
    private static final Logger LOGGER = Logger.getLogger(Redirect404Filter.class.getName());
    private boolean enabled = false;
    
    /**
     * @throws ServletException
     */
    public Redirect404Filter() throws ServletException {
        super(Redirect404Filter.class);
        load();
        PluginServletFilter.addFilter(this);
    }
    
    /**
     * @return
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean configure(StaplerRequest req, JSONObject json)
            throws hudson.model.Descriptor.FormException
    {
        setEnabled(json.getBoolean("enabled"));
        save();
        return super.configure(req, json);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init(FilterConfig config) throws ServletException {
        // Nothing to do
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // Nothing to do
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException
    {
        if (
                !isEnabled()
                || !Jenkins.getInstance().isUseSecurity()
                || !(response instanceof HttpServletResponse)
        ) {
            // nothing to do
            chain.doFilter(request, response);
            return;
        }
        
        chain.doFilter(request, new HttpServletResponseWrapper((HttpServletResponse) response) {
            private void checkStatus(int sc) {
                if (sc == SC_NOT_FOUND && Functions.isAnonymous()) {
                    LOGGER.log(Level.FINE, "Redirect to the login page.");
                    throw new AccessDeniedException2(
                            Jenkins.ANONYMOUS,
                            Permission.READ
                        );
                }
            }
            
            @Override
            public void setStatus(int sc) {
                checkStatus(sc);
                super.setStatus(sc);
            }
            
            @Override
            public void setStatus(int sc, String sm) {
                checkStatus(sc);
                super.setStatus(sc, sm);
            }
            
            @Override
            public void sendError(int sc) throws IOException {
                checkStatus(sc);
                super.sendError(sc);
            }
            
            @Override
            public void sendError(int sc, String msg) throws IOException {
                checkStatus(sc);
                super.sendError(sc, msg);
            }
        });
    }
    
    @Override
    public Redirect404Filter getDescriptor() {
        return this;
    }
    
    @Override
    public String getDisplayName() {
        return Messages.Redirect404Filter_DisplayName();
    }
}
