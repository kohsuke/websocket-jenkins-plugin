package org.jenkinsci.plugins.websocket;

import hudson.Extension;
import jenkins.model.Jenkins;
import org.glassfish.tyrus.core.TyrusWebSocketEngine;
import org.glassfish.tyrus.server.TyrusServerContainer;
import org.glassfish.tyrus.spi.WebSocketEngine;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;

import javax.servlet.Filter;
import javax.websocket.CloseReason;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collections;

/**
 * Component that houses Tyrus (JSR-356 RI) websocket engine.
 *
 * Needs to be public to be {@link Extension}, but other plugins only should be using {@link WebSocketEndpoint}.
 *
 * @author Kohsuke Kawaguchi
 */
@Extension @Restricted(NoExternalUse.class)
public class TyrusComponent {
    /*package*/ final WebSocketEngine engine;
    /*package*/ final Filter tyrus;
    /*package*/ final String contextPath;

    public TyrusComponent() throws Exception {
        contextPath = Jenkins.getInstance().servletContext.getContextPath();

        final TyrusServerContainer serverContainer = new TyrusServerContainer(Collections.<Class<?>>emptySet()) {

            // TODO: we can make these parameters customizable
            private final WebSocketEngine engine =
                    TyrusWebSocketEngine.builder(this)
//                                        .incomingBufferSize(incomingBufferSize)
//                                        .maxSessionsPerApp(maxSessionsPerApp)
//                                        .maxSessionsPerRemoteAddr(maxSessionsPerRemoteAddr)
//                                        .parallelBroadcastEnabled(parallelBroadcastEnabled)
//                                        .tracingType(tracingType)
//                                        .tracingThreshold(tracingThreshold)
                                        .build();

            @Override
            public void start(String rootPath, int port) throws IOException, DeploymentException {
                register(ServerEndpointConfig.Builder.create(EndpointImpl.class, "/").build());
                super.start(rootPath, port);
            }

            @Override
            public void register(Class<?> endpointClass) throws DeploymentException {
                engine.register(endpointClass, contextPath);
            }

            @Override
            public void register(ServerEndpointConfig serverEndpointConfig) throws DeploymentException {
                engine.register(serverEndpointConfig, contextPath);
            }

            @Override
            public WebSocketEngine getWebSocketEngine() {
                return engine;
            }
        };

        engine = serverContainer.getWebSocketEngine();

        // not public
        Class<?> c = Class.forName("org.glassfish.tyrus.servlet.TyrusServletFilter");
        Constructor<?> ctr = c.getDeclaredConstructor(TyrusWebSocketEngine.class);
        ctr.setAccessible(true);
        tyrus = (Filter)ctr.newInstance(engine);

        try {
            // AFAICT from TyrusServletFilter, the port is unused
            serverContainer.start(contextPath,0);
        } finally {
            serverContainer.doneDeployment();
        }
    }

    public static class EndpointImpl extends Endpoint {
        @Override
        public void onOpen(Session session, EndpointConfig config) {
            WebSocketEndpoint e = WebSocketEndpoint.KEY.get();
            assert e!=null;
            e.getEndpoint().onOpen(session,config);
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            WebSocketEndpoint e = toWebSocketEndpoint(session);
            if (e!=null)
                e.getEndpoint().onClose(session, closeReason);
        }

        @Override
        public void onError(Session session, Throwable thr) {
            WebSocketEndpoint e = toWebSocketEndpoint(session);
            if (e!=null)
                e.getEndpoint().onError(session, thr);
        }

        private WebSocketEndpoint toWebSocketEndpoint(Session session) {
            return (WebSocketEndpoint) session.getUserProperties().get(ENDPOINT_KEY);
        }
    }

    private static final String ENDPOINT_KEY = WebSocketEndpoint.class.getName();

    public static TyrusComponent get() {
        return Jenkins.getActiveInstance().getInjector().getInstance(TyrusComponent.class);
    }
}
