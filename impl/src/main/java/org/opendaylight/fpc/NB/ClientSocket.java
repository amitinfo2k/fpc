/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.NB;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.fpc.utils.eventStream.ParseStream;

public class ClientSocket {
	private static boolean run;
	private static ByteBuffer[] byteBufferList = new ByteBuffer[50];
	private static int count=0;
	private static ByteBuffer clone(ByteBuffer readBuffer){
		ByteBuffer clone = ByteBuffer.allocate(readBuffer.capacity());
		readBuffer.rewind();//copy from the beginning
		clone.put(readBuffer);
		readBuffer.rewind();
		clone.flip();
		return clone;
	}

	public static void closeClient(){

			run = false;

	}

	public static void sendRequest(String clientUri){
		String origClientUri = clientUri;
	clientUri = clientUri.trim();
	if(clientUri.startsWith("http://"))
		clientUri = clientUri.substring(7).trim();
		String[] clientInfo = clientUri.split(":");
		String[] clientInfoSplit = clientInfo[1].split("/");
		String requestString = "GET /"+clientInfoSplit[1]+" HTTP/1.1\r\n"+
				   "Host: "+clientUri+"\r\n"+
				   "Connection: Keep-Alive\r\n\r\n";

		try {
			AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
			Future<Void> future = client.connect(new InetSocketAddress(InetAddress.getByName(clientInfo[0]), Integer.parseInt(clientInfoSplit[0])));
			future.get();
			ByteBuffer buffer = ByteBuffer.wrap(requestString.getBytes());
		    Future<Integer> writeResult = client.write(buffer);
		    writeResult.get();
		    run = true;
		    ByteBuffer readBuffer = ByteBuffer.allocate(32768);
		    count = 0;
		    while(run){
		    	//byteBufferList[i] = ByteBuffer.allocate(2000);
		    	if(client.isOpen()){
		    		try {
			    	Future<Integer> readResult = client.read(readBuffer);
			    	if(readResult.get() != -1) {
				    	readBuffer.rewind();
				    	byte[] byteArray = new byte[readBuffer.remaining()];
		        		System.arraycopy(readBuffer.array(), 0, byteArray, 0, readBuffer.remaining());
				    	ByteBuffer newReadBuffer = ByteBuffer.wrap(byteArray);
						ParseStream.blockingQueue.put(new AbstractMap.SimpleEntry<String, ByteBuffer>(origClientUri, newReadBuffer));
						readBuffer = ByteBuffer.allocate(32768);
						count++;
						if(count%50 == 0){
							System.out.println(count);
						}
				    }} catch(Exception e){
				    	e.printStackTrace();
				    }
			    	//String echo = new String(readBuffer.array()).trim();
			    	//readBuffer = ByteBuffer.allocate(2000);
			    	//if(echo.contains("deregister")){
			    		//break;
			    	//}
		    	}
		    }
			client.close();
		    //client.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
