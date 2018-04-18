/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty;

import static org.opendaylight.fpc.utils.FPCConstants.SERVER_PORT;

import java.util.HashMap;
import java.util.Map;

import org.opendaylight.fpc.netty.handler.ResponseStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

public class FPCSSEServer
{    
	
	private static final Logger logger = LoggerFactory.getLogger(ResponseStreamHandler.class);
	
	public static Map<ChannelHandlerContext, Thread> clientMap = new HashMap<>();
	
    private ChannelFuture channel;
    private final EventLoopGroup masterGroup;
    private final EventLoopGroup slaveGroup;
   
    
    public FPCSSEServer()
    {
        masterGroup = new NioEventLoopGroup();
        slaveGroup = new NioEventLoopGroup(); 
        
        logger.info("Epoll available : "+Epoll.isAvailable());
                 
        //slaveGroup = new NioEventLoopGroup(0,new AffinityThreadFactory("atf_wrk", AffinityStrategies.SAME_CORE)); 
        
        //masterGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(1) : new NioEventLoopGroup(1);
        //slaveGroup = Epoll.isAvailable() ? new EpollEventLoopGroup(2) : new NioEventLoopGroup(2);
            
    }

    public void start()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() { shutdown(); }
        });
        
        try
        {
        	        	
            final ServerBootstrap bootstrap =
                new ServerBootstrap()
                    .group(masterGroup, slaveGroup)
                  //  .channel(Epoll.isAvailable() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>(){
                        @Override
                        public void initChannel(final SocketChannel ch) throws Exception
                        {                           
                           
                           // ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                            ch.pipeline().addLast("codec", new HttpServerCodec());
                          //  ch.pipeline().addLast("encode", new ResponseEncoder());
                        	ch.pipeline().addLast("aggregator", new HttpObjectAggregator(512*1024));
                        	ch.pipeline().addLast("request", new ResponseStreamHandler());                        	
                        	
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            logger.info("Server listening {} ",SERVER_PORT);
            channel = bootstrap.bind(SERVER_PORT).sync();
        }
        catch (final InterruptedException e) { }
    }
    
    public void shutdown()
    {
        slaveGroup.shutdownGracefully();
        masterGroup.shutdownGracefully();

        try
        {
            channel.channel().closeFuture().sync();
        }
        catch (InterruptedException e) { }
    }

    public static void main(String[] args)
    {
    	//InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());
        new FPCSSEServer().start();
        
    }
}
