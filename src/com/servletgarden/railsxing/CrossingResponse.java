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
    int status;
    Map<String, String> responseHeader;
    String body;
    
    CrossingResponse() {}

    public String getBody() {
        return body;
    }

    public Map<String, String> getResponseHeader() {
        return responseHeader;
    }

    public int getStatus() {
        return status;
    }
}
