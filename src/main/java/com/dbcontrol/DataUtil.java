package com.dbcontrol;

import com.dbcontrol.results.DBMetaData;
import com.dbcontrol.results.DBRow;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Internal utility methods for the data package.
 *
 * @author Derek Mulvihill - Aug 19, 2013
 */
class DataUtil {
    static List<DBRow> getRowsFromResultSet(ResultSet rs) throws SQLException {
        DBMetaData dbm = new DBMetaData(rs.getMetaData());
        List<DBRow> retval = new ArrayList<>();
        while (rs.next()) {
            retval.add(new DBRow(dbm, rs));
        }
        return retval;
    }

    /**
     * Certain sql types aren't the best to work with (Eg. date/times), so this translates from alternative object types (Eg. Joda date/time classes) to the sql types.
     */
    static Object getDBObject(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof LocalDateTime) {
            LocalDateTime dt = (LocalDateTime) obj;
            obj = new Timestamp(dt.toDate().getTime());
        } else if (obj instanceof LocalDate) {
            LocalDate ld = (LocalDate) obj;
            obj = new Date(ld.toDate().getTime());
        } else if (obj instanceof LocalTime) {
            LocalTime lt = (LocalTime) obj;
            obj = new Time(new LocalDate(1970, 1, 1).toLocalDateTime(lt).toDate().getTime());
        }
        return obj;
    }
}
