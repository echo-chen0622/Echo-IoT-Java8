package org.echoiot.rule.engine.rest;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.echoiot.rule.engine.api.TbContext;
import org.echoiot.rule.engine.api.TbNodeConfiguration;
import org.echoiot.rule.engine.api.TbNodeException;
import org.echoiot.server.common.data.id.DeviceId;
import org.echoiot.server.common.data.id.EntityId;
import org.echoiot.server.common.data.id.RuleChainId;
import org.echoiot.server.common.data.id.RuleNodeId;
import org.echoiot.server.common.msg.TbMsg;
import org.echoiot.server.common.msg.TbMsgDataType;
import org.echoiot.server.common.msg.TbMsgMetaData;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class TbRestApiCallNodeTest {

    private TbRestApiCallNode restNode;

    @Mock
    private TbContext ctx;

    private final EntityId originator = new DeviceId(Uuids.timeBased());
    private final TbMsgMetaData metaData = new TbMsgMetaData();

    private final RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private final RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

	private HttpServer server;

    public void setupServer(String pattern, HttpRequestHandler handler) throws IOException {
        SocketConfig config  = SocketConfig.custom().setSoReuseAddress(true).setTcpNoDelay(true).build();
    	server = ServerBootstrap.bootstrap()
                .setSocketConfig(config)
    			.registerHandler(pattern, handler)
    			.create();
        server.start();
    }

    private void initWithConfig(TbRestApiCallNodeConfiguration config) {
        try {
            @NotNull ObjectMapper mapper = new ObjectMapper();
            @NotNull TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));
            restNode = new TbRestApiCallNode();
            restNode.init(ctx, nodeConfiguration);
        } catch (TbNodeException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @After
    public void teardown() {
        server.stop();
    }

    @Test
    public void deleteRequestWithoutBody() throws IOException, InterruptedException {
        @NotNull final CountDownLatch latch = new CountDownLatch(1);
        @NotNull final String path = "/path/to/delete";
    	setupServer("*", new HttpRequestHandler() {

			@Override
			public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response, HttpContext context)
					throws HttpException, IOException {
                try {
                    assertEquals("Request path matches", request.getRequestLine().getUri(), path);
                    assertFalse("Content-Type not included", request.containsHeader("Content-Type"));
                    assertTrue("Custom header included", request.containsHeader("Foo"));
                    assertEquals("Custom header value", "Bar", request.getFirstHeader("Foo").getValue());
                    response.setStatusCode(200);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                // ignore
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                } catch ( Exception e ) {
                    System.out.println("Exception handling request: " + e);
                    e.printStackTrace();
                    latch.countDown();
                }
            }
		});

        @NotNull TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("DELETE");
        config.setHeaders(Collections.singletonMap("Foo", "Bar"));
        config.setIgnoreRequestBody(true);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        restNode.onMsg(ctx, msg);

        assertTrue("Server handled request", latch.await(10, TimeUnit.SECONDS));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());


        assertEquals("USER", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals("{}", dataCaptor.getValue());
    }

    @Test
    public void deleteRequestWithBody() throws IOException, InterruptedException {
        @NotNull final CountDownLatch latch = new CountDownLatch(1);
        @NotNull final String path = "/path/to/delete";
        setupServer("*", new HttpRequestHandler() {

            @Override
            public void handle(@NotNull HttpRequest request, @NotNull HttpResponse response, HttpContext context)
                    throws HttpException, IOException {
                try {
                    assertEquals("Request path matches", path, request.getRequestLine().getUri());
                    assertTrue("Content-Type included", request.containsHeader("Content-Type"));
                    assertEquals("Content-Type value", "text/plain;charset=ISO-8859-1",
                            request.getFirstHeader("Content-Type").getValue());
                    assertTrue("Content-Length included", request.containsHeader("Content-Length"));
                    assertEquals("Content-Length value", "2",
                            request.getFirstHeader("Content-Length").getValue());
                    assertTrue("Custom header included", request.containsHeader("Foo"));
                    assertEquals("Custom header value", "Bar", request.getFirstHeader("Foo").getValue());
                    response.setStatusCode(200);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(1000L);
                            } catch (InterruptedException e) {
                                // ignore
                            } finally {
                                latch.countDown();
                            }
                        }
                    }).start();
                } catch ( Exception e ) {
                    System.out.println("Exception handling request: " + e);
                    e.printStackTrace();
                    latch.countDown();
                }
            }
        });

        @NotNull TbRestApiCallNodeConfiguration config = new TbRestApiCallNodeConfiguration().defaultConfiguration();
        config.setRequestMethod("DELETE");
        config.setHeaders(Collections.singletonMap("Foo", "Bar"));
        config.setIgnoreRequestBody(false);
        config.setRestEndpointUrlPattern(String.format("http://localhost:%d%s", server.getLocalPort(), path));
        initWithConfig(config);

        @NotNull TbMsg msg = TbMsg.newMsg("USER", originator, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);
        restNode.onMsg(ctx, msg);

        assertTrue("Server handled request", latch.await(10, TimeUnit.SECONDS));

        @NotNull ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        @NotNull ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        @NotNull ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        @NotNull ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        @NotNull ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals("USER", typeCaptor.getValue());
        assertEquals(originator, originatorCaptor.getValue());
        assertNotSame(metaData, metadataCaptor.getValue());
        assertEquals("{}", dataCaptor.getValue());
    }

}
