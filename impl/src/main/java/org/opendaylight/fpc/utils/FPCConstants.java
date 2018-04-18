/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

public class FPCConstants {
	
	public static final int SERVER_PORT = 8070;
	
	private static final String EVENT_STR="event:";
	private static final String DATA_STR="data:";
	private static final String CHAR_NEW_LINE="\n";
	private static final String CHAR_CR_NEW_LINE="\r\n";
	
	public static final String CLIENT_URL = "client-uri";
	public static final String CLIENT_ID = "client-id";
	
	public static final String URI_RESPONSE  = "/response";	
	public static final String URI_NOTIFICATION  = "/notification";
	
	public static final String HTTP_HEADER_VAL_TXT_EVT_STREAM = "text/event-stream";
		
	public static final String Create = "create";
	public static final String Update = "update";
	public static final String Delete = "delete";
	
	public static final String RPC_CONFIGURE = "configure";
	public static final String RPC_REGISTER_CLIENT = "register_client";
	public static final String RPC_UNREGISTER_CLIENT = "deregister_client";
	
	
	public static enum OpType {
        Create(0, "create"),
        
        Update(1, "update"),
        
        Query(2, "query"),
        
        Delete(3, "delete");
        
        private final int value;
		private final String strValue;

        private OpType(int value,String strValue) {
            this.value = value;
            this.strValue = strValue;
            
        }
        
        public int getValue() {
            return this.value;
        }
		public String getStrValue() {
			return strValue;
		}       
        
	};
	
	
	
}
