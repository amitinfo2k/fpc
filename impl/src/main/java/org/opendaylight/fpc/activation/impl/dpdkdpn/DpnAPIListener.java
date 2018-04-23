/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;

import java.math.BigInteger;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.opendaylight.fpc.activation.cache.transaction.Transaction;
import org.opendaylight.fpc.activation.cache.transaction.Transaction.OperationStatus;
import org.opendaylight.fpc.dpn.DPNStatusIndication;
import org.opendaylight.fpc.dpn.DPNTypeIndication;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotification;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.notify.value.DownlinkDataNotificationBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.OpIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DPN Listener Interface for DPN initiated messages.
 */
public class DpnAPIListener {
    protected static final Logger LOG = LoggerFactory.getLogger(DpnAPIListener.class);
    private static byte DPN_HELLO = 0b0000_0001;
    private static byte DPN_BYE = 0b0000_0010;
    private static byte DOWNLINK_DATA_NOTIFICATION = 0b0000_0101;
    private static byte DPN_STATUS_INDICATION = 0b0000_1100;
    private static byte DPN_OVERLOAD_INDICATION = 0b0000_0101;
    private static byte DPN_REPLY = 0b0000_0100;
    private static String DOWNLINK_DATA_NOTIFICATION_STRING = "Downlink-Data-Notification";
    static private Map<String, FpcDpnId> uplinkDpnMap = new ConcurrentHashMap<String, FpcDpnId>();
    static private Map<String, Short> topicToNodeMap = new ConcurrentHashMap<String, Short>();

    /**
     * Sets the mapping of a node id / network id key to DPN Identities.
     * @param key - Combination of node id and network id
     * @param dpnId - DPN Identifier
     */
    static public void setUlDpnMapping(String key, FpcDpnId dpnId) {
	 LOG.info("Adding DPN Mapping {} => {}", key, dpnId);
        uplinkDpnMap.put(key, dpnId);
    }

    /**
     * Removes a mapping of a node id / network id if it exists.
     * @param key - Combination of node id and network id
     */
    static public void removeUlDpnMapping(String key) {
        LOG.info("Removing DPN Mapping {}", key);
        uplinkDpnMap.remove(key);
    }

    /** Sets the mapping of a node id / network id to ZMQ Topic
     * @param key - Concatenation of node id + / + network id
     * @param topic - ZMQ Topic
     */
    static public void setTopicToNodeMapping(String key, Short topic){
       topicToNodeMap.put(key, topic);
    }

    /**
     * Removes a mapping of node id / network id to ZMQ Topic if it exists
     * @param key - Concatenation of node id + / + network id
     */
    static public void removeTopicToNodeMapping(String key){
    	topicToNodeMap.remove(key);
    }

    /**
     * Gets the mapping for node id / network id to ZMQ Topic
     * @param Key - Concatenation of node id + / + network id
     * @return - ZMQ Topic
     */
    static public Short getTopicFromNode(String Key){
    	return topicToNodeMap.get(Key);
    }

    /**
     * Look up the ZMQ Topic of a DPN using its DPN Id
     * @param dpnId - DPN Id of the DPN
     * @return - ZMQ Topic
     */
    static public Short getTopicFromDpnId(FpcDpnId dpnId){
		for(Entry<String, FpcDpnId> entry : uplinkDpnMap.entrySet()){
			LOG.debug("entry: "+entry.getValue().getString());
			LOG.debug("dpnId: "+dpnId.getString());
    		if(entry.getValue().getString().equals(dpnId.getString())){
			return topicToNodeMap.get(entry.getKey());
    		}
    	}
		return null;
    }

    /**
     * Gets the mapping for a ZeroMQ Topic
     * @param key - Dpn nodeid +"/"+ networkId
     * @return FpcDpnId mapped to the ZeroMQ Topic or null if a mapping is not present
     */
    static public FpcDpnId getMapping(String key) {
	return uplinkDpnMap.get(key);
    }

    /**
     * Decodes a DPN message.
     * @param buf - message buffer
     * @return - A pair with the DPN Id and decoded Object
     */
    public Map.Entry<FpcDpnId, Object> decode(byte[] buf) {
	if(buf[1] == DPN_REPLY){
        	processReply(buf);
        	return null;
        }
        else if (buf[1] == DOWNLINK_DATA_NOTIFICATION) {
		short nodeIdLen = buf[18];
		short networkIdLen = buf[19+nodeIdLen];
		String key = new String(Arrays.copyOfRange(buf, 19, 19+nodeIdLen)) +"/" + new String(Arrays.copyOfRange(buf, 20+nodeIdLen, 20+nodeIdLen+networkIdLen));
		return uplinkDpnMap.get(key) == null? null : new AbstractMap.SimpleEntry<FpcDpnId, Object>(uplinkDpnMap.get(key), processDDN(buf,key));
        } else if(buf[1] == DPN_STATUS_INDICATION) {
            DPNStatusIndication.Status status = null;
	    String  type = "NO_DPN_TYPE";

            int dpnTypeValue = buf[8];//dpn-type position in content
            short nodeIdLen = buf[9];
            short networkIdLen = buf[10+nodeIdLen];
            //String key = new String(Arrays.copyOfRange(buf, 9, 9+nodeIdLen)) +"/" + new String(Arrays.copyOfRange(buf, 10+nodeIdLen, 10+nodeIdLen+networkIdLen));
            StringBuilder sb= new StringBuilder();
		sb.append(new String(Arrays.copyOfRange(buf, 10, 10+nodeIdLen)));
		sb.append("/");
		sb.append(new String(Arrays.copyOfRange(buf, 11+nodeIdLen, 11+nodeIdLen+networkIdLen)));

		for(DPNTypeIndication test_type: DPNTypeIndication.values()){
			if(test_type.getTypeValue() == dpnTypeValue){
				type = test_type.toString();
				break;
			}
		}

		//Add dpn-type date in here similar to networkid and nodeid
		String key= sb.toString();
	    LOG.info("Hello Key: "+key);
            if (buf[3] ==  DPN_OVERLOAD_INDICATION) {
                status = DPNStatusIndication.Status.OVERLOAD_INDICATION;
            } else if (buf[3] ==  DPN_HELLO) {
                status = DPNStatusIndication.Status.HELLO;
                setTopicToNodeMapping(key,(short)buf[2]);
            } else if (buf[3] ==  DPN_BYE) {
                status = DPNStatusIndication.Status.BYE;
                removeTopicToNodeMapping(key);
            }
            return new AbstractMap.SimpleEntry<FpcDpnId, Object>(uplinkDpnMap.get(key), new DPNStatusIndication(status, key, type));
        }
        return null;
    }

    /**
     * Decodes a DownlinkDataNotification
     * @param buf - message buffer
     * @param key - Concatenation of node id + / + network id
     * @return DownlinkDataNotification or null if it could not be successfully decoded
     */
    public DownlinkDataNotification processDDN(byte[] buf,String key) {
        DownlinkDataNotificationBuilder ddnB = new DownlinkDataNotificationBuilder();

        return ddnB.setSessionId(checkSessionId(toBigInt(buf,2)))
        		.setMessageType(DOWNLINK_DATA_NOTIFICATION_STRING)
        		.setClientId(new ClientIdentifier(fromIntToLong(buf, 10)))
        		.setOpId(new OpIdentifier(BigInteger.valueOf(fromIntToLong(buf, 14))))
        		.setDpnId(uplinkDpnMap.get(key))
        		.build();
    }

    /**
     * Ensures the session id is an unsigned 64 bit integer
     * @param sessionId - session id received from the DPN
     * @return unsigned session id
     */
    private BigInteger checkSessionId(BigInteger sessionId){
    	if(sessionId.compareTo(BigInteger.ZERO) < 0){
    		sessionId = sessionId.add(BigInteger.ONE.shiftLeft(64));
    	}
    	return sessionId;
    }

    /**
     * Decodes an acknowledgement from the DPN and completes that transaction
     * @param buf - message buffer
     */
    public void processReply(byte[] buf){
    	ClientIdentifier ClientId = new ClientIdentifier(fromIntToLong(buf, 3));
		OpIdentifier OpId = new OpIdentifier(BigInteger.valueOf(fromIntToLong(buf, 7)));
		LOG.debug(ClientId+"/"+OpId);
		Transaction t = Transaction.get(ClientId+"/"+OpId.toString());
		if(t != null){
			t.setStatus(OperationStatus.DPN_RESPONSE_PROCESSED, System.currentTimeMillis());
			t.setCauseValue(buf[2]);
			t.sendNotification();
		} else {
			ErrorLog.logError("Transaction not found: "+ClientId+"/"+OpId);
		}
    }

    /**
     * Converts a byte array to BigInteger
     * @param source - byte array
     * @param offset - offset in the array where the 8 bytes begins
     * @return BigInteger representing a Uint64
     */
    public BigInteger toBigInt(byte[] source, int offset) {
        return new BigInteger(Arrays.copyOfRange(source, offset, offset+8));
    }

    /**
     * Decodes a Long value
     * @param source - byte array
     * @param offset - offset in the array where the 8 bytes begins
     * @return BigInteger representing a Uint64
     */
    public long toLong(byte[] source, int offset) {
        return new BigInteger(Arrays.copyOfRange(source, offset, offset+8)).longValue();
    }

    /**
     * Converts an integer to a long (used for larger unsigned integers)
     * @param source - message buffer (byte array)
     * @param offset - offset in the array where the 4 bytes begins
     * @return Long value of the unsigned integer
     */
    public long fromIntToLong(byte[] source, int offset){
    	long value = 0;
    	for (int i = offset; i < offset + 4; i++)
    	{
    		 value = (value << 8) + (source[i] & 0xff);
    	}
    	return value;
    }

    /**
     * Decodes a 32 bit value
     * @param source - byte array
     * @param offset - offset in the array where the 8 bytes begins
     * @return integer
     */
    public int toInt(byte[] source, int offset) {
        return new BigInteger(Arrays.copyOfRange(source, offset, offset+4)).intValue();
    }

    /**
     * Decodes a Short (byte) value
     * @param source - byte array
     * @param offset - offset in the array where the 8 bytes begins
     * @return Short
     */
    public short toShort(byte[] source, int offset) {
        return (short) source[offset];
    }
}

