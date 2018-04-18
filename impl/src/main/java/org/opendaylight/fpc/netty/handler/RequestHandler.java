/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty.handler;

import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import org.json.JSONObject;
import org.opendaylight.fpc.impl.FpcagentDispatcher;
import org.opendaylight.fpc.netty.ConfigResponseService;
import org.opendaylight.fpc.utils.EventDataRequest;
import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.netconf.sal.restconf.api.JSONRestconfService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yangtools.yang.common.OperationFailedException;
import com.google.common.base.Optional;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class RequestHandler extends SimpleChannelInboundHandler<EventDataRequest> {

	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
        public static Map<Integer,String> clientIdToUri = new ConcurrentHashMap<Integer,String>();
	public static final String CONFIGURE = "configure";
	public static final String REGISTER_CLIENT = "register_client";
	public static final String UNREGISTER_CLIENT = "deregister_client";
	private FpcagentDispatcher dispatcher;
	private ExecutorService execService;

	private Object service;
	private String clientUrl;

	public RequestHandler(ExecutorService execService, String clientUrl) {
		this.execService = execService;
		this.clientUrl = clientUrl;
		Object[] instances = FpcCodecUtils.getGlobalInstances(JSONRestconfService.class, this);
		this.service = (instances != null) ? (JSONRestconfService) instances[0] : null;
		dispatcher = new FpcagentDispatcher();
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, EventDataRequest msg) throws Exception {

//		logger.info("RequestHandler++");
		logger.debug("{} received {}", ctx.channel(), msg);

		switch (msg.getEvent().trim()) {

		case CONFIGURE:
			execService.execute(new Runnable() {

				@Override
				public void run() {

					ConfigureInput configInput = (ConfigureInput) msg.getData();
					ConfigureInputBuilder cib = new ConfigureInputBuilder(configInput);
					cib.setTimestamp(configInput.getTimestamp() + ",ISE:" + System.currentTimeMillis());
					ConfigureInput inputRequest = cib.build();

					try {
						
						ConfigureOutput configureOutput = dispatcher.configure(inputRequest).get().getResult();
					
					/*	cib = new ConfigureInputBuilder(inputRequest);
						cib.setTimestamp(inputRequest.getTimestamp() + ",AQE:" + System.currentTimeMillis());
						ctx.fireChannelRead(cib.build());*/
						
					} catch (InterruptedException e) {
						logger.error("Error while configure call ",e);
					} catch (ExecutionException e) {
						logger.error("Error while configure call ",e);
					}
					// ConfigResponseService.clientUriToOutputQueue.offer(new
					// AbstractMap.SimpleEntry<>(clientUrl, new
					// AbstractMap.SimpleEntry<>("application/json;/restconf/operations/ietf-dmm-fpcagent:configure",
					// configureOutput)));			

				}
			});
			break;
		case REGISTER_CLIENT:
            Optional<String> registerClientOutput = null;
            try {
                registerClientOutput = ((JSONRestconfService) service).invokeRpc("fpc:register_client",
                        Optional.of((String) msg.getData()));
            } catch (OperationFailedException e) {
                e.printStackTrace();
            }
            addClientIdToUriMapping(registerClientOutput.get(),clientUrl);
	   
            ConfigResponseService.registerClientQueue.offer(new AbstractMap.SimpleEntry(clientUrl, "event:application/json;/restconf/operations/fpc:register_client\ndata:"+registerClientOutput.get()+"\r\n"));
            break;

		case UNREGISTER_CLIENT:
            Optional<String> deregisterClientOutput = null;
            try {
                deregisterClientOutput = ((JSONRestconfService) service).invokeRpc("fpc:deregister_client",
                        Optional.of((String) msg.getData()));
            } catch (OperationFailedException e) {
                e.printStackTrace();
            }

            ConfigResponseService.registerClientQueue.offer(new AbstractMap.SimpleEntry(clientUrl, "event:application/json;/restconf/operations/fpc:deregister_client\ndata:"+deregisterClientOutput.get()+"\r\n"));
            removeClientIdToUriMapping((String) msg.getData());
            default:
                break;
		}

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.channel().close();
	}
    private void removeClientIdToUriMapping(String deregisterClientInput) {
        JSONObject deregisterJSON = new JSONObject(deregisterClientInput);
        clientIdToUri.remove(Integer.parseInt(deregisterJSON.getJSONObject("input").getString("client-id")));
    }
    private void addClientIdToUriMapping(String registerClientOutput, String uri) {
        JSONObject registerJSON = new JSONObject(registerClientOutput);
        clientIdToUri.put(Integer.parseInt(registerJSON.getJSONObject("output").getString("client-id")), uri);
    }

}

