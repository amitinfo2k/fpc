/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty.handler;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutorService;

import org.opendaylight.fpc.activation.cache.HierarchicalCache;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.cache.transaction.Transaction.OperationStatus;
import org.opendaylight.fpc.activation.impl.dpdkdpn.DpnAPI2;
import org.opendaylight.fpc.dpn.DpnHolder;
import org.opendaylight.fpc.tenant.TenantManager;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.ErrorTypeIndex;
import org.opendaylight.fpc.utils.NameResolver;
import org.opendaylight.fpc.utils.NameResolver.FixedType;
import org.opendaylight.fpc.utils.zeromq.ZMQClientPool;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ErrorTypeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.RefScope;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.DeleteOrQuery;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.Err;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.ErrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.fpc.context.Dpns;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ActivationHandler extends SimpleChannelInboundHandler<ConfigureInput> {
	private static final Logger logger = LoggerFactory.getLogger(ActivationHandler.class);
	// private DpnAPI2 api;
	private static Map<Long, Contexts> sessionContextsMap = new ConcurrentHashMap<>();
	private DpnAPI2 api;
	final static Long ZERO = new Long(0L);
	private static AtomicLong entrants = new AtomicLong(0L);
        private ExecutorService executors;

	public ActivationHandler() {
		api = new DpnAPI2(ZMQClientPool.getInstance().getWorker());
	}

	public ActivationHandler(ExecutorService executors) {                                                 
		this.executors = executors;
                api = new DpnAPI2(ZMQClientPool.getInstance().getWorker());          
        } 

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ConfigureInput in) throws Exception {

           executors.execute(new Runnable() {
                @Override
                public void run() {  

		long startTime = System.currentTimeMillis();
		Transaction tx = Transaction.get(in.getClientId(), in.getOpId());
		// logger.info("ActivationHandler Configure has been called");
		// logger.info("[Amit] ActivationHandler++ ");

		ConfigureInputBuilder cib = new ConfigureInputBuilder(in);
		cib.setTimestamp(in.getTimestamp() + ",AQD:" + startTime);
		ConfigureInput input = cib.build();

		tx.setStatusTs(OperationStatus.ACTIVATION_DEQUEUE, startTime);
		HierarchicalCache oCache = new HierarchicalCache(
				(input.getOpRefScope() != null) ? input.getOpRefScope() : RefScope.Unknown,
				tx.getTenantContext().getSc(), true);
		if (input.getOpBody() instanceof Payload) {
			oCache.newOpCache((Payload) input.getOpBody());
			tx.setPayloadCache(oCache.getOpCache());
		}

		try {

			switch (input.getOpType()) {

			case Create:
			case Update:
				for (Contexts context : (oCache.getPayloadContexts() == null) ? Collections.<Contexts>emptyList()
						: oCache.getPayloadContexts()) {
					logger.info("{} session , contextid = {} ,  opid= {}", input.getOpType(),
							context.getContextId().getInt64(), input.getOpId().getValue());
					if (!sessionContextsMap.containsKey(context.getContextId().getInt64())) {
						sessionContextsMap.put(context.getContextId().getInt64(), context);

					} // fi

					for (Dpns dpn : (context.getDpns() == null) ? Collections.<Dpns>emptyList() : context.getDpns()) {

						DpnHolder dpnInfo = null;
						if (TenantManager.vdpnDpnsMap.get(dpn.getDpnId()) != null) {
							for (FpcDpnId dpnId : TenantManager.vdpnDpnsMap.get(dpn.getDpnId())) {
								dpnInfo = tx.getTenantContext().getDpnInfo().get(dpnId.toString());
								if (dpnInfo.activator != null) {
									try {
										dpnInfo.activator.activate(api, input.getClientId(), input.getOpId(),
												input.getOpType(),
												(context.getInstructions() != null) ? context.getInstructions()
														: input.getInstructions(),
												context, oCache, input.getTimestamp());
									} catch (Exception e) {
										logger.error("Action failed ", e);
										processActivationError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL),
												e, "PROTOCOL - operation failed - ERROR - Context Activation - ", tx,
												System.currentTimeMillis() - startTime);
									}
								} else {
									logger.error("No activator found for DPN" + dpn.getDpnId().toString());
								}
							} // for
						} // fi
						else {

							dpnInfo = tx.getTenantContext().getDpnInfo().get(dpn.getDpnId().toString());
							if (dpnInfo.activator != null) {
								try {
									dpnInfo.activator.activate(api, input.getClientId(), input.getOpId(),
											input.getOpType(), (context.getInstructions() != null)
													? context.getInstructions() : input.getInstructions(),
											context, oCache, input.getTimestamp());
									tx.setStatus(OperationStatus.AWAITING_RESPONSES,
											System.currentTimeMillis() - startTime);
								} catch (Exception e) {
									logger.error("Action failed ", e);
									processActivationError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL), e,
											"PROTOCOL - operation failed - ERROR - Context Activation - ", tx,
											System.currentTimeMillis() - startTime);
								}
							} else {
								logger.error("No activator found for DPN" + dpn.getDpnId().toString());
							}

						}

					}
				} // for
				break;

			case Delete:

				DeleteOrQuery doq = (DeleteOrQuery) input.getOpBody();
				for (Targets target : (doq.getTargets() != null) ? doq.getTargets()
						: Collections.<Targets>emptyList()) {
					FpcDpnId ident = null;
					Entry<FixedType, String> entry = extractTypeAndId(NameResolver.extractString(target.getTarget()));

					if (entry == null)
						logger.error("Unable to extract context ID - " + target.getTarget().toString());
					else
						logger.debug("context id : {} ", entry.getValue());
                
					Long contextId = Long.parseLong(entry.getValue());
					
					Contexts context = sessionContextsMap.remove(contextId);

					logger.debug("delete context : {} ", context);
					if (context != null) {
						// for (Context context : cList) {
						for (Dpns dpn : (context.getDpns() == null) ? Collections.<Dpns>emptyList()
								: context.getDpns()) {

						DpnHolder dpnInfo = tx.getTenantContext().getDpnInfo().get(dpn.getDpnId().toString());
						if (dpnInfo.activator != null) {
						try {
						dpnInfo.activator.activate(api, input.getClientId(), input.getOpId(), input.getOpType(), (context.getInstructions() != null) ? context.getInstructions() : input.getInstructions(), context, oCache, input.getTimestamp());
						tx.setStatus(OperationStatus.AWAITING_RESPONSES, System.currentTimeMillis() - startTime);
						} catch (Exception e) {
						processActivationError(new ErrorTypeId(ErrorTypeIndex.CONTEXT_ACTIVATION_FAIL), e, "PROTOCOL - operation failed - ERROR - Context Activation - ", tx, System.currentTimeMillis() - startTime);
						}
						} else {
						logger.info("No activator found for DPN" + dpn.getDpnId().toString());
						}
						} // for3
					  }//fir
					  else{
						logger.info("Contexts remaining ");
					for ( Long contextid : sessionContextsMap.keySet()){
						logger.info(" {} ",contextid);						
					}//

					  }		
									
					// }//for2

				} // for1

				break;

			}// switch

			long entries = entrants.incrementAndGet();
			if ((entries % 100) == 0) {
				logger.info("Activation entries = {} ", entries);
			}

		} catch (Exception e) {
			e.printStackTrace();
			logger.error("error while processing activation {}", ctx.channel(), e.getMessage());
		} //

		}
	 });

	}//

	private Err processActivationError(ErrorTypeId id, Exception e, String message, Transaction tx, long duration) {
		String mess = (e != null) ? message + e.getMessage() : message;
		Err rt = new ErrBuilder().setErrorTypeId(id).setErrorInfo(mess).build();
		tx.setResultType(rt);
		tx.fail(duration);
		if (e != null) {
			ErrorLog.logError(e.getStackTrace());
		}
		return rt;
	}

	/**
	 * Extract type and ID of an entity
	 * 
	 * @param restconfPath
	 *            - Instance identifier of the entity
	 * @return - Map Entry with Type as key and Id as value
	 */
	public Map.Entry<FixedType, String> extractTypeAndId(String restconfPath) {
		for (Map.Entry<FixedType, Map.Entry<Pattern, Integer>> p : NameResolver.entityPatterns.entrySet()) {
			Matcher m = p.getValue().getKey().matcher(restconfPath);
			if (m.matches()) {
				return new AbstractMap.SimpleEntry<FixedType, String>(p.getKey(), m.group(1));
			}
		}
		return null;
	}

}//

