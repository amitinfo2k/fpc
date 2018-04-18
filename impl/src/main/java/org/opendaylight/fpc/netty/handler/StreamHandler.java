/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty.handler;

import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.fpc.utils.StreamEventData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class StreamHandler extends SimpleChannelInboundHandler<ByteBuf>{
	private static final Logger logger = LoggerFactory.getLogger(StreamHandler.class);
	private static final String EVENT = "event";
	private static final String CHAR_COLON = ":";
	private static final String CHAR_NEW_LINE = "\n";
	private static AtomicLong entrants = new AtomicLong(0L);
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ByteBuf frame) throws Exception {
		String eventData= frame.toString(CharsetUtil.UTF_8);			
		if(eventData.startsWith(EVENT)){				
			String[] array = eventData.split(CHAR_NEW_LINE);
			String event = array[0].split(CHAR_COLON)[2];
			String data = array[1].split(CHAR_COLON, 2)[1];
			logger.debug("event : {} , data {} ", event, data);
						
			ctx.fireChannelRead(new StreamEventData(event, data));	
			
			long entries =entrants.incrementAndGet();			
			if ((entries % 500) == 0) {
			       logger.info("[Amit] Entries received = {} ", entries);
		    }
		
		}
		frame.release();	
	}

}

