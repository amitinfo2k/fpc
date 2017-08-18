/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.utils.buffer;

import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.opendaylight.fpc.utils.StringConstants;

public class Parser {
	private ByteBuffer byteBuffer;
	private int byteBufferPosition;

	private int indexOf(char searchChar){
		return indexOf(searchChar, 0);
	}

	private int indexOf(char searchChar, int position){
		int index = -1;
		for(int i = position; i < this.byteBuffer.limit(); i++){
			if(this.byteBuffer.get(i) == searchChar)
				return i;
		}
		return index;
	}

	private void seekToValue(){
		//Bypass any white spaces after colon
		while(Character.isWhitespace(this.byteBuffer.get(this.byteBufferPosition)) ||
				this.byteBuffer.get(this.byteBufferPosition) == ':'){
			this.byteBufferPosition++;
		}
	}

	private void seekToAttribute(){
		while(this.byteBufferPosition < this.byteBuffer.limit() &&
				(Character.isWhitespace(this.byteBuffer.get(this.byteBufferPosition)) ||
						this.byteBuffer.get(this.byteBufferPosition) == ',' ||
						this.byteBuffer.get(this.byteBufferPosition) == '}')){
			this.byteBufferPosition++;
		}
		if(this.byteBufferPosition >= this.byteBuffer.limit()){
			this.byteBufferPosition = -1;
		}
	}

	private String getAttribute(){
		this.seekToAttribute();
		if(this.byteBufferPosition < 0)
			return null;
		//Read Attribute
		String attribute = null;
		int start = this.indexOf('"', this.byteBufferPosition);
		int end;
		if(start > -1){
			start++; //increment to next position to after quote
			end = this.indexOf('"',start);
			int lengthOfAttributeString = end - start;
			byte[] byteArray = new byte[lengthOfAttributeString];
			this.byteBuffer.position(start);
			this.byteBuffer.get(byteArray, 0, lengthOfAttributeString);
			attribute = new String(byteArray);
			this.byteBufferPosition = end+1; //move string buffer position to after the closing quote
		}
		return attribute;
	}

	private AbstractMap.SimpleEntry<Object, List<Integer>> getValue(){
		this.seekToValue();
		AbstractMap.SimpleEntry<Object, List<Integer>> valueEntry = null;
		//Read value. Possible value types - String, number, boolean, null, array, object
		Object value;
		int start = this.byteBufferPosition, end = -1;
		switch(this.byteBuffer.get(start)){
			//String
			case '"':
				start++; //Increment to first character of string
				end = this.indexOf('"',start);

				//move past any escaped quotes within the string
				while(this.byteBuffer.get(end-1) == '\\'){
					end = this.indexOf('"',end+1);
				}

				//extract value
				int lengthOfValueString = end - start;
				byte[] byteArray = new byte[lengthOfValueString];
				this.byteBuffer.position(start);
				this.byteBuffer.get(byteArray, 0, lengthOfValueString);
				value = new String(byteArray);

				//Record start and end positions in the buffer
				List<Integer> position = new ArrayList<Integer>();
				position.add(start);
				position.add(end);
				valueEntry = new AbstractMap.SimpleEntry<Object, List<Integer>>(value, position);

				this.byteBufferPosition = end+1; //move string buffer position to after the closing quote
				break;

			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				//Number
				break;
		}
		return valueEntry;
	}

	public void parse(ByteBuffer byteBuffer, Integer byteBufferStartPosition, Map<String, DTOInstance> attributes) {
		this.byteBuffer = byteBuffer;
		this.byteBufferPosition = byteBufferStartPosition;
		this.byteBuffer.position(byteBufferStartPosition);

		while(this.byteBufferPosition >=0 && this.byteBufferPosition<=this.byteBuffer.limit()){
			String attribute = this.getAttribute();
			if(attribute != null){
				AbstractMap.SimpleEntry<Object, List<Integer>> valueEntry = this.getValue();

				//TODO - Add schema check for Value here

				attributes.put(attribute, new DTOInstance(valueEntry.getValue().get(0),valueEntry.getValue().get(1)));

			}
		}
	}

}
