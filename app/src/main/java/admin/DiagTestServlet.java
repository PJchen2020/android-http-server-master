/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package admin;

import admin.logic.HTMLDiagTest;
import admin.logic.HTMLDocument;
import ro.polak.http.exception.ServletException;
import ro.polak.http.servlet.HttpServlet;
import ro.polak.http.servlet.HttpServletRequest;
import ro.polak.http.servlet.HttpServletResponse;

/**
 * Admin panel front page.
 */
public class DiagTestServlet extends HttpServlet {

    /**
     * {@inheritDoc}
     */
    @Override
    public void service(final HttpServletRequest request, final HttpServletResponse response) throws ServletException {
        HTMLDiagTest doc = renderDocument();
        response.getWriter().print(doc.toString());
    }

    private HTMLDiagTest renderDocument() {
        HTMLDiagTest doc = new HTMLDiagTest("Derek Test Page");
        doc.setOwnerClass(getClass().getSimpleName());

        doc.writeln("<div class=\"page-header\"><h1>About</h1></div>");
        doc.write("<p>" + ro.polak.http.WebServer.SIGNATURE + " running.</p>");
        doc.write("<p> Derek test page.</p>");
        doc.write("<button type=\"button\" onclick=\"btnclick()\">Click Test</button>");

        return doc;
    }
}
