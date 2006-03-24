/**
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2005 Jive Software. All rights reserved.
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */
package org.jivesoftware.phone.database;

import org.jivesoftware.database.DbConnectionManager;
import org.jivesoftware.util.LocaleUtils;
import org.jivesoftware.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;

/**
 *
 */
public class DatabaseUtil {

    private static final String CHECK_VERSION =
            "SELECT version FROM jiveVersion WHERE name=?";

    private static final String CHECK_VERSION_OLD =
            "SELECT minorVersion FROM jiveVersion";

    private static final int DATABASE_VERSION = 1;


    /**
     * Checks to see if the database needs to be upgraded. This method should be
     * called once every time the application starts up.
     *
     * @throws java.sql.SQLException if an error occured.
     */
    public static boolean upgradeDatabase(Connection con) throws Exception {

        if (!tablesExist(con)) {

            DbConnectionManager.DatabaseType databaseType = DbConnectionManager.getDatabaseType();

            if (databaseType == DbConnectionManager.DatabaseType.unknown) {
                Log.info(LocaleUtils.getLocalizedString("upgrade.database.unknown_db"));
                System.out.println(LocaleUtils.getLocalizedString("upgrade.database.unknown_db"));
                return false;
            }
            else if (databaseType == DbConnectionManager.DatabaseType.interbase) {
                Log.info(LocaleUtils.getLocalizedString("upgrade.database.interbase_db"));
                System.out.println(LocaleUtils.getLocalizedString("upgrade.database.interbase_db"));
                return false;
            }

            // Resource will be like "/database/upgrade/6/wildfire_hsqldb.sql"
            String resourceName = "/database/asterisk-im_" + databaseType + ".sql";

            InputStream resource = DbConnectionManager.class.getResourceAsStream(resourceName);

            applyScript(resource, con);
            updateVersionNumber(con);
            return true;
        }

        int version = 0;
        PreparedStatement pstmt = null;
        try {
            pstmt = con.prepareStatement(CHECK_VERSION);
            pstmt.setString(1, "asterisk-im");
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            version = rs.getInt(1);
            rs.close();
        }
        catch (SQLException sqle) {
            // Releases of Wildfire before 2.6.0 stored a major and minor version
            // number so the normal check for version can fail. Check for the
            // version using the old format in that case.
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
                pstmt = con.prepareStatement(CHECK_VERSION_OLD);
                ResultSet rs = pstmt.executeQuery();
                rs.next();
                version = rs.getInt(1);
                rs.close();
            }
            catch (SQLException sqle2) {
                // Must be database version 0.
            }
        }
        finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            }
            catch (Exception e) {
                Log.error(e);
            }
        }
        if (version == DATABASE_VERSION) {
            return false;
        }
        // The database is an old version that needs to be upgraded.
        Log.info(LocaleUtils.getLocalizedString("upgrade.database.old_schema"));
        System.out.println(LocaleUtils.getLocalizedString("upgrade.database.old_schema"));
        DbConnectionManager.DatabaseType databaseType = DbConnectionManager.getDatabaseType();

        if (databaseType == DbConnectionManager.DatabaseType.unknown) {
            Log.info(LocaleUtils.getLocalizedString("upgrade.database.unknown_db"));
            System.out.println(LocaleUtils.getLocalizedString("upgrade.database.unknown_db"));
            return false;
        }
        else if (databaseType == DbConnectionManager.DatabaseType.interbase) {
            Log.info(LocaleUtils.getLocalizedString("upgrade.database.interbase_db"));
            System.out.println(LocaleUtils.getLocalizedString("upgrade.database.interbase_db"));
            return false;
        }

        // Run all upgrade scripts until we're up to the latest schema.
        for (int i = version + 1; i <= DATABASE_VERSION; i++) {
            Statement stmt;
            InputStream resource = null;
            try {
                // Resource will be like "/database/upgrade/6/wildfire_hsqldb.sql"
                String resourceName = "/database/upgrade/" + i + "/asterisk-im_" +
                        databaseType + ".sql";
                resource = DbConnectionManager.class.getResourceAsStream(resourceName);
                if (resource == null) {
                    // If the resource is null, the specific upgrade number is not available.
                    continue;
                }
                applyScript(resource, con);
                // If the version is greater than 6, automatically update the version information.
                // Previous to version 6, the upgrade scripts set the version themselves.
                if (version > 6) {
                    stmt = con.createStatement();
                    stmt.execute("UPDATE jiveVersion SET version=" + i + " WHERE name='wildfire'");
                    stmt.close();
                }
            }
            finally {
                if (resource != null) {
                    try {
                        resource.close();
                    }
                    catch (Exception e) {
                        // Ignore.
                    }
                }
            }
        }
        updateVersionNumber(con);
        Log.info(LocaleUtils.getLocalizedString("upgrade.database.success"));
        System.out.println(LocaleUtils.getLocalizedString("upgrade.database.success"));
        return true;
    }

    private static void applyScript(InputStream resource, Connection con) throws IOException, SQLException {
        BufferedReader in;
        Statement stmt;
        in = new BufferedReader(new InputStreamReader(resource));
        boolean done = false;
        while (!done) {
            StringBuilder command = new StringBuilder();
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    done = true;
                    break;
                }
                // Ignore comments and blank lines.
                if (DbConnectionManager.isSQLCommandPart(line)) {
                    command.append(line);
                }
                if (line.endsWith(";")) {
                    break;
                }
            }
            // Send command to database.
            if (!done) {
                stmt = con.createStatement();
                stmt.execute(command.toString());
                stmt.close();
            }
        }
    }

    public static boolean tablesExist(Connection con) {

        PreparedStatement psmt = null;
        ResultSet rs = null;

        boolean exists = true;

        try {
            psmt = con.prepareStatement("SELECT 1 FROM phoneUser");
            rs = psmt.executeQuery();
            rs.close();
            psmt.close();

            psmt = con.prepareStatement("SELECT 1 FROM phoneDevice");
            rs = psmt.executeQuery();
        }
        catch (SQLException e) {
            exists = false;
        }
        finally {
            DbConnectionManager.closeResultSet(rs);
            DbConnectionManager.closePreparedStatement(psmt);
        }

        return exists;
    }

    private static void updateVersionNumber(Connection con) throws SQLException {

        String sql = "DELETE FROM jiveVersion where name like 'asterisk-im'";

        PreparedStatement psmt = null;

        try {
            psmt = con.prepareStatement(sql);
            psmt.executeUpdate();
            psmt.close();

            sql = "INSERT INTO jiveVersion (name, version) VALUES ('asterisk-im', ?)";
            psmt = con.prepareStatement(sql);
            psmt.setInt(1, DATABASE_VERSION);
            psmt.executeUpdate();
        }
        finally {
            DbConnectionManager.closePreparedStatement(psmt);
        }

    }

}

