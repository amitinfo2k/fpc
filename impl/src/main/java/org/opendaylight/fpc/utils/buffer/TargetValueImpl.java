/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.buffer;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class TargetValueImpl implements TargetValue {

	private ByteBuffer byteBuffer;
	private Map<String, DTOInstance> attributes;
	private Map<String, DTOProto> schemaChildren;

	//private FpcIdentity target;
	private String target;

	public TargetValueImpl() {
		this.target = null;
	}

	@Override
	public String toString() {
		String groupingName = this.name;
		StringBuilder stringBuilder = new StringBuilder(groupingName);
		if(this.attributes != null && this.attributes.get("target")!=null)
				stringBuilder.append(" = [ Target = \""+this.getTarget()+"\" ]");
		return stringBuilder.toString();
	}

	@Override
	public Object fromBuffer(ByteBuffer buf, DTOInstance dtoInstance) {
		byte[] byteArray = new byte[dtoInstance.getStopIndex() - dtoInstance.getStartIndex()];
		buf.position(dtoInstance.getStartIndex());
		buf.get(byteArray,0,dtoInstance.getStopIndex() - dtoInstance.getStartIndex());
		return byteArray;
	}

	@Override
	public void toBuffer(ByteBuffer buf, DTOInstance dtoInstance, Object Value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void appendToBuffer(ByteBuffer buf) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getTarget(){
		if(this.target == null && attributes.get("target") != null){
			this.target = new String((byte[]) fromBuffer(this.byteBuffer,attributes.get("target")));
		}
		return this.target;
	}

	public TargetValue build(ByteBuffer byteBuffer){
		attributes = new HashMap<String, DTOInstance>();
		this.byteBuffer = byteBuffer;
		Parser parser = new Parser();
		parser.parse(byteBuffer,0,attributes);
		return (TargetValue) this;
	}

}
