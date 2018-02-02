/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.dpn;

//import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;

// TODO - Add the subtypes for Overload Indication and Control
/**
 * Provides basic status changes,
 */
public enum DPNTypeIndication {
  	sgwu(1), pgwu(2), spgw(3);
  
	private int typeValue;
   
    	private DPNTypeIndication(int typeValue) {
        	this.typeValue = typeValue;
    	}

    	public int getTypeValue() {
        	return typeValue;
    	}
}
