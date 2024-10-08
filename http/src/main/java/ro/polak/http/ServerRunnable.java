/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package ro.polak.http;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import ro.polak.http.configuration.ServerConfig;
import ro.polak.http.errorhandler.HttpErrorHandlerResolver;
import ro.polak.http.exception.AccessDeniedException;
import ro.polak.http.exception.MethodNotAllowedException;
import ro.polak.http.servlet.HttpServletRequest;
import ro.polak.http.servlet.factory.HttpServletRequestImplFactory;
import ro.polak.http.servlet.factory.HttpServletResponseImplFactory;
import ro.polak.http.servlet.impl.HttpServletRequestImpl;
import ro.polak.http.servlet.impl.HttpServletResponseImpl;
import ro.polak.http.utilities.IOUtilities;

/**
 * Server thread.
 *
 * @author Piotr Polak piotr [at] polak [dot] ro
 * @since 200802
 */
public class ServerRunnable implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ServerRunnable.class.getName());

    private final ServerConfig serverConfig;
    private final Socket socket;
    private final HttpServletRequestImplFactory requestFactory;
    private final HttpServletResponseImplFactory responseFactory;
    private final HttpErrorHandlerResolver httpErrorHandlerResolver;
    private final PathHelper pathHelper;

    /**
     * Default constructor.
     *
     * @param socket
     * @param serverConfig
     * @param requestFactory
     * @param httpErrorHandlerResolver
     */
    public ServerRunnable(final Socket socket,
                          final ServerConfig serverConfig,
                          final HttpServletRequestImplFactory requestFactory,
                          final HttpServletResponseImplFactory responseFactory,
                          final HttpErrorHandlerResolver httpErrorHandlerResolver,
                          final PathHelper pathHelper) {
        this.socket = socket;
        this.serverConfig = serverConfig;
        this.requestFactory = requestFactory;
        this.responseFactory = responseFactory;
        this.httpErrorHandlerResolver = httpErrorHandlerResolver;
        this.pathHelper = pathHelper;

        //浏览器输入一次会有两个ServerRunnable object产生  derek 2024/08/28 at home
//        try {
//            LOGGER.log(Level.INFO, "ServerThread object is created, getRemoteSocketAddress = "
//                    + socket.getRemoteSocketAddress() + ", available = " + socket.getInputStream().available() + ", thread ID = " + Thread.currentThread().getId());
//        }catch (Exception e){
//            LOGGER.log(Level.INFO, "ServerThread object get Exception when object create: " + e.toString());
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        HttpServletResponseImpl response = null;
        //LOGGER.log(Level.INFO, "ServerRunnable.run is running.");
        try {
            try {
                response = responseFactory.createFromSocket(socket);

                //add by Derek to fix the issue of
                //"if available bytes from InputStream --> 0, then will block in in.read(buffer, 0, buffer.length)"
                //which in function HttpServletRequestImplFactory.getStatusLine
                if(0 == socket.getInputStream().available()) {
                    LOGGER.log(Level.INFO, "socket.getInputStream().available = " + socket.getInputStream().available() + ", RemoteSocketAddress = " + socket.getRemoteSocketAddress());
                    //socket.close();
                    socket.setKeepAlive(true);
                    //return;
                }

                HttpServletRequestImpl request = requestFactory.createFromSocket(socket);

                //no work Derek 2024/08/21
                //LOGGER.log(Level.INFO, "Handling request {0} {1}", new Object[]{
                //        request.getMethod(), request.getRequestURI()});
                LOGGER.log(Level.INFO, "Handling request: Method[" + request.getMethod() + "] RequestURI[" +request.getRequestURI() + "]");

                String requestedPath = request.getRequestURI();

                if (pathHelper.isPathContainingIllegalCharacters(requestedPath)) {
                    throw new AccessDeniedException();
                }

                validateRequest(request);
                setDefaultResponseHeaders(request, response);
                serverConfig.getServletDispatcher().load(requestedPath, request, response);
            } catch (RuntimeException e) {
                if (response != null) {
                    httpErrorHandlerResolver.getHandler(e).serve(response);
                }

                throw e; // Make it logged by the main thread
            } finally {
                IOUtilities.closeSilently(socket);
            }
        } catch (IOException e) {
            //LOGGER.log(Level.INFO, "Encountered IOException when handling request {0}", new Object[]{e.getMessage()});
            LOGGER.log(Level.INFO, "Encountered IOException when handling request, IOException:" + e.getMessage());
        }
    }

    /**
     * Sets default response headers.
     *
     * @param request
     * @param response
     */
    private void setDefaultResponseHeaders(final HttpServletRequest request, final HttpServletResponseImpl response) {
        boolean isKeepAlive = false;
        if (request.getHeader(Headers.HEADER_CONNECTION) != null) {
            isKeepAlive = request.getHeader(Headers.HEADER_CONNECTION).equalsIgnoreCase("keep-alive");
        }

        response.setKeepAlive(isKeepAlive && serverConfig.isKeepAlive());
        response.getHeaders().setHeader(Headers.HEADER_SERVER, WebServer.SIGNATURE);
    }

    /**
     * Throws exception in case of invalid request.
     *
     * @param request
     */
    private void validateRequest(final HttpServletRequest request) {
        if (!isMethodSupported(request.getMethod())) {
            throw new MethodNotAllowedException();
        }
    }

    /**
     * Tells whether the given HTTP method is supported.
     *
     * @param method
     * @return
     */
    private boolean isMethodSupported(final String method) {
        for (String aMethod : serverConfig.getSupportedMethods()) {
            if (aMethod.equals(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns associated socket.
     *
     * @return
     */
    protected Socket getSocket() {
        return socket;
    }
}
