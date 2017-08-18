/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.buffer;

import java.nio.ByteBuffer;
import java.util.Map;

public interface DTOProto {
	boolean isFixedLength = false;
	Object fromBuffer(ByteBuffer buf, DTOInstance dtoInstance);
	void toBuffer(ByteBuffer buf, DTOInstance dtoInstance, Object Value);
	void appendToBuffer(ByteBuffer buf);
	String toString();
}
