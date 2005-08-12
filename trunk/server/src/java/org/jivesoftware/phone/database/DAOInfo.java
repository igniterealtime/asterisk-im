package org.jivesoftware.phone.database;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

/**
 * @author Andrew Wright
 */
@Target(ElementType.TYPE)
public @interface DAOInfo {

    String value();

}
