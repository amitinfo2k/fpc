/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty.handler;

import static org.opendaylight.fpc.utils.FPCConstants.RPC_CONFIGURE;
import static org.opendaylight.fpc.utils.FPCConstants.RPC_REGISTER_CLIENT;
import static org.opendaylight.fpc.utils.FPCConstants.RPC_UNREGISTER_CLIENT;

import java.io.StringReader;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.fpc.utils.EventDataRequest;
import org.opendaylight.fpc.utils.yangtools.SchemaManager;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.fpc.rev150105.RegisterClientInput;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonParserStream;
import org.opendaylight.yangtools.yang.data.impl.schema.ImmutableNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.impl.schema.NormalizedNodeResult;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javassist.ClassPool;

/**
 * Echoes uppercase content of text frames.
 */
public class RequestDecoder extends ChannelInboundHandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(RequestDecoder.class);
	
	private static final String EVENT = "event";
	private static final String CHAR_COLON = ":";
	private static final String CHAR_NEW_LINE = "\n";
        private static final String CHAR_COMMA = ",";
        private static final String STR_TIMESTAMP_PREFIX = ",\"timestamp\":\"RDE:";
	private static AtomicLong entrants = new AtomicLong(0L);
    private SchemaContext context;
    private BindingRuntimeContext bindingContext;
    private BindingNormalizedNodeCodecRegistry codecRegistry;
    private static final QName TOP_ODL_FPC_QNAME =
            QName.create("urn:ietf:params:xml:ns:yang:fpcagent", "2016-08-03", "fpcagent");
    static final YangInstanceIdentifier inputYII =
            YangInstanceIdentifier.of(TOP_ODL_FPC_QNAME);    

	private SchemaPath CONFIGURE_INPUT_PATH;
	private SchemaNode CONFIGURE;
	
	private SchemaNode REGISTER_CLIENT;
	private SchemaNode UNREGISTER_CLIENT;
	private SchemaPath REGISTER_CLIENT_INPUT_PATH;
	private SchemaPath UNREGISTER_CLIENT_INPUT_PATH;
	
	public RequestDecoder(){
		
	    logger.info("Building context");
     
        final ModuleInfoBackedContext moduleContext = SchemaManager.get();

        try {
			moduleContext.addModuleInfos(Collections.singleton(BindingReflections.getModuleInfo(ConfigureInput.class)));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        context =  moduleContext.tryToCreateSchemaContext().get();
        if (context == null) {
            logger.info("Context could not be built");
        } else {
            logger.info("Context built");
        }

        logger.info("Building Binding Context");
        bindingContext = BindingRuntimeContext.create(moduleContext, context);

        logger.info("Building Binding Codec Factory");
        final BindingNormalizedNodeCodecRegistry bindingStreamCodecs = new BindingNormalizedNodeCodecRegistry(StreamWriterGenerator.create(JavassistUtils.forClassPool(ClassPool.getDefault())));
        bindingStreamCodecs.onBindingRuntimeContextUpdated(bindingContext);
        codecRegistry = bindingStreamCodecs;
        Module fpcagent = context.findModuleByName("ietf-dmm-fpcagent", null);
        Set<RpcDefinition> rpcList = fpcagent.getRpcs();
        for(RpcDefinition rpc : rpcList){
        	if(rpc.getQName().getLocalName().equals("configure")){
        		CONFIGURE = rpc;
        		CONFIGURE_INPUT_PATH = rpc.getInput().getPath();
        		//CONFIGURE_OUTPUT_PATH = rpc.getOutput().getPath();
        		break;
        	}
        	if(rpc.getQName().getLocalName().equals("register_client")){
        		REGISTER_CLIENT = rpc;
        		REGISTER_CLIENT_INPUT_PATH=rpc.getInput().getPath();
        	//	REGISTER_CLIENT_OUTPUT_PATH=rpc.getOutput().getPath();
        		
        	}
        	if(rpc.getQName().getLocalName().equals("unregister_client")){
        		UNREGISTER_CLIENT = rpc;
        		UNREGISTER_CLIENT_INPUT_PATH=rpc.getInput().getPath();
        	//	UNREGISTER_CLIENT_OUTPUT_PATH=rpc.getOutput().getPath();
        	}
        	
        }
        logger.info("Mapping service built");
		
	}
	
		
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object frame) throws Exception {
		logger.debug("{} received {}", ctx.channel(), frame);
				
		if(frame instanceof String){
			String eventData=(String)frame;			
			if(eventData.startsWith(EVENT)){				
				String[] array = eventData.split(CHAR_NEW_LINE);
				String event = array[0].split(CHAR_COLON)[2];
				String data = array[1].split(CHAR_COLON, 2)[1];
				logger.debug("event : {} , data {} ", event, data);
				Object reqObj = null;
				switch (event) {
				case RPC_REGISTER_CLIENT:
				case RPC_UNREGISTER_CLIENT:					
					reqObj = data;
                                        break;
				case RPC_CONFIGURE:
					    data = data.split(CHAR_COMMA, 2)[0] +STR_TIMESTAMP_PREFIX + System.currentTimeMillis() + "\","
							+ data.split(CHAR_COMMA, 2)[1];
				        reqObj= (ConfigureInput) parseRequest(data,CONFIGURE,CONFIGURE_INPUT_PATH);				
					
					break;
				default:
					break;
				}
				ctx.fireChannelRead(new EventDataRequest(event, reqObj));	
				
				long entries =entrants.incrementAndGet();			
				if ((entries % 100) == 0) {
				       logger.info("[Amit] Entries received = {} ", entries);
			        }
			}			
		}		
		
	}//end of method
	
	/**
	 *  Parse Yang JSON string to Java Object
	 *  
	 * @param jsonData
	 * @param rpcName
	 * @param rpcInputPath
	 * @return
	 */
	private Object parseRequest(String jsonData,SchemaNode rpcName,SchemaPath rpcInputPath){

		    NormalizedNodeResult result = new NormalizedNodeResult();
	        NormalizedNodeStreamWriter streamWriter = ImmutableNormalizedNodeStreamWriter.from(result);
	        JsonParserStream jsonParser = JsonParserStream.create(streamWriter, context, rpcName);
	        try{
	        	jsonParser.parse(new JsonReader(new StringReader(jsonData)));
	        } catch(Exception e){
	        	e.printStackTrace();
	        	logger.error("[Amit] Error while parsing input {} ",e);
	        }
	        NormalizedNode<?, ?> transformedInput = result.getResult();			      	        
		
		return codecRegistry.fromNormalizedNodeRpcData(rpcInputPath, (ContainerNode) transformedInput);
	}
	
}//end of class
