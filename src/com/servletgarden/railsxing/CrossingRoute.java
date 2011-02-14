/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.servletgarden.railsxing;

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
    
    CrossingRoute() {}

    public String getAction() {
        return action;
    }

    public String getController() {
        return controller;
    }
    
    public String getName() {
        String tmp = name.substring(0, name.lastIndexOf("_"));
        StringTokenizer st = new StringTokenizer(tmp, "_");
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

    public IRubyObject getPath_info() {
        return path_info_pattern;
    }

    public IRubyObject getRequest_method() {
        return request_method_pattern;
    }
}
