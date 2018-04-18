/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.netty;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.cache.transaction.Transaction.OperationStatus;
import org.opendaylight.fpc.activation.workers.ActivationThreadPool;
import org.opendaylight.fpc.activation.workers.ConfigureWorker;
import org.opendaylight.fpc.activation.workers.MonitorWorker;
import org.opendaylight.fpc.impl.FpcagentServiceBase;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.ErrorTypeIndex;
import org.opendaylight.fpc.utils.NameResolver;
import org.opendaylight.fpc.utils.NameResolver.FixedType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureBundlesOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureDpnOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureInputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ConfigureOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ErrorTypeId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpInput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Payload;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.Result;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpHeader.OpType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.OpBody;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.CreateOrUpdate;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.op.input.op_body.DeleteOrQuery;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.payload.Contexts;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.ResultType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.CommonSuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.DeleteSuccessBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.Err;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.result.body.result.type.ErrBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.targets.value.Targets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.fpc.config.rev160927.FpcConfig;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;

public class IETFRPCServiceImpl extends FpcagentServiceBase {
	private static final Logger logger = LoggerFactory.getLogger(IETFRPCServiceImpl.class);
	// private static Map<Long, Contexts> sessionContextMap = new
	// ConcurrentHashMap<>();

	public static ConcurrentHashMap<String, ConfigureWorker> sessionMap = new ConcurrentHashMap<String, ConfigureWorker>();

	public IETFRPCServiceImpl(DataBroker db, ActivationThreadPool activationService, MonitorWorker monitorService,
			NotificationPublishService notificationService, FpcConfig conf) {
		super(db, activationService, monitorService, notificationService, conf);
	}

	@Override
	public Future<RpcResult<ConfigureBundlesOutput>> configureBundles(ConfigureBundlesInput input) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Future<RpcResult<ConfigureOutput>> configure(ConfigureInput input) {
		logger.debug("configure ++ ");
		long startTime = System.currentTimeMillis();
	
		ConfigureWorker worker = null;
		ResultType rt = null;
		Result res = null;
		Transaction tx = null;
		try {
			tx = Transaction.newTransaction((OpInput) input, startTime);
			rt = immediateChecks((OpInput) input, tx);
			res = (rt != null) ? ((rt instanceof Err) ? Result.Err : Result.OkNotifyFollows) : Result.OkNotifyFollows;

			if ((tx.getStatus() != OperationStatus.COMPLETED) && (tx.getStatus() != OperationStatus.FAILED)) {
				switch (input.getOpType()) {
				case Create:
				case Update:
					OpBody opBody = ((OpInput) input).getOpBody();
					for (Contexts context : ((CreateOrUpdate) opBody).getContexts()) {
						ConfigureWorker entry = sessionMap.get(NameResolver.extractString(context.getContextId()));
						if (entry != null) {
							if (input.getOpType().equals(OpType.Update)) {
								
								ConfigureInputBuilder cib = new ConfigureInputBuilder(input);
								cib.setTimestamp(input.getTimestamp() + ",AQE:" + startTime);
								ConfigureInput in = cib.build();

								entry.getQueue().put(new AbstractMap.SimpleEntry<Transaction, Object>(tx, in));
								tx.setStatusTs(OperationStatus.UPDATE, System.currentTimeMillis());
								tx.setStatusTs(OperationStatus.ACTIVATION_ENQUEUE, startTime);
							} else if (input.getOpType().equals(OpType.Create)) {
								ErrorLog.logError(
										"Create session received for a session that was already created. Session id - "
												+ context.getContextId().toString());
								tx.setStatusTs(OperationStatus.ERRORED_CREATE, startTime);
							}
						} else {
							if (input.getOpType().equals(OpType.Create)) {
								worker = activationService.getWorker();
								
								ConfigureInputBuilder cib = new ConfigureInputBuilder(input);
								cib.setTimestamp(input.getTimestamp() + ",AQE:" + startTime);
								ConfigureInput in = cib.build();

								worker.getQueue().put(new AbstractMap.SimpleEntry<Transaction, Object>(tx, in));

								sessionMap.put(NameResolver.extractString(context.getContextId()), worker);
								
							} else if (input.getOpType().equals(OpType.Update)) {
								for (int i = 0; i < 1500; i++) {
									Thread.sleep(1);
									entry = sessionMap.get(NameResolver.extractString(context.getContextId()));
									if (entry != null) {
										if (input.getOpType().equals(OpType.Update)) {
											
											ConfigureInputBuilder cib = new ConfigureInputBuilder(input);
											cib.setTimestamp(
													input.getTimestamp() + ",AQE:" + System.currentTimeMillis());
											ConfigureInput in = cib.build();
											entry.getQueue()
													.put(new AbstractMap.SimpleEntry<Transaction, Object>(tx, in));
											
											break;
										}
									}
								}
								if (entry == null) {
									tx.setStatusTs(OperationStatus.ERRORED_UPDATE, System.currentTimeMillis());
									ErrorLog.logError(
											"Update received for a session which hasn't been created yet. Session Id - "
													+ context.getContextId().toString());
								}
							}
						}
					}

					rt = new CommonSuccessBuilder(((Payload) input.getOpBody())).build();

					break;
				case Delete:
					// logger.debug("delete session");
					try {
						for (Targets target : ((DeleteOrQuery) ((OpInput) input).getOpBody()).getTargets()) {
							Entry<FixedType, String> entry = extractTypeAndId(
									NameResolver.extractString(target.getTarget()));
							if (entry.getKey().equals(FixedType.CONTEXT))
								if (sessionMap.get(entry.getValue()) != null) {
									sessionMap.get(entry.getValue()).getQueue()
											.put(new AbstractMap.SimpleEntry<Transaction, Object>(tx, input));
									
									sessionMap.remove(entry.getValue());
								}
						}
						rt = new DeleteSuccessBuilder((DeleteOrQuery) input.getOpBody()).build();
					} catch (InterruptedException e) {
						rt = activationServiceInterrupted(e, tx, System.currentTimeMillis() - startTime);
					}
					break;
				default:
					logger.warn("case not supported : "+input.getOpType());
					break;

				}// switch

			} // fi
		} catch (Exception e) {
			// e.printStackTrace();
			res = Result.Err;
			rt = new ErrBuilder().setErrorTypeId(new ErrorTypeId(ErrorTypeIndex.MESSAGE_WITH_NO_BODY))
					.setErrorInfo("SYSTEM - operation failed - No Body was sent with this message.").build();
			logger.error("Error while processing request {} ", e.getMessage());
		}

		if (res == null)
			res = Result.OkNotifyFollows;

		ConfigureOutput configureOutput = new ConfigureOutputBuilder().setOpId(input.getOpId()).setResult(res)
				.setResultType(rt).build();
		return Futures.immediateFuture(RpcResultBuilder.<ConfigureOutput>success(configureOutput).build());
	}

	@Override
	public Future<RpcResult<ConfigureDpnOutput>> configureDpn(ConfigureDpnInput input) {
		// TODO Auto-generated method stub
		return null;
	}//

	/*
	 * Extract the type and Id of the entity
	 * 
	 * @param restconfPath - Instance id of the entity
	 * 
	 * @return - Map Entry of the Type and Id of the entity
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

