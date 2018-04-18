/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty.handler;

import static org.opendaylight.fpc.utils.FPCConstants.CLIENT_ID;
import static org.opendaylight.fpc.utils.FPCConstants.CLIENT_URL;
import static org.opendaylight.fpc.utils.FPCConstants.HTTP_HEADER_VAL_TXT_EVT_STREAM;
import static org.opendaylight.fpc.utils.FPCConstants.URI_RESPONSE;
import static org.opendaylight.fpc.utils.FPCConstants.URI_NOTIFICATION;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.fpc.netty.ConfigResponseService;
import org.opendaylight.fpc.netty.FPCSSEServer;
import org.opendaylight.fpc.netty.NotificationService;
import org.opendaylight.fpc.netty.client.FPCClient;
import com.google.gson.Gson;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

public class ResponseStreamHandler extends ChannelInboundHandlerAdapter {
	private static final Logger logger = LoggerFactory.getLogger(ResponseStreamHandler.class);
	
	public static Queue<Map.Entry<String, Object>> concurrentLinkedQueue = new ConcurrentLinkedQueue<>();
	private Gson gson = new Gson();
	// TODO support multi-client
	// Key = Client URL , Value = Channel context
	
	// Key = Client ID , Value = Channel context
//	public static Map<String, ChannelHandlerContext> clientNotifyContextMap = new ConcurrentHashMap<>();

	public ResponseStreamHandler() {
		
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.debug("{} received {} ", ctx, msg);
		
		if (msg instanceof FullHttpRequest) {

			final FullHttpRequest request = (FullHttpRequest) msg;

			HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			HttpHeaders headers = response.headers();

			if (URI_RESPONSE.equals(request.getUri())) {
				String reqData = request.content().toString(Charset.defaultCharset());
								
				Map<String, String> myMap = gson.fromJson(reqData, HashMap.class);
				String clientURL = myMap.get(CLIENT_URL);

				logger.info("Client URL :  {} ", clientURL);

				FPCClient fpcClient= new FPCClient();
				fpcClient.setUrl(clientURL);
				
				headers.set(HttpHeaders.Names.CONTENT_TYPE, HTTP_HEADER_VAL_TXT_EVT_STREAM);
				headers.set(HttpHeaders.Names.CACHE_CONTROL,"no-cache, no-store");
				headers.set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
				ctx.writeAndFlush(response);
				
				fpcClient.start();				
				FPCSSEServer.clientMap.put(ctx, fpcClient);			
				new ConfigResponseService().start();
				ConfigResponseService.clientURIContextMap.put(clientURL, ctx);
                                	

			} //
			else if(URI_NOTIFICATION.equals(request.getUri())){
				
				String requestContent=request.content().toString(Charset.defaultCharset());
				
				logger.info("notification stream request received : "+requestContent);
								
				Map<String, String> myMap = gson.fromJson(requestContent, HashMap.class);
				
				String clientId = myMap.get(CLIENT_ID);
				
				headers.set(HttpHeaders.Names.CONTENT_TYPE, HTTP_HEADER_VAL_TXT_EVT_STREAM);
				headers.set(HttpHeaders.Names.CACHE_CONTROL,"no-cache, no-store");
				headers.set(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
				ctx.writeAndFlush(response);				
				new Thread(new NotificationService()).start();			
				NotificationService.clientNotifyContextMap.put(clientId, ctx);
				System.out.println("notification stream request received : "+clientId);
			}//
		} //
	}//

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.INTERNAL_SERVER_ERROR,
				Unpooled.copiedBuffer(cause.getMessage().getBytes())));
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("notifyhanlder:channelInactive++");
		if(FPCSSEServer.clientMap.containsKey(ctx)){
			Thread client=FPCSSEServer.clientMap.remove(ctx);
			System.out.println("stoping client");	
			client.interrupt();
		}//	
		else{
			super.channelInactive(ctx);
		}
		System.out.println("notifyhanlder:channelInactive--");
		
	}
	
	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		System.out.println("notifyhanlder:channelUnregistered");
		super.channelUnregistered(ctx);
	}
}
