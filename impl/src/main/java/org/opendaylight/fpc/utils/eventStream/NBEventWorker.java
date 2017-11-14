/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.eventStream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.json.JSONObject;
import org.opendaylight.fpc.NB.ClientSocket;
import org.opendaylight.fpc.NB.SendNotification;
import org.opendaylight.fpc.NB.SendResponse;
import org.opendaylight.fpc.NB.ServerSocket;
import org.opendaylight.fpc.impl.FpcagentDispatcher;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.FpcCodecUtils;
import org.opendaylight.fpc.utils.Worker;
import org.opendaylight.netconf.sal.restconf.api.JSONRestconfService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;

/**
 * Implements a Worker to process event-data pairs sent by the FPC Client
 */
public class NBEventWorker implements Worker {
	public static Map<Integer,String> clientIdToUri = new ConcurrentHashMap<Integer,String>();
	private CountDownLatch startSignal;
	//client-uri, event string, data string
	private BlockingQueue<Entry<String,Entry<String, String>>> blockingQueue;
	private boolean run;
	Object service;
	public static final QName TOP_ODL_FPC_QNAME =
            QName.create("urn:ietf:params:xml:ns:yang:fpcagent", "2016-08-03","input").intern();
    static final YangInstanceIdentifier inputYII =
            YangInstanceIdentifier.of(TOP_ODL_FPC_QNAME);
    static final InstanceIdentifier<ConfigureInput> inputII =
            InstanceIdentifier.create(ConfigureInput.class);
    FpcCodecUtils fpcCodecUtils;

	/**
	 * Constructor
	 * @param startSignal - Startr signal for the worker
	 * @param blockingQueue = Blocking queue for the worker
	 */
	public NBEventWorker(CountDownLatch startSignal, BlockingQueue<Map.Entry<String,Map.Entry<String, String>>> blockingQueue){
		this.blockingQueue = blockingQueue;
		this.startSignal = startSignal;
		Object[] instances =  FpcCodecUtils.getGlobalInstances(JSONRestconfService.class, this);
    	this.service = (instances != null) ? (JSONRestconfService) instances[0] : null;
    	try {
            fpcCodecUtils = FpcCodecUtils.get(ConfigureInput.class, inputYII);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
	}

	 /**
	  * Returns the blocking queue of the worker
	 * @return Blocking Queue
	 */
	public BlockingQueue<Map.Entry<String,Map.Entry<String, String>>> getBlockingQueue() {
        return blockingQueue;
    }

	@Override
	public void run() {
		this.run = true;
		String clientId;
		Entry<String,Entry<String, String>> contents = null;
		FpcagentDispatcher dispatcher = new FpcagentDispatcher();
		while(run) {
			try{
				contents = blockingQueue.take();
				if(service != null){
					if(contents.getValue().getKey().contains("ietf-dmm-fpcagent:configure")){
						ConfigureInput input = fpcCodecUtils.ConfigureInputFromJsonString(contents.getValue().getValue());
						dispatcher.configure(input);
						//Optional<String> output = ((JSONRestconfService) service).invokeRpc("ietf-dmm-fpcagent:configure", Optional.of(contents.getValue().getValue()));
						//ConfigureService.blockingQueue.add(new AbstractMap.SimpleEntry(contents.getKey(), "event:application/json;/restconf/operations/ietf-dmm-fpcagent:configure\ndata:"+output.get()+"\r\n"));
					}
					else if(contents.getValue().getKey().contains("fpc:register_client")){
						Optional<String> output = ((JSONRestconfService) service).invokeRpc("fpc:register_client", Optional.of(contents.getValue().getValue()));
						addClientIdToUriMapping(output.get(),contents.getKey());
						String response = "event:application/json;/restconf/operations/fpc:register_client\ndata:"+output.get();
						response = Integer.toHexString(response.length()) + "\r\n" + response + "\r\n";
						SendResponse.registerClientQueue.add(new AbstractMap.SimpleEntry(contents.getKey(), response));
					}
					else if(contents.getValue().getKey().contains("fpc:deregister_client")){
						Optional<String> output = ((JSONRestconfService) service).invokeRpc("fpc:deregister_client", Optional.of(contents.getValue().getValue()));
						removeClientIdToUriMapping(contents.getValue().getValue());
						ClientSocket.closeClient();
					}
				}
			} catch (Exception e){
				//ErrorLog.logError("Error in NBEventWorker.",e.getStackTrace());
				//ErrorLog.logError("ClientUri:Event:Data val = "+contents.toString());
				try{
				if(contents.getValue().getKey().contains("ietf-dmm-fpcagent:configure")){
					Thread.sleep(1000);
					ConfigureInput input = fpcCodecUtils.ConfigureInputFromJsonString(contents.getValue().getValue());
					dispatcher.configure(input);
					//Optional<String> output = ((JSONRestconfService) service).invokeRpc("ietf-dmm-fpcagent:configure", Optional.of(contents.getValue().getValue()));
					//ConfigureService.blockingQueue.add(new AbstractMap.SimpleEntry(contents.getKey(), "event:application/json;/restconf/operations/ietf-dmm-fpcagent:configure\ndata:"+output.get()+"\r\n"));
				}
				} catch (Exception e1) {
					ErrorLog.logError("Error in NBEventWorker.",e1.getStackTrace());
					ErrorLog.logError("ClientUri:Event:Data val = "+contents.toString());
				}
			}
		}
	}

	/**
	 * Add a FPC client URI to FPC client id mapping
	 * @param registerClientOutput - Output of the register_cleint rpc
	 * @param uri - FPC Client URI
	 */
	private void addClientIdToUriMapping(String registerClientOutput, String uri) {
		JSONObject registerJSON = new JSONObject(registerClientOutput);
		clientIdToUri.put(Integer.parseInt(registerJSON.getJSONObject("output").getString("client-id")), uri);
	}

	/**
	 * Remove a FPC client URI to FPCClient Id mapping
	 * @param deregisterClientInput - Input String of deregister_client rpc
	 */
	private void removeClientIdToUriMapping(String deregisterClientInput) {
		JSONObject deregisterJSON = new JSONObject(deregisterClientInput);
		String clientId = deregisterJSON.getJSONObject("input").getString("client-id");
		String clientUri = clientIdToUri.get(Integer.parseInt(clientId));
		try {
			if(clientUri != null){
				if(ServerSocket.responseSocketMap.get(clientUri).isOpen())
					ServerSocket.responseSocketMap.get(clientUri).close();
				ServerSocket.responseSocketMap.remove(clientUri);
			}
			if(ServerSocket.notificationSocketMap.get(clientId).isOpen())
				ServerSocket.notificationSocketMap.get(clientId).close();
			ServerSocket.notificationSocketMap.remove(clientId);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		clientIdToUri.remove(Integer.parseInt(clientId));

	}

	@Override
	public void close() throws Exception {
		this.run = false;
	}

	@Override
	public void stop() {
	}

	@Override
	public void open() {
	}

	@Override
	public boolean isOpen() {
		return false;
	}

}
