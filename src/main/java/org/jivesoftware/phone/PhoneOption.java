/**
 * Copyright (C) 2006 headissue GmbH; Jens Wilke. All rights reserved.
 *
 * This software is published under the terms of the GNU Public License (GPL),
 * a copy of which is included in this distribution.
 */
package org.jivesoftware.phone;

public class PhoneOption {
	String title;
	String propertyName;
	String paramName;
	Type type = Type.text;
    private String defaultValue = "";
    private String description;

    public PhoneOption(String title, String propertyName, String ifName) {
        this.title = title;
        this.propertyName = propertyName;
        this.paramName = ifName;
    }
	
	public PhoneOption(String title, String propertyName, String ifName, Type type) {
        this(title, propertyName, ifName, type, "", null);
    }

	public PhoneOption(String title, String propertyName, String ifName, Type type,
                       String defaultValue, String description) {
		this.title = title;
		this.propertyName = propertyName;
		this.paramName = ifName;
		this.type = type;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public Type getType() {
		return type;
	}
	public String getTitle() {
		return title;
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

    public String getDefaultValue() {
        return defaultValue;
    }

    public enum Type {
        textbox, flag, text
    }
}
