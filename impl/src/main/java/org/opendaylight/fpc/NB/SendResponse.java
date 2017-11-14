/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.NB;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletOutputStream;

import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.utils.ErrorLog;


/**
 * A servlet context listener that sends response (ConfigureOutput) events to clients.
 */
public class SendResponse {
	//key = client-id, value = configure-output
   public static BlockingQueue<Map.Entry<String,Map.Entry<Transaction,String>>> blockingQueue = new LinkedBlockingQueue<Map.Entry<String,Map.Entry<Transaction,String>>>();
   public static BlockingQueue<Map.Entry<String,String>> registerClientQueue = new LinkedBlockingQueue<Map.Entry<String,String>>();

   public static void init() {
	   Thread thread = new Thread(new Runnable(){

		@Override
		public void run() {
			String clientUri = null;
			AsynchronousSocketChannel socket = null;
			while(true)
			   {
				   try {
					   Map.Entry<String,Map.Entry<Transaction,String>> entry = blockingQueue.take();
					   clientUri = entry.getKey();
					   socket = ServerSocket.responseSocketMap.get(clientUri);
					   if(socket != null && socket.isOpen()){
						   Future<Integer> future = socket.write(ByteBuffer.wrap(entry.getValue().getValue().getBytes()));
						   future.get();
						   entry.getValue().getKey().setResponseSent();
					   }
				   } catch (Exception e){
					   ErrorLog.logError("Exception - Cannot write to client",	e.getStackTrace());
					   try {
						socket.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					   ServerSocket.responseSocketMap.remove(clientUri);
					   break;
				   }
			   }
		}

	   });
	   thread.start();

	   Thread thread1 = new Thread(new Runnable(){

			@Override
			public void run() {
				String clientUri = null;
				AsynchronousSocketChannel socket = null;
				while(true)
				   {
					   try {
						   Map.Entry<String,String> entry = registerClientQueue.take();
						   clientUri = entry.getKey();
						   socket = ServerSocket.responseSocketMap.get(clientUri);
						   if(socket != null){
							   Future<Integer> future = socket.write(ByteBuffer.wrap(entry.getValue().getBytes()));
							   future.get();
						   }
					   } catch (Exception e){
						   ErrorLog.logError("Exception - Cannot write to client",	e.getStackTrace());
						   try {
								socket.close();
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							   ServerSocket.responseSocketMap.remove(clientUri);
						   break;
					   }
				   }
			}

		   });
		   thread1.start();

   }

}
