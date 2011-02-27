/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.servletgarden.railsxing;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyHash;
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
                "require 'config/environment'\n" +
                "ActionController::Routing::Routes.finalize!";
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
    
    public static Map<String, Object> getEnvMap(HttpServletRequest request) throws IOException {
	Map<String, Object> env = new LinkedHashMap<String, Object>();
        env.put("HTTP_VERSION".intern(), request.getProtocol());
        env.put("rack.url_scheme", request.getScheme());
        env.put("AUTH_TYPE".intern(), request.getAuthType());
        //env.put("PATH_TRANSLATED".intern(), request.getPathTranslated()); // no meaningfull path
        env.put("REQUEST_METHOD".intern(), request.getMethod());
        String tmp = request.getParameter("_method");
        if (tmp != null) env.put("rack.methodoverride.original_method", tmp.toUpperCase());
        env.put("PATH_INFO".intern(), request.getRequestURI());
        env.put("QUERY_STRING".intern(), request.getQueryString());
        env.put("SERVER_NAME".intern(), request.getServerName());
        env.put("SERVER_PORT".intern(), String.valueOf(request.getServerPort()));
        env.put("CONTENT_TYPE".intern(), request.getContentType());
        env.put("CONTENT_LENGTH".intern(), String.valueOf(request.getContentLength()));
        env.put("REQUEST_URI".intern(), request.getRequestURI());
        env.put("REMOTE_HOST", request.getRemoteHost());
        env.put("REMOTE_ADDR", request.getRemoteAddr());
        env.put("REMOTE_USER", request.getRemoteUser());
        env.put("CONTENT_TYPE", request.getContentType());
        env.put("CONTENT_LENGTH", request.getContentLength());
        return env;
    }
    
    public static void setParamsToEnv(ScriptingContainer container, HttpServletRequest request, Map<String, Object> env) throws IOException {
        String tmp = request.getContentType();
        if (tmp == null) {
            env.put("rack.input", "");
        } else if ("application/x-www-form-urlencoded".equals(tmp.toLowerCase())) {
            env.put("rack.input", "");
            env.put("rack.request.form_input", "");
            Map form_hash = RubyHash.newHash(container.getProvider().getRuntime());
            Enumeration<String> names = request.getParameterNames();
            while (names.hasMoreElements()) {
                String key = names.nextElement();
                if ("utf8".equals(key)) {
                    form_hash.put("utf8", convertToUTF8(request.getParameter("utf8")));
                } else if ("authenticity_token".equals(key)) {
                    form_hash.put("authenticity_token", request.getParameter("authenticity_token"));
                } else if ("commit".equals(key)) {
                    form_hash.put("commit", request.getParameter("commit"));
                } else {
                    parseParameter(container.getProvider().getRuntime(), form_hash, key, convertToUTF8(request.getParameter(key)));
                }
            }
            
            env.put("rack.request.form_hash", form_hash);
        } else if ("multipart/form-data".equals(tmp.toLowerCase())) {
            env.put("rack.input", request.getInputStream());
        } else {
            env.put("rack.input", "");
        }
        Map map = (Map) request.getSession().getAttribute("action_dispatch.request.flash_hash");
        if (map != null) env.put("action_dispatch.request.flash_hash", map);
    }
    
    private static String convertToUTF8(String str) throws UnsupportedEncodingException {
        return new String(str.getBytes("ISO-8859-1"), "UTF-8");
    }
    
    private static Pattern pattern = Pattern.compile("[\\[\\]]");
    
    private static void parseParameter(Ruby runtime, Map<String, RubyHash> form_hash, String key, String value) {
        // form_hash should not be null
        // key is something like user[name] or user[email]
        // params is a pair of a model name and key-value pair. ex) user => {:name => 'name', :email => 'email'}
        String[] results = pattern.split(key);
        // results[0] and results[1] should be something like user and name respectively
        if (results == null || results.length != 2) return;
        RubyHash m; // m is a key-value pair. ex) {:name => 'name', :email => 'email'}
        if (form_hash.containsKey(results[0])) {
            m = (RubyHash) form_hash.get(results[0]);
        } else {
            m = RubyHash.newHash(runtime);
        }
        m.put(results[1], value);
        form_hash.put(results[0], m);
    }
    
    public static CrossingRoute findMatchedRoute(ScriptingContainer container, List<CrossingRoute> routes, String request_uri, String method) {
        if (request_uri == null || method == null) return null;
        IRubyObject params = (IRubyObject) container.runScriptlet("Rails.application.routes.recognize_path(\"" + request_uri + "\")");
        IRubyObject ruby_path = RubyString.newString(container.getProvider().getRuntime(), request_uri);
        IRubyObject ruby_method = RubyString.newString(container.getProvider().getRuntime(), method.toUpperCase());
        for (CrossingRoute route : routes) {
            if (container.callMethod(route.path_info_pattern, "match", ruby_path) != null) {
                if (container.callMethod(route.request_method_pattern, "match", ruby_method) != null) {
                    route.params = params;
                    return route;
                }
            }
        }
        return null;
    }
    
    public static CrossingResponse dispatch(ScriptingContainer container, String context_path, CrossingRoute route, Map<String, Object> env) {
        env.put("action_dispatch.request.path_parameters", route.getParams());
        String script = 
                "response = " + route.getName() + ".action('" + route.getAction() + "').call(env)\n" +
                "return response[0], response[1], response[2].body, response[2].request.flash";       
        container.put("env", env);
        RubyArray responseArray = (RubyArray)container.runScriptlet(script);
        CrossingResponse response = new CrossingResponse();
        response.context_path = context_path;
        response.status = ((Long)responseArray.get(0)).intValue(); //status code; Fixnum
        response.responseHeader = (Map)responseArray.get(1); // response header; Hash
        response.body = (String)responseArray.get(2); // response body
        response.flash = (Map) responseArray.get(3);
        return response;
    }
}
