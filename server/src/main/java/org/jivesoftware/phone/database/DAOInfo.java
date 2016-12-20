package org.jivesoftware.phone.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Used to tell what type of an implementation a dao uses
 *
 * @author Andrew Wright
 * @since 1.0
 */
@Target(ElementType.TYPE)
public @interface DAOInfo {

    enum daoType {
        JDBC
    }

    daoType value();

}
