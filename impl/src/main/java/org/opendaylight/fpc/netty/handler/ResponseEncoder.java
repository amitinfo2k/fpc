/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty.handler;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.opendaylight.fpc.utils.yangtools.SchemaManager;
import org.opendaylight.netconf.sal.rest.api.RestconfNormalizedNodeWriter;
import org.opendaylight.netconf.sal.rest.impl.RestconfDelegatingNormalizedNodeWriter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yangtools.binding.data.codec.gen.impl.StreamWriterGenerator;
import org.opendaylight.yangtools.binding.data.codec.impl.BindingNormalizedNodeCodecRegistry;
import org.opendaylight.yangtools.sal.binding.generator.impl.ModuleInfoBackedContext;
import org.opendaylight.yangtools.sal.binding.generator.util.BindingRuntimeContext;
import org.opendaylight.yangtools.sal.binding.generator.util.JavassistUtils;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.util.BindingReflections;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier;
import org.opendaylight.yangtools.yang.data.api.schema.DataContainerChild;
import org.opendaylight.yangtools.yang.data.api.schema.stream.NormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONCodecFactory;
import org.opendaylight.yangtools.yang.data.codec.gson.JSONNormalizedNodeStreamWriter;
import org.opendaylight.yangtools.yang.data.codec.gson.JsonWriterFactory;
import org.opendaylight.yangtools.yang.model.api.Module;
import org.opendaylight.yangtools.yang.model.api.RpcDefinition;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.api.SchemaNode;
import org.opendaylight.yangtools.yang.model.api.SchemaPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultHttpResponse;
import javassist.ClassPool;

/**
 * Response encoder
 * 
 * --Serialize the response to JSON string
 * 
 */
public class ResponseEncoder extends ChannelOutboundHandlerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(ResponseEncoder.class);

	private static final String CHAR_NEW_LINE = "\n";
	private static final String EVENT_STR = "event:";
	private static final String DATA_STR = "data:";
	private static final String CHAR_CR_NEW_LINE = "\r\n";

	private static AtomicLong deentrants = new AtomicLong(0L);

	private SchemaContext context;
	private BindingRuntimeContext bindingContext;
	private BindingNormalizedNodeCodecRegistry codecRegistry;
	private static final QName TOP_ODL_FPC_QNAME = QName.create("urn:ietf:params:xml:ns:yang:fpcagent", "2016-08-03",
			"fpcagent");
	static final YangInstanceIdentifier inputYII = YangInstanceIdentifier.of(TOP_ODL_FPC_QNAME);

	private SchemaPath CONFIGURE_OUTPUT_PATH;
	private SchemaNode CONFIGURE;
	
	
	public ResponseEncoder(){
		
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
        		//CONFIGURE_INPUT_PATH = rpc.getInput().getPath();
        		CONFIGURE_OUTPUT_PATH = rpc.getOutput().getPath();
        		break;
        	}
        	
        	
        }
        logger.info("Mapping service built");	
		
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {

		if (msg instanceof DefaultHttpResponse)
			ctx.write(msg, promise);
		else {
			logger.debug("write++" + msg);
			Map.Entry<String, Object> entry = (Map.Entry<String, Object>) msg;
			String event = entry.getKey();
			Object data = entry.getValue();
						
			StringBuilder sb = new StringBuilder();
			sb.append(EVENT_STR).append(event).append(CHAR_NEW_LINE).append(DATA_STR);
			// 
			if(data instanceof ConfigureOutput ){				
				sb.append(toJSONStr((ConfigureOutput)data)).append(CHAR_CR_NEW_LINE);
			}//
			else
				sb.append(data).append(CHAR_CR_NEW_LINE);
			
			ctx.write(Unpooled.copiedBuffer(sb.toString().getBytes()), promise);

			long entries = deentrants.incrementAndGet();
			if ((entries % 50) == 0) {
				logger.info("Entries responded = {} ", entries);
			}

		} //
	}//

	private String toJSONStr(DataObject object) {
		final Writer writer = new StringWriter();
		JsonWriter jsonWriter = JsonWriterFactory.createJsonWriter(writer);
		final NormalizedNodeStreamWriter domWriter = JSONNormalizedNodeStreamWriter.createExclusiveWriter(
				JSONCodecFactory.create(context), CONFIGURE_OUTPUT_PATH,
				CONFIGURE_OUTPUT_PATH.getLastComponent().getNamespace(), jsonWriter);
		RestconfNormalizedNodeWriter restConfWriter = null;
		try {
			jsonWriter.beginObject();
			restConfWriter = (RestconfNormalizedNodeWriter) RestconfDelegatingNormalizedNodeWriter
					.forStreamWriter(domWriter);
			jsonWriter.name("output");
			for (DataContainerChild<? extends org.opendaylight.yangtools.yang.data.api.YangInstanceIdentifier.PathArgument, ?> child : codecRegistry
					.toNormalizedNodeRpcData((DataContainer) object).getValue()) {
				try {
					restConfWriter.write(child);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}
			jsonWriter.endObject();
			restConfWriter.flush();
			jsonWriter.flush();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return writer.toString();
	}

}
