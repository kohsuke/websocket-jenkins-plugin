package org.jenkinsci.plugins.websocket;

import hudson.model.UnprotectedRootAction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.TestExtension;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.IOException;

/**
 * @author Kohsuke Kawaguchi
 */
public class EchoTest {
    @Rule
    public final JenkinsRule j = new JenkinsRule();

    public EchoTest() {
        j.timeout = 0;
    }

    @Test
    public void echoService() throws Exception {
        j.interactiveBreak();
    }

    /**
     * Maps /echo/ to the WebSocket endpoint.
     */
    @TestExtension
    public static class EchoService extends WebSocketEndpoint implements UnprotectedRootAction {
        public EchoService() {
            super(new EchoServiceImpl());
        }

        @Override
        public String getIconFileName() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getUrlName() {
            return "echo";
        }
    }

    public static class EchoServiceImpl extends Endpoint {
        @Override
        public void onOpen(final Session session, EndpointConfig ec) {
            session.addMessageHandler(new MessageHandler.Whole<String>() {

                @Override
                public void onMessage(String text) {
                    try {
                        session.getBasicRemote().sendText(text);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
    }
}
