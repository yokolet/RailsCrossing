/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.servletgarden.railsxing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import org.jruby.RubyArray;
import org.jruby.RubyString;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class CrossingHelpers {

    public static ScriptingContainer initialize(ServletConfig config) {
        ScriptingContainer c = getContainerInstance();
        String rails_path = config.getInitParameter("rails_path");
        String gem_path = config.getInitParameter("gem_path");
        // web app should be portable. base_path based paths are feasible. should be changed
        String base_path = config.getServletContext().getRealPath("/WEB-INF");
        fireUpRails(c, base_path, rails_path, gem_path);
        return c;
    }
    
    public static ScriptingContainer getContainerInstance() {
        ScriptingContainer container = new ScriptingContainer(LocalContextScope.CONCURRENT, LocalVariableBehavior.TRANSIENT);
        container.setClassLoader(container.getClass().getClassLoader());
        return container;
    }
    
    public static void fireUpRails(ScriptingContainer container, String base_path, String rails_path, String gem_path) {
        container.put("load_path", base_path + "/" + rails_path);
        container.put("gem_path", base_path + "/" + gem_path);
        container.put("gemfile", base_path + "/" + rails_path + "/Gemfile");
        String script =
                "$LOAD_PATH << load_path; ENV['GEM_HOME'] = gem_path; ENV['BUNDLE_GEMFILE'] = gemfile\n" +
                "require 'config/environment'";
        container.runScriptlet(script);
    }
    
    public static List<CrossingRoute> parseRoutes(ScriptingContainer container) {
        List<CrossingRoute> routeArray = new ArrayList<CrossingRoute>();
        String script =
                "result = []\n" +
                "ActionDispatch::Routing::Routes.routes.each do |route|\n" +
                "  params = []\n" +
                "  params << route.to_a[1][:path_info]\n" +
                "  params << route.to_a[1][:request_method]\n" +
                "  params << route.to_a[2][:controller]\n" +
                "  params << route.to_a[2][:action]\n" +
                "  params << route.to_a[3]\n" +
                "  result << params\n" +
                "end\n" +
                "result";
        RubyArray routesArray = (RubyArray) container.runScriptlet(script);
        for (int i=0; i<routesArray.size(); i++) {
            RubyArray params = (RubyArray)routesArray.get(i);
            CrossingRoute route = new CrossingRoute();
            route.path_info_pattern = (IRubyObject) params.get(0);
            route.request_method_pattern = (IRubyObject) params.get(1);
            route.controller = (String) params.get(2);
            route.action = (String) params.get(3);
            route.name = (String) params.get(4);
            routeArray.add(route);
        }
        return routeArray;
    }
    
    public static Map<String, String> getEnvMap(HttpServletRequest request) {
	Map<String, String> env = new HashMap<String, String>();
        env.put("rack.input", "");
        env.put("REQUEST_METHOD".intern(), request.getMethod());
        env.put("PATH_INFO".intern(), request.getPathInfo());
        env.put("QUERY_STRING".intern(), request.getQueryString());
        env.put("SERVER_NAME".intern(), request.getServerName());
        env.put("SERVER_PORT".intern(), String.valueOf(request.getServerPort()));
        env.put("CONTENT_TYPE".intern(), request.getContentType());
        env.put("CONTENT_LENGTH".intern(), String.valueOf(request.getContentLength()));
        env.put("REQUEST_URI".intern(), request.getRequestURI());
        env.put("REMOTE_HOST", request.getRemoteHost());
        env.put("REMOTE_ADDR", request.getRemoteAddr());
        env.put("REMOTE_USER", request.getRemoteUser());
        return env;
    }
    
    public static CrossingRoute findMatchedRoute(ScriptingContainer container, List<CrossingRoute> routes, String request_uri, String method) {
        if (request_uri == null || method == null) return null;
        IRubyObject ruby_path = RubyString.newString(container.getProvider().getRuntime(), request_uri);
        IRubyObject ruby_method = RubyString.newString(container.getProvider().getRuntime(), method.toUpperCase());
        for (CrossingRoute route : routes) {
            if (container.callMethod(route.path_info_pattern, "match", ruby_path) != null) {
                if (container.callMethod(route.request_method_pattern, "match", ruby_method) != null) {
                    return route;
                }
            }
        }
        return null;
    }
    
    public static CrossingResponse dispatch(ScriptingContainer container, String context_path, CrossingRoute route, Map<String, String> env) {
        String script = 
                "response = " + route.getName() + ".action('" + route.getAction() + "').call(env)\n" +
                "return response[0], response[1], response[2].body";
        container.put("env", env);
        RubyArray responseArray = (RubyArray)container.runScriptlet(script);
        CrossingResponse response = new CrossingResponse();
        response.context_path = context_path;
        response.status = ((Long)responseArray.get(0)).intValue(); //status code; Fixnum
        response.responseHeader = (Map)responseArray.get(1); // response header; Hash
        response.body = (String)responseArray.get(2); // response body
        return response;
    }
}
