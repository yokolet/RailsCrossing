/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.servletgarden.railsxing;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Static files serving Servlet. This should have a cache for performance. Not yet.
 * 
 * @author Yoko Harada <yokolet@gmail.com>
 */
public class CrossingDefaultServlet extends HttpServlet {
    private String asset_dir_name;
    
    @Override
    public void init(ServletConfig config) {
        String rails_path = config.getInitParameter("rails_path");
        asset_dir_name = config.getServletContext().getRealPath("/WEB-INF/" + rails_path) + "/public";
    }
   
    /** 
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        String file_name = getRealPathToFile(request);
        
        response.setContentType(getContentTypeFromFilename(file_name));

        PrintWriter out = response.getWriter();
        
        InputStreamReader reader = new InputStreamReader(new FileInputStream(file_name));
        char cbuf[] = new char[4096];
        try {
            int size = 0;
            while ((size = reader.read(cbuf, 0, cbuf.length)) != -1) {
                out.write(cbuf, 0, size);
            }
        } finally {
            reader.close();
            out.close();
        }
        // allows servlet container to set content-length
    }
    
    private String getContentTypeFromFilename(String file_name) {
        String extension = file_name.substring(file_name.lastIndexOf(".") + 1).toLowerCase();
        if ("html".equals(extension) || "htm".equals(extension)) return "text/html";
        else if ("css".equals(extension)) return "text/css";
        else if ("js".equals(extension)) return "text/javascript";
        else return "text/plain";
    }
    
    private String getRealPathToFile(HttpServletRequest request) {
        String context_path = request.getContextPath();
        String request_uri = request.getRequestURI();
        return asset_dir_name + request_uri.substring(context_path.length());
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /** 
     * Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    } 

    /** 
     * Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    /** 
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
