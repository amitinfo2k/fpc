/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.buffer;

public class DTOInstance {
	private int startIndex;
	private int stopIndex;

	public DTOInstance(Integer startIndex,Integer stopIndex){
		this.startIndex = startIndex;
		this.stopIndex = stopIndex;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getStopIndex() {
		return stopIndex;
	}

	public void setStopIndex(int stopIndex) {
		this.stopIndex = stopIndex;
	}
}
