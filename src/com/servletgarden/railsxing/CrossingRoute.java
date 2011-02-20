/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.servletgarden.railsxing;

import java.util.Map;
import java.util.StringTokenizer;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class CrossingRoute {
    IRubyObject path_info_pattern; // Rack::Mount::RegexpWithNamedGroups
    IRubyObject request_method_pattern; // Rack::Mount::RegexpWithNamedGroups
    String controller; //String
    String action; //String
    String name; // String
    String controller_class_name = null;
    IRubyObject params;
    
    CrossingRoute() {}

    public String getAction() {
        return action;
    }

    public String getController() {
        return controller;
    }
    
    public String getName() {
        if (controller_class_name != null) return controller_class_name;
        return controller_class_name = guessControllerName();
    }

    public IRubyObject getPath_info() {
        return path_info_pattern;
    }

    public IRubyObject getRequest_method() {
        return request_method_pattern;
    }
    
    private String guessControllerName() {
        String tmp = controller;
        String scope_name = getScopeName();
        if (scope_name != null) tmp = tmp.substring(scope_name.length() + 1);
        return getCapitalizedControllerName(tmp);
    }
    
    private String getScopeName() {
        //   controller  |   action   |       name            |    actual controller and action
        // Acacia/home   | index      | home_index            | HomeController.action('index')
        // rails/info    | properties | rails_info_properties | Rails::InfoController.action('properties')
        // users         | index      | users                 | UsersController.action('index')
        // users         | create     | nil                   | UsersController.action('create')
        
        if (!controller.contains("/")) return null;
        String tmp = controller.substring(0, controller.indexOf("/")).toLowerCase();
        return name.contains(tmp) ? null : tmp;
    }
    
    private String getCapitalizedControllerName(String s) {
        StringTokenizer st = new StringTokenizer(s, "/");
        StringBuffer sb = new StringBuffer();
        while(st.hasMoreTokens()) {
            if (sb.length() > 0) sb.append("::");
            String token = st.nextToken();
            token = String.format("%s%s",
                                  Character.toUpperCase(token.charAt(0)),
                                  token.substring(1));
            sb.append(token);
        }
        sb.append("Controller");
        return new String(sb);
    }
    
    public IRubyObject getParams() {
        return params;
    }
}
