/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.plugin.jdbc;

import com.facebook.presto.spi.RecordSink;
import com.google.common.base.Throwables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.base.Preconditions.checkState;

public class JdbcRecordSink
        implements RecordSink
{
    private final Connection connection;
    private final PreparedStatement statement;

    private final int fieldCount;
    private int field = -1;
    private int batchSize;

    public JdbcRecordSink(JdbcOutputTableHandle handle, JdbcClient jdbcClient)
    {
        try {
            connection = jdbcClient.getConnection(handle);
            connection.setAutoCommit(false);
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }

        try {
            statement = connection.prepareStatement(jdbcClient.buildInsertSql(handle));
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }

        fieldCount = handle.getColumnNames().size();
    }

    @Override
    public void beginRecord(long sampleWeight)
    {
        checkState(field == -1, "already in record");
        field = 0;
    }

    @Override
    public void finishRecord()
    {
        checkState(field != -1, "not in record");
        checkState(field == fieldCount, "not all fields set");
        field = -1;

        try {
            statement.addBatch();
            batchSize++;

            if (batchSize >= 1000) {
                statement.executeBatch();
                connection.commit();
                connection.setAutoCommit(false);
                batchSize = 0;
            }
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void appendNull()
    {
        try {
            statement.setObject(next(), null);
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void appendBoolean(boolean value)
    {
        try {
            statement.setBoolean(next(), value);
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void appendLong(long value)
    {
        try {
            statement.setLong(next(), value);
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void appendDouble(double value)
    {
        try {
            statement.setDouble(next(), value);
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void appendString(byte[] value)
    {
        try {
            statement.setString(next(), new String(value, UTF_8));
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public String commit()
    {
        // commit and close
        try (Connection connection = this.connection) {
            if (batchSize > 0) {
                statement.executeBatch();
                connection.commit();
            }
        }
        catch (SQLException e) {
            throw Throwables.propagate(e);
        }
        return ""; // the committer does not need any additional info
    }

    private int next()
    {
        checkState(field != -1, "not in record");
        checkState(field < fieldCount, "all fields already set");
        field++;
        return field;
    }
}
