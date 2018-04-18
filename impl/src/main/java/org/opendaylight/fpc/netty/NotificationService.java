/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import org.opendaylight.fpc.netty.handler.ResponseStreamHandler;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.Unpooled;
import java.util.concurrent.atomic.AtomicLong;


/**
 *  sends event-data pairs for config result notifications
 */
public class NotificationService implements Runnable {
	//key = client id, value = config-result-notification
//   public static ConcurrentLinkedQueue<Map.Entry<String,Map.Entry<String, Object>>> notificationQueue = new ConcurrentLinkedQueue<>();
  public static ConcurrentLinkedQueue<Map.Entry<String,String>> notificationQueue = new ConcurrentLinkedQueue<>();
  public static Map<String, ChannelHandlerContext> clientNotifyContextMap = new ConcurrentHashMap<>();

  private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
  private static AtomicLong deentrants = new AtomicLong(0L);
  private static final String CHAR_NEW_LINE = "\n";
	private static final String EVENT_STR = "event:";
	private static final String DATA_STR = "data:";
	private static final String CHAR_CR_NEW_LINE = "\r\n";


    
   @Override
	public void run() {
		while (true) {
			try {			
					Map.Entry<String, String> entry = notificationQueue.poll();
					if (entry != null) {
						ChannelHandlerContext ctx = clientNotifyContextMap.get(entry.getKey());
						ctx.writeAndFlush(Unpooled.copiedBuffer(entry.getValue().getBytes()));
						long entries = deentrants.incrementAndGet();
						if ((entries % 500) == 0) {
							logger.info("[Amit]Entried notified = {} ", entries);
						}	

					}
			} catch (Exception e) {
				e.printStackTrace();				
			}//
		}
	}//run   
}
