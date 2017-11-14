/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.eventStream;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.AbstractMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.fpc.utils.ErrorLog;

/**
 * A class that parses the request stream and enqueues creates event-data pairs for processing
 */
public class ParseStream implements Runnable {
	public static BlockingQueue<Map.Entry<String, ByteBuffer>> blockingQueue = new LinkedBlockingQueue<Map.Entry<String, ByteBuffer>>();
	private CharBuffer charBuf;
	private String partialChunk; //Chunked data left over at the end of the buffer
	boolean chunkedBuffer;

	private String getLine(){
		StringBuilder s = new StringBuilder();
		if(this.chunkedBuffer){
			while(this.charBuf.hasRemaining()){
				char ch = this.charBuf.get();
				if(ch == '\n'){
					this.partialChunk += s.toString();
					this.chunkedBuffer = false;
					return this.partialChunk;
				}
				if(ch =='\r')
					continue;
				s.append(ch);
			}
		}
		while(this.charBuf.hasRemaining()){
			char ch = this.charBuf.get();
			if(ch == '\n'){
				String line = s.toString();
				return line.length() > 0 ? line : null;
			}
			if(ch =='\r')
				continue;
			s.append(ch);
			if(!this.charBuf.hasRemaining()){
				this.partialChunk = s.toString();
				this.chunkedBuffer = true;
			}
		}
		return null;
	}


	@Override
	public void run() {
		chunkedBuffer = false;
		String event = null,data = null;
		System.out.println("Parse Stream Started");
		while(true){
			try {
				Map.Entry<String, ByteBuffer> entry = blockingQueue.take();
				ByteBuffer byteBuffer = entry.getValue();
//				byteBuffer.rewind();
//				byte[] byteArray = new byte[byteBuffer.remaining()];
//        		System.arraycopy(byteBuffer.array(), 0, byteArray, 0, byteBuffer.remaining());
//        		byteBuffer =  ByteBuffer.wrap(byteArray);
				this.charBuf = Charset.forName("ISO-8859-1").decode(byteBuffer);
				//System.out.println(this.charBuf);
				while(this.charBuf.hasRemaining()){
					String line = getLine();
					if(line != null){
						if(line.startsWith("event")){
							event = line.split(":",2)[1];

						} else if(line.startsWith("data")){
							data = line.split(":",2)[1];
							if(event != null && data != null)
								NBEventPool.getInstance().getWorker().getBlockingQueue().put(new AbstractMap.SimpleEntry<String,Map.Entry<String,String>>(entry.getKey(), new AbstractMap.SimpleEntry<String,String>(event, data)));
							event = null;
							data = null;
						}
					}

				}
			} catch (InterruptedException e) {
				ErrorLog.logError(e.getLocalizedMessage(),e.getStackTrace());
			}
		}
	}

}
