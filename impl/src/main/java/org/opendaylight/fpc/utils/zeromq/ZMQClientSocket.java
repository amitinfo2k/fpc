/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.zeromq;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import org.opendaylight.fpc.utils.ErrorLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.zeromq.ZContext;

import zmq.Ctx;
import zmq.ZMQ;

/**
 * ZMQ Client Socket.
 */
public class ZMQClientSocket extends ZMQBaseSocket {
    private static final Logger logger = LoggerFactory.getLogger(ZMQClientSocket.class);
    private BlockingQueue<ByteBuffer> blockingQueue;
    private AtomicLong entrants = new AtomicLong(0L);
    /**
     * Client Constructor.
     *
     * @param context - ZConext
     * @param address - ZMQ Address
     * @param socketType - ZMQ Socket Type
     * @param startSignal - threadpool start signal
     * @param blockingQueue - A byte buffer BlockingQueue assigned to the Client
     */
    public ZMQClientSocket(Ctx context, String address, int socketType,
            CountDownLatch startSignal, BlockingQueue<ByteBuffer> blockingQueue) {
        super(context,address,socketType, startSignal);
        this.context = ZMQ.createContext();
        this.blockingQueue = blockingQueue;
    }

    /**
     * Retrieves the Worker's Queue.
     * @return A BlockingQueue that accepts a byte buffer
     */
    public BlockingQueue<ByteBuffer> getBlockingQueue() {
        return blockingQueue;
    }

    @Override
    public void open() {
          
    	socket = context.createSocket(ZMQ.ZMQ_PUB);
		ZMQ.setSocketOption(socket, ZMQ.ZMQ_SNDHWM, 5000);
		socket.connect(address);
	//	   logger.info("Socket send rate size - "+ZMQ.getContextOption(context, ZMQ.ZMQ_RATE));
	 //       logger.info("Socket send hwm size - "+ZMQ.getContextOption(context, ZMQ.ZMQ_SNDHWM));
	   //     logger.info("Socket send buffer size - "+ZMQ.getContextOption(context, ZMQ.ZMQ_SNDBUF));
	        
	   
         
    }

    @Override
	public void close() {
    	this.run = false;
    	//socket.disconnect(address);
    	socket.close();
    	//context.destroySocket(socket);
		super.close();
	}

	@Override
    public void run() {
        this.run = true;
        logger.info("ZMQClientSocket RUN started - awaiting start signal");
        try {
            startSignal.await();
           // boolean result;
            while(run) {
                ByteBuffer bb = blockingQueue.take();
                //result=socket.send(bb.array());
                int result = ZMQ.send(socket, bb.array(),bb.array().length, 0);
                long entries = entrants.incrementAndGet();
                if ((entries % 500) == 0) {
                        logger.info("[Amit] ZMQ Entries written = {}  ", entries);
                        if ((entries % 4000) == 0){
                                LockSupport.parkNanos(10);                                
                        }
                }//    
                 logger.info("[Amit] ZMQ write result {} ",result);
            }
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
    }
}
