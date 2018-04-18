/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.oio.OioSocketChannel;

import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.ChannelOption;


public final class FPCClient extends Thread {

	String url;
	boolean run = false;
	private ChannelFuture channel;

	public FPCClient() {

	}

	public String getUrl() {
		return this.url;
	}

	public void setUrl(String url) {
		this.url = url;
		this.run = true;
	}
	
	public void init() {
		System.out.println("init url : "+url);
		
		String host;
		int port;
		URI uriSimple;

		try {
			uriSimple = new URI(url);
			host = uriSimple.getHost() == null ? "127.0.0.1" : uriSimple.getHost();
			port = uriSimple.getPort();

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			run = false;
			return;
		}

		// Configure the client.
		//	EventLoopGroup group = new NioEventLoopGroup();
	        EventLoopGroup group = new OioEventLoopGroup();

		try {
			Bootstrap b = new Bootstrap();

		//	b.group(group).channel(NioSocketChannel.class).handler(new FPCClientInitializer(this.url));
			b.group(group).channel(OioSocketChannel.class).handler(new FPCClientInitializer(this.url));	
			
		/*	RecvByteBufAllocator recvByteBufAllocator = new FixedRecvByteBufAllocator(2048*10);
			b.option(ChannelOption.RCVBUF_ALLOCATOR, recvByteBufAllocator);
			b.option(ChannelOption.SINGLE_EVENTEXECUTOR_PER_GROUP,false);
			b.option(ChannelOption.MAX_MESSAGES_PER_READ, 1000);
		*/
		//	Channel ch = b.connect(host, port).sync().channel();
            channel = b.connect(host, port).sync();
            Channel ch = channel.channel();

			URI uriGet = new URI(url.toString());
			
			StringBuilder strB = new  StringBuilder();
			strB.append("GET /request HTTP/1.1\n");
			strB.append("host: "+uriGet.toString()+"\n");			
			strB.append("connection: Keep-Alive\n");
			strB.append("cache-control: no-cache, no-store\r\n");
			strB.append("\r\n");
            ch.writeAndFlush(Unpooled.copiedBuffer(strB.toString().getBytes()));
            //Put currentThread in deadlock until someone kill or interrupt it
            Thread.currentThread().join();
                      

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		//} catch (IOException e) {
			// TODO Auto-generated catch block
		//	e.printStackTrace();
		} finally {
			// Shut down executor threads to exit.
			group.shutdownGracefully();
            try {
                channel.channel().closeFuture().sync();
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

		}

	}//

	public static void main(String[] args) throws Exception {

		FPCClient client = new FPCClient();
		client.setUrl("http://192.168.56.104:9997/request");
		client.init();

	}

	@Override
	public void run() {
		System.out.println("Sleeping 2 sec");
		try {
			Thread.sleep(1000 * 2);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		init();
	}// run

}
