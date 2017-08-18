/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.buffer;

import java.nio.ByteBuffer;

public class TestRunner {
	public static void runDTOTest(){
		//Sample JSON string
		String jsonString = "{\"target\":\"/test/target/target1\"}";

		//ByteBuffer created from json string.
		ByteBuffer byteBuffer = ByteBuffer.wrap(jsonString.getBytes());

		TargetValueImpl targetValueImpl = new TargetValueImpl();
		System.out.println("Test JSON - "+jsonString);
		TargetValue targetValue = targetValueImpl.build(byteBuffer);
		System.out.println("TargetValue.toString() --> "+targetValue.toString());
	}
}
