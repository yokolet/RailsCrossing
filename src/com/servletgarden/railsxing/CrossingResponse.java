/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.servletgarden.railsxing;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    private static Pattern pattern = 
        Pattern.compile("(<\\s*link\\s+href\\s*=\\s*(\"|')\\/stylesheets/)|(<\\s*script\\s+src\\s*=\\s*(\"|')\\/javascripts/)");
    
    private String addContextPath() {
        Matcher matcher = pattern.matcher(body);
        boolean result = matcher.find();
        StringBuffer sb = new StringBuffer();
        while(result) {
            String matched = matcher.group();
            String replacement = "";
            if (matched.contains("/stylesheets/")) {
                replacement = matched.replace("/stylesheets/", context_path + "/stylesheets/");
            } else if (matched.contains("/javascripts/")) {
                replacement = matched.replace("/javascripts/", context_path + "/javascripts/");
            }
            matcher.appendReplacement(sb, replacement);
            result = matcher.find();
        }
        matcher.appendTail(sb);
        return new String(sb);
    }
}
