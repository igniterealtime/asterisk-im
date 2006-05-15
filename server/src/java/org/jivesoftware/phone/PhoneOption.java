/**
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */
package org.jivesoftware.phone;

public class PhoneOption {

	public static int TEXTBOX = 1;
	public static int FLAG = 2;
	public static int TEXT = 3;
	
	String description;
	String propertyName;
	String paramName;
	int type = TEXT;
	
	public PhoneOption(String description, String propertyName, String ifName) {
		this.description = description;
		this.propertyName = propertyName;
		this.paramName = ifName;
	}
	
	public PhoneOption(String description, String propertyName, String ifName, int type) {
		this.description = description;
		this.propertyName = propertyName;
		this.paramName = ifName;
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
	public String getDescription() {
		return description;
	}
	public String getParamName() {
		return paramName;
	}
	public String getPropertyName() {
		return propertyName;
	}
	public String check(String value) {
		return null;
	}
	public boolean isPassword() {
		return false;
	}
	public boolean isRequired() {
		return false;
	}
	
}
