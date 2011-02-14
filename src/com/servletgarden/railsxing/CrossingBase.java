/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.servletgarden.railsxing;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.List;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.jruby.embed.ScriptingContainer;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
@WebServlet(name="CrossingBase", urlPatterns={"/CrossingBase"})
public abstract class CrossingBase extends HttpServlet {
    private static final long serialVersionUID = 1L;
    protected ScriptingContainer container;
    protected List<CrossingRoute> routes;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public CrossingBase() {
        super();
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        container = CrossingHelpers.initialize(config);
        routes = CrossingHelpers.parseRoutes(container);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Base Servlet for Rails Crossing";
    }
    
    @Override
    public void destroy() {
        if (container != null) container.terminate();
    }
    
    protected Map<String, String> getEnvMap(HttpServletRequest request) {
        return CrossingHelpers.getEnvMap(request);
    }
    
    protected CrossingRoute findMatchedRoute(String context_path, String path_info, String method) {
        return CrossingHelpers.findMatchedRoute(container, routes, context_path, path_info, method);
    }
    
    protected void dispatch(HttpServletRequest request, HttpServletResponse response) throws IOException {
        CrossingRoute route = findMatchedRoute(request.getContextPath(), request.getPathInfo(), request.getMethod());
        if (route == null) return;
        Map<String, String> env = getEnvMap(request);
        CrossingResponse crossingResponse =  CrossingHelpers.dispatch(container, route, env);
        response.setStatus(crossingResponse.getStatus());
        Set<String> keys = crossingResponse.getResponseHeader().keySet();
        for (String key : keys) {
            String value = crossingResponse.getResponseHeader().get(key);
            response.setHeader(key, value);
        }
        PrintWriter writer = response.getWriter();
        writer.write(crossingResponse.getBody());
    }
    
    protected CrossingResponse dispatch(HttpServletRequest request) {
        CrossingRoute route = findMatchedRoute(request.getContextPath(), request.getPathInfo(), request.getMethod());
        Map<String, String> env = getEnvMap(request);
        return CrossingHelpers.dispatch(container, route, env);
    }
}
