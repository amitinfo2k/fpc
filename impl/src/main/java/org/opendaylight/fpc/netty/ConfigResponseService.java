/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.utils.ErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

/**
 * A servlet context listener that sends event-data pairs for config result
 * notifications
 */
public class ConfigResponseService {
	private static final Logger logger = LoggerFactory.getLogger(ConfigResponseService.class);
	public static ConcurrentLinkedQueue<Map.Entry<String, Map.Entry<Transaction, String>>> clientUriToOutputQueue = new ConcurrentLinkedQueue<Map.Entry<String, Map.Entry<Transaction, String>>>();
	public static ConcurrentLinkedQueue<Map.Entry<String, String>> registerClientQueue = new ConcurrentLinkedQueue<Map.Entry<String, String>>();
	public static Map<String, ChannelHandlerContext> clientURIContextMap = new ConcurrentHashMap<>();
	private static AtomicLong deentrants = new AtomicLong(0L);

	Thread thread = new Thread(new Runnable() {

		@Override
		public void run() {
			String clientUri = null;
			while (true) {
				try {
					Map.Entry<String, Map.Entry<Transaction, String>> entry = clientUriToOutputQueue.poll();
					if (entry != null) {
						clientUri = entry.getKey();
						ChannelHandlerContext ctx = clientURIContextMap.get(clientUri);
						ctx.writeAndFlush(Unpooled.copiedBuffer(entry.getValue().getValue().getBytes()));
						entry.getValue().getKey().setResponseSent();

						long entries = deentrants.incrementAndGet();
						if ((entries % 500) == 0) {
							logger.info("[Amit]Entried ackldged = {} ", entries);
						}

					}
				} catch (Exception e) {
					ErrorLog.logError("Exception - Cannot write to client", e.getStackTrace());
				}
			}

		}

	});

	Thread thread1 = new Thread(new Runnable() {

		@Override
		public void run() {
			String clientUri = null;
			while (true) {
				while (true) {
					try {
						Map.Entry<String, String> entry = registerClientQueue.poll();
						if (entry != null) {
							clientUri = entry.getKey();
							ChannelHandlerContext ctx = clientURIContextMap.get(clientUri);
							ctx.writeAndFlush(Unpooled.copiedBuffer(entry.getValue().getBytes()));
						}
					} catch (Exception e) {
						ErrorLog.logError("Exception - Cannot write to client", e.getStackTrace());
						clientURIContextMap.remove(clientUri);
						break;
					}
				}
			}

		}

	});

	public void start() {
		thread1.start();
		thread.start();
	}
}// class
