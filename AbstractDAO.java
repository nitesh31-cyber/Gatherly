package com.gatherly.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * OOP — INHERITANCE + COLLECTIONS & GENERICS + DATABASE OPERATIONS
 * Abstract generic base class for every DAO. <T> is bound to a concrete
 * entity type by each subclass (e.g. "class UserDAO extends AbstractDAO<User>").
 *
 * runQuery() is written ONCE, here, and reused by every DAO: it opens a
 * connection, binds parameters, runs the query, and builds a properly
 * typed List<T> by calling mapRow() for each row.
 *
 * mapRow() is abstract — UserDAO maps a row to a User, EventDAO maps a row
 * to an EventRecord, and so on. That's POLYMORPHISM again: runQuery() calls
 * `this.mapRow(rs)` without caring which subclass it's actually running in.
 *
 * EXCEPTION HANDLING: every JDBC call here can throw SQLException (a checked
 * exception), so it must be declared on every method signature — the
 * compiler won't allow it to be silently ignored. Connections and
 * statements are opened in try-with-resources blocks so they're closed
 * automatically even when a query fails partway through.
 */
public abstract class AbstractDAO<T> implements Repository<T> {

    /** Each concrete DAO says how to turn one row of its own table into a T. */
    protected abstract T mapRow(ResultSet rs) throws SQLException;

    /** Runs a SELECT and maps every row into a List<T> (GENERICS + COLLECTIONS). */
    protected List<T> runQuery(String sql, Object... params) throws SQLException {
        List<T> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    /** Runs an INSERT/UPDATE/DELETE and returns the number of affected rows. */
    protected int runUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindParams(ps, params);
            return ps.executeUpdate();
        }
    }

    /** Same as runUpdate(), but also returns the auto-generated id (for INSERTs). */
    protected int runInsert(String sql, Object... params) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            bindParams(ps, params);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    private void bindParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
