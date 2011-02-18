/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.servletgarden.railsxing;

import java.util.Map;

/**
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class CrossingResponse {
    String context_path;
    int status;
    Map<String, String> responseHeader;
    String body;
    
    CrossingResponse() {}

    public String getBody() {
        return addContextPath();
    }

    public Map<String, String> getResponseHeader() {
        return responseHeader;
    }

    public int getStatus() {
        return status;
    }
    
    private String addContextPath() {
        // TODO: should use regex
        String tmp = body;
        tmp = tmp.replace("/stylesheets/", context_path + "/stylesheets/");
        tmp = tmp.replace("/javascripts/", context_path + "/javascripts/");
        return tmp;
    }
}
