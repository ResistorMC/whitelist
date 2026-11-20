package me.minecraft.resistor.whitelist.database.sqlite;

import me.minecraft.resistor.whitelist.Whitelist;

import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SQLiteWhitelistTable {
    private Connection conn;
    private Whitelist plugin;

    public SQLiteWhitelistTable(Connection conn, Whitelist plugin) {
        this.conn = conn;
        this.plugin = plugin;
    }

    public void createTable() throws SQLException{
        String sql = "CREATE TABLE IF NOT EXISTS whitelist (" +
                "nick TEXT PRIMARY KEY COLLATE NOCASE," +
                "last_seen INTEGER NOT NULL," +
                "rest INTEGER NOT NULL DEFAULT 0," +
                "rest_until INTEGER NOT NULL DEFAULT 0" +
                ");";

        try (Statement stmt = conn.createStatement()){
            stmt.execute(sql);
        }
    }


    public List<String> getWhitelist() throws SQLException {
        String sql = "SELECT nick FROM whitelist ORDER BY nick;";
        List<String> nicks = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                nicks.add(rs.getString("nick"));
            }
        }
        return nicks;
    }

    public Map<String,Long> getInfoPlayer(String nick) throws SQLException {
        String sql = "SELECT last_seen, rest, rest_until FROM whitelist WHERE nick = ?";
        Map<String,Long> map = new HashMap<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1,nick);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    map.put("last_seen", rs.getLong("last_seen"));
                    map.put("rest", rs.getLong("rest"));
                    map.put("rest_until", rs.getLong("rest_until"));
                    map.put("null", 1L);
                } else {
                    map.put("null",0L);
                }
            }
        }
        return map;
    }

    public Set<String> checkInactive(int day) throws SQLException {
        String sql = "SELECT nick FROM whitelist WHERE rest = 0 AND last_seen < ?";
        Set<String> set = new HashSet<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(day));

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getString("nick").toLowerCase(Locale.ROOT));
                }
            }

        }
        return set;
    }

    public int setRest(String nick, int day, int maxday) throws SQLException {
        String sql = "UPDATE whitelist SET rest = 1, rest_until = ? WHERE nick = ?";

        if (day <= maxday && day > 0) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setLong(1,System.currentTimeMillis() + TimeUnit.DAYS.toMillis(day));
                pstmt.setString(2, nick);

                return pstmt.executeUpdate();
            }
        } else if (day > maxday) {
            return 105;
        } else {
            return 106;
        }
    }

    public int cancelRest(String nick) throws SQLException {
        String sql = "UPDATE whitelist SET rest = 0, rest_until = 0, last_seen = ? WHERE nick = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setString(2,nick);

            return pstmt.executeUpdate();
        }
    }

    public Set<String> checkRest() throws SQLException {
        String sql = "SELECT nick FROM whitelist WHERE rest = 1 AND rest_until <= ?";
        Set<String> set = new HashSet<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    set.add(rs.getString("nick"));
                }
            }
        }

        resetRest(set);

        return set;
    }

    public void resetRest(Set<String> set) throws SQLException {
        String sql = "UPDATE whitelist SET rest = 0, rest_until = 0, last_seen = ? WHERE nick = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (String nick : set) {
                pstmt.setLong(1, System.currentTimeMillis());
                pstmt.setString(2, nick);

                pstmt.executeUpdate();
            }
        }
    }

    public int addPlayer(String nick) throws SQLException {
        String sql = "INSERT OR IGNORE INTO whitelist (nick, last_seen) VALUES (?,?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nick);
            pstmt.setLong(2, System.currentTimeMillis());

            return pstmt.executeUpdate();
        }
    }

    public int updateLastSeen(String nick) throws SQLException {
        String sql = "UPDATE whitelist SET last_seen = ? WHERE nick = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, System.currentTimeMillis());
            pstmt.setString(2, nick);

            return pstmt.executeUpdate();
        }
    }

    public int removePlayer(String nick) throws SQLException {
        String sql = "DELETE FROM whitelist WHERE nick = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nick);

            return pstmt.executeUpdate();
        }
    }


}
