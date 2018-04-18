/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty.client;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.fpc.netty.handler.RequestHandler;
import org.opendaylight.fpc.netty.handler.RequestParserYang;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;

public class FPCClientInitializer extends ChannelInitializer<SocketChannel> {

	private String clientUrl;
		
    ExecutorService reqhandlerExecutors = Executors.newFixedThreadPool(11);
	ExecutorService parseExecutors = Executors.newFixedThreadPool(6);	
	public FPCClientInitializer(String clientUrl) {
		this.clientUrl= clientUrl;
	}

	@Override
	public void initChannel(SocketChannel ch) {
		
		
		ChannelPipeline pipeline = ch.pipeline();

		//pipeline.addLast(new LoggingHandler(LogLevel.DEBUG));	
		pipeline.addLast(new DelimiterBasedFrameDecoder(8192, new ByteBuf[] {
                     Unpooled.wrappedBuffer(new byte[] { '\r', '\n' })
                }));
		pipeline.addLast(new RequestParserYang(parseExecutors));
        pipeline.addLast(new RequestHandler(reqhandlerExecutors,clientUrl));
	 
	}
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		reqhandlerExecutors.shutdownNow();
		parseExecutors.shutdownNow();
		ctx.fireChannelInactive();
	}
}
