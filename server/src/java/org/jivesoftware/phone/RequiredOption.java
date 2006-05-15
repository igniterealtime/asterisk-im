/**
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */
package org.jivesoftware.phone;

public class RequiredOption extends PhoneOption {

	public RequiredOption(String description, String propertyName, String ifName) {
		super(description, propertyName, ifName);
	}
	
	public String check(String value) {
		if (value==null || "".equals(value)) {
			return description+" is required";
		}
		return null;
	}
	
	public boolean isRequired() {
		return true;
	}

}
