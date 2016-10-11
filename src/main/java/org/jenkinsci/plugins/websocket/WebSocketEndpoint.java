package org.jenkinsci.plugins.websocket;

import hudson.util.HttpResponses;
import org.kohsuke.stapler.AttributeKey;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.websocket.Endpoint;
import java.io.IOException;

import static javax.servlet.http.HttpServletResponse.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class WebSocketEndpoint {
    /**
     * JSR-356 API that handles WebSocket connections to this endpoint.
     */
    private final Endpoint endpoint;

    public WebSocketEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    /**
     * Receives a WebSocket connection, upgrades it, and hand over to Tyrus.
     *
     * <p>
     * the actual logic is implemented as a Filter by Tyrus.
     * If it doesn't handle the request, it attempts to pass it to FilterChain.
     * In our case that means the client didn't send us the WebSocket connection request and that's an error.
     */
    public final void doIndex(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        final TyrusComponent t = TyrusComponent.get();

        KEY.set(req,this);  // so that we know how to route back onOpen request

        // fake request URI so that Tyrus will accept the request and dispatch that to the sole endpoint registered
        HttpServletRequest r = new HttpServletRequestWrapper(req) {
            @Override
            public String getRequestURI() {
                return t.contextPath+"/";
            }
        };

        t.tyrus.doFilter(r, rsp, new FilterChain() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
                throw HttpResponses.error(SC_BAD_REQUEST,"Expected WebSocket connection");
            }
        });
    }

    static final AttributeKey<WebSocketEndpoint> KEY = AttributeKey.requestScoped();

}
