package com.piusvelte.sonet.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;

/**
 * Created by bemmanuel on 5/25/15.
 */
public class DatabaseUtils {

    private DatabaseUtils() {
        // Not instantiable
    }

    public static String addNullCheckCaseWhen(@NonNull String alias, @NonNull String column) {
        return String.format("when %s.%s is not null then %s.%s", alias, column, alias, column);
    }

    public static String addCase(@NonNull String[] aliases, @NonNull String column, int defaultValue, @NonNull String as) {
        StringBuilder caseStatement = new StringBuilder("(case");

        for (String alias : aliases) {
            caseStatement.append(" ")
                    .append(addNullCheckCaseWhen(alias, column));
        }

        caseStatement.append(" else ")
                .append(defaultValue)
                .append(" end) as ")
                .append(as);

        return caseStatement.toString();
    }

    public static boolean growTable(SQLiteDatabase db,
            String tableName,
            String columnName,
            String columnType,
            String columnValue,
            boolean quotedValue) {
        boolean success = false;
        Cursor sqlite_master = db
                .query("sqlite_master", new String[] { "sql" }, "tbl_name=? and type='table'", new String[] { tableName }, null, null, null);

        if (sqlite_master.moveToFirst()) {
            // get the existing table structure
            String sql = sqlite_master.getString(0);
            db.execSQL("drop table if exists " + tableName + "_bkp;");
            db.execSQL("create temp table " + tableName + "_bkp as select * from " + tableName + ";");
            db.execSQL("drop table if exists " + tableName + ";");
            // create new table
            StringBuilder createTable = new StringBuilder();
            createTable.append(sql.substring(0, sql.length() - 1));
            createTable.append(", ");
            createTable.append(columnName);
            createTable.append(" ");
            createTable.append(columnType);
            createTable.append(");");
            db.execSQL(createTable.toString());
            // restore data
            String columnData = sql.substring(sql.indexOf("(") + 1);

            if (columnData.length() > 0) {
                StringBuilder insertStmt = new StringBuilder();
                insertStmt.append("insert into ");
                insertStmt.append(tableName);
                insertStmt.append(" select ");
                // interate over columns
                int previousColumn = 0;
                insertStmt.append(columnData.substring(previousColumn, columnData.indexOf(" ")));
                insertStmt.append(",");

                while ((previousColumn = columnData.indexOf(",", previousColumn)) != -1) {
                    previousColumn += 2;
                    insertStmt.append(columnData
                            .substring(previousColumn, (columnData.indexOf(" ", previousColumn) - previousColumn) + previousColumn));
                    insertStmt.append(",");
                }

                // add new column value
                if (quotedValue) {
                    insertStmt.append("'");
                    insertStmt.append(columnValue);
                    insertStmt.append("'");
                } else {
                    insertStmt.append(columnValue);
                }

                insertStmt.append(" from ");
                insertStmt.append(tableName);
                insertStmt.append("_bkp;");
                db.execSQL(insertStmt.toString());
                success = true;
            }

            db.execSQL("drop table if exists " + tableName + "_bkp;");
        }

        sqlite_master.close();
        return success;
    }
}
