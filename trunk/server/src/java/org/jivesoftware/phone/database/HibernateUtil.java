/**
 * $RCSfile: HibernateUtil.java,v $
 * $Revision: 1.20 $
 * $Date: 2005/07/01 01:09:40 $
 *
 * Copyright (C) 1999-2004 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.database;

import org.jivesoftware.phone.PhoneDevice;
import org.jivesoftware.phone.PhoneUser;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.dialect.*;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;
import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.database.JiveID;
import org.jivesoftware.util.Log;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Used to acquire a hibernate session and store it as a thread local.
 *
 * @author Andrew Wright
 */
public class HibernateUtil {

    private static final Logger log = Logger.getLogger(HibernateUtil.class.getName());

    private static SessionFactory sessionFactory = null;

    private static Boolean tablesExist = null;

    public static synchronized Session getSession() {

        if(sessionFactory != null) {

            return sessionFactory.openSession();

        } else {
            return null;
        }

    }

    public static void init() {

        if (sessionFactory == null) {
            try {

                Configuration cfg = getFullConfiguration();

                // Create the SessionFactory
                sessionFactory = cfg.buildSessionFactory();

            }
            catch (Exception e) {
                log.log(Level.SEVERE, e.getMessage(), e);
                throw new ExceptionInInitializerError(e);
            }

        }
    }

    public static void close(Session session) {

        if (session != null)  {

            try {
                session.close();
            }
            catch (HibernateException e) {
                log.log(Level.SEVERE, e.getMessage(),  e);
            }


        }

    }

    /**
     * Closes the session factory
     */
    public static void close() {
        if(sessionFactory != null) {
            sessionFactory.close();
            sessionFactory = null;
        }
    }

    @SuppressWarnings({"unchecked"})
    public static List<Exception> removeDB() {
        SchemaExport exporter = new SchemaExport(getFullConfiguration());
        exporter.drop(true, true);

        String sql = "delete from jiveID where idType in (?,?)";
        Connection con = null;
        PreparedStatement psmt = null;

        List<Exception> exceptions = exporter.getExceptions();

        int phoneUserType = PhoneUser.class.getAnnotation(JiveID.class).value();
        int phoneDeviceType = PhoneDevice.class.getAnnotation(JiveID.class).value();

        boolean abortTransaction = false;

        try {
            con = DbConnectionManager.getTransactionConnection();
            psmt = con.prepareStatement(sql);
            psmt.setInt(1, phoneUserType);
            psmt.setInt(2, phoneDeviceType);
            psmt.executeUpdate();

            insertJiveID(con, phoneUserType);
            insertJiveID(con, phoneDeviceType);
        }
        catch (SQLException e) {
            exceptions.add(e);
            abortTransaction = true;
        }
        finally {
            DbConnectionManager.closeConnection(psmt, null);
            DbConnectionManager.closeTransactionConnection(con, abortTransaction);
        }

        return exceptions;
    }


    @SuppressWarnings({"unchecked"})
    public static List<Exception> initDB() {

        if(tablesExist()) {
            return Collections.emptyList();
        }

        log.info("Installing phone plugin database");
        SchemaExport exporter = new SchemaExport(getFullConfiguration());
        exporter.create(true, true);
        List<Exception> exceptions = exporter.getExceptions();

        int phoneUserType = PhoneUser.class.getAnnotation(JiveID.class).value();
        int phoneDeviceType = PhoneDevice.class.getAnnotation(JiveID.class).value();

        Connection con = null;
        PreparedStatement psmt = null;
        boolean abortTransaction = false;

        try {
            con = DbConnectionManager.getTransactionConnection();

            insertJiveID(con, phoneUserType);
            insertJiveID(con, phoneDeviceType);
        }
        catch (SQLException e) {
            exceptions.add(e);
            abortTransaction = true;
        }
        finally {
            DbConnectionManager.closeConnection(psmt, null);
            DbConnectionManager.closeTransactionConnection(con, abortTransaction);
        }


        tablesExist = null; // value should be rechecked
        return exceptions;

    }

    @SuppressWarnings({"unchecked"})
    public static List<Exception> updateDB() {

        if(tablesExist()) {
            return Collections.emptyList();
        }

        log.info("Installing phone plugin database");
        SchemaUpdate updater = new SchemaUpdate(getFullConfiguration());
        updater.execute(true, true);
        return (List<Exception>)updater.getExceptions();

    }

    private static void insertJiveID(Connection con, int type) throws SQLException {

        String sql = "insert into jiveID (id,idType) values (1, ?);";

        PreparedStatement psmt = null;

        try {
            psmt =  con.prepareStatement(sql);
            psmt.setInt(1, type);
            psmt.executeUpdate();
        }
        finally {
            DbConnectionManager.closeConnection(psmt, null);
        }

    }

    public static boolean tablesExist() {

        if(tablesExist != null) {
            return tablesExist;
        }

        Connection con = null;
        ResultSet rs = null;

        boolean foundPhoneUser = false;
        boolean foundPhoneDevice = false;

        try {
            con = DbConnectionManager.getConnection();
            DatabaseMetaData metaData = con.getMetaData();

            // Because some tables seem to be case sensitive when try to select phone%
            // I have changed to just go through all the tables.
            rs = metaData.getTables(null, null, null, new String[] { "TABLE" } );


            while(rs.next()) {

                String tableName = rs.getString(3);

                if("phoneUser".equalsIgnoreCase(tableName)) {
                    foundPhoneUser = true;
                }
                else if ("phoneDevice".equalsIgnoreCase(tableName)) {
                    foundPhoneDevice = true;
                }

            }


        }
        catch (SQLException e) {
            log.log(Level.SEVERE, e.getMessage(), e);
        }
        finally {

            try {
                if(rs != null) { rs.close(); }
            }
            catch (SQLException e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }


            try {
                if(con != null) { con.close(); }
            }
            catch (SQLException e) {
                log.log(Level.SEVERE, e.getMessage(), e);
            }
        }

        tablesExist =  foundPhoneUser && foundPhoneDevice;
        return tablesExist;
    }

    private static Configuration getFullConfiguration() {

        DbConnectionManager.DatabaseType databaseType = DbConnectionManager.getDatabaseType();

        Log.debug("Asterisk-IM: Messenger is using database type : "+databaseType);

        String dialect = getDialect(databaseType).getName();

        Log.debug("Asterisk-IM: Using Hibernate Dialect : "+dialect);

        return getConfiguration()
                        .setProperty("hibernate.order_updates", "true")
                        .setProperty("hibernate.dialect", dialect)
                        .setProperty("hibernate.connection.provider_class",
                                JiveConnectionProvider.class.getName());

    }

    /**
     * Used to return a configuration which contains all of the classes needed for phone
     *
     * @return the configuration
     */
    private static Configuration getConfiguration() {
        return new Configuration()
                .addClass(PhoneDevice.class)
                .addClass(PhoneUser.class);
    }

    /**
     * Used to find a hibernate dialect for the database type
     *
     * @param type database type, from db connection manager
     * @return the correct dialect class
     */
    private static Class getDialect(DbConnectionManager.DatabaseType type) {

        Class dialect;

        if (type.equals(DbConnectionManager.DatabaseType.postgres)) {
            dialect =  PostgreSQLDialect.class;
        }
        else if (type.equals(DbConnectionManager.DatabaseType.mysql)) {
            dialect =  MySQLDialect.class;
        }
        else if (type.equals(DbConnectionManager.DatabaseType.oracle)) {
            dialect =  Oracle9Dialect.class;
        }
        else if (type.equals(DbConnectionManager.DatabaseType.hsqldb)) {
            dialect =  HSQLDialect.class;
        }
        else if (type.equals(DbConnectionManager.DatabaseType.db2)) {
            dialect =  DB2Dialect.class;
        }
        else if (type.equals(DbConnectionManager.DatabaseType.sqlserver)) {
            dialect =  SQLServerDialect.class;
        }
        else {
            dialect =  GenericDialect.class;
        }

        return dialect;
    }

    /**
     * Used to output ddl to a file
     *
     * @param type       the database type
     * @param outputFile the output file
     */
    public static void outputDDL(DbConnectionManager.DatabaseType type, String outputFile) {

        Configuration cfg = getConfiguration().setProperty("hibernate.dialect",
                getDialect(type).getName());

        SchemaExport export = new SchemaExport(cfg);
        export.setOutputFile(outputFile);
        export.setDelimiter(";");
        export.create(true, false);

    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.print("you must specify database type and output file!");
            System.exit(1);
        }

        String database = args[0];
        String file = args[1];

        DbConnectionManager.DatabaseType type = DbConnectionManager.DatabaseType.valueOf(database);

        outputDDL(type, file);
    }

}
