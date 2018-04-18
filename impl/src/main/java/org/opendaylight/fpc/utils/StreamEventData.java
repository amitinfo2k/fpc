/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils;

public class StreamEventData {
	private String event;
	private String data;
	private String clientURL;
	
	public StreamEventData(String event,String data){
		this.event=event;
		this.data=data;		
	}
	
	public StreamEventData(String event,String data,String clientURL){
		this.event=event;
		this.data=data;
		this.clientURL=clientURL;
	}
	
	
	public String getEvent() {
		return event;
	}
	public void setEvent(String event) {
		this.event = event;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getClientURL() {
		return clientURL;
	}
	public void setClientURL(String clientURL) {
		this.clientURL = clientURL;
	}	

}

