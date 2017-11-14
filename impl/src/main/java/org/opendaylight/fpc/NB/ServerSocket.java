/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.NB;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.json.JSONObject;

public class ServerSocket {
	private static final byte CR = 0x0d;
	private static final byte LF = 0x0a;
	//key = client-uri, value = Async Socket Channel to write response to
	public static Map<String,AsynchronousSocketChannel> responseSocketMap = new ConcurrentHashMap<String,AsynchronousSocketChannel>();
	public static Map<String,AsynchronousSocketChannel> notificationSocketMap = new ConcurrentHashMap<String,AsynchronousSocketChannel>();

	public static void start(){
		AsynchronousServerSocketChannel listener;
		try {
			listener = AsynchronousServerSocketChannel.open().bind(new InetSocketAddress(8070));
			listener.accept(null, new CompletionHandler<AsynchronousSocketChannel,Void>() {
				public void completed(AsynchronousSocketChannel ch, Void att) {
					// accept the next connection
					listener.accept(null, this);

					// handle this connection
					handle(ch);
				}
				public void failed(Throwable exc, Void att) {

				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	public static void handle(AsynchronousSocketChannel channel){
		ByteBuffer byteBuffer = ByteBuffer.allocate(5000);
		channel.read(byteBuffer, null, new CompletionHandler<Integer,Void>(){

			@Override
			public void completed(Integer result, Void attachment) {
				boolean header = false;
				//byteBuffer.flip();
				if(result > 0){
					byteBuffer.position(0);
					while(byteBuffer.hasRemaining()){
						byte currChar = byteBuffer.get();
						if(currChar == ':'){
							header = true;
							continue;
						}
						if(currChar == 'H'){
							currChar = byteBuffer.get();
							if(currChar == 'T'){
								currChar = byteBuffer.get();
								if(currChar == 'T'){
									currChar = byteBuffer.get();
									if(currChar == 'P'){
										header = true;
										continue;
									}
								}
							}
						}
						if(currChar == LF){
							if(header == true){
								header = false;
								continue;
							}
							else { //End of headers
								System.out.println("End of headers");
								break;
							}
						}
					}
					byte[] dst = new byte[byteBuffer.limit() - byteBuffer.position()];
					byteBuffer.get(dst, 0, byteBuffer.limit() - byteBuffer.position());
					if(dst.length > 0) {
						JSONObject jsonObj =  new JSONObject(new String(dst));
						if(jsonObj.has("client-uri")){
							String clientUri = jsonObj.getString("client-uri");
							responseSocketMap.put(clientUri, channel);
							String OK = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nTransfer-Encoding: chunked\r\nServer: FPC Agent\r\n\r\n";
							Future<Integer> future = channel.write(ByteBuffer.wrap(OK.getBytes()));
							try {
								future.get();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ExecutionException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							SendResponse.init();
							//Initiate the request stream
							ClientSocket.sendRequest(clientUri);
						} else if( jsonObj.has("client-id")){
							String clientId = jsonObj.getString("client-id");
							notificationSocketMap.put(clientId, channel);
							String OK = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nTransfer-Encoding: chunked\r\nServer: FPC Agent\r\n\r\n";
							Future<Integer> future = channel.write(ByteBuffer.wrap(OK.getBytes()));
							try {
								future.get();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ExecutionException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							SendNotification.init();
						}
					}
				}

			}

			@Override
			public void failed(Throwable exc, Void attachment) {
				// TODO Auto-generated method stub

			}

		});
	}

}
