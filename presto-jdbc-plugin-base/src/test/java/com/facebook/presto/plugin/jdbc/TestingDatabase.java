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

import com.facebook.presto.spi.PartitionResult;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.SplitSource;
import com.facebook.presto.spi.TupleDomain;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.google.common.collect.Iterables.getOnlyElement;

public final class TestingDatabase
        implements AutoCloseable
{
    private final Connection connection;
    private final JdbcClient jdbcClient;

    public TestingDatabase()
            throws SQLException
    {
        String connectionUrl = "jdbc:h2:mem:test" + System.nanoTime();
        jdbcClient = new BaseJdbcClient(
                new JdbcConnectorId("test"),
                new BaseJdbcConfig()
                        .setDriverClass("org.h2.Driver")
                        .setConnectionUrl(connectionUrl),
                "\""
        );

        connection = DriverManager.getConnection(connectionUrl);
        connection.createStatement().execute("CREATE SCHEMA example");

        connection.createStatement().execute("CREATE TABLE example.numbers(text varchar primary key, value bigint)");
        connection.createStatement().execute("INSERT INTO example.numbers(text, value) VALUES " +
                "('one', 1)," +
                "('two', 2)," +
                "('three', 3)," +
                "('ten', 10)," +
                "('eleven', 11)," +
                "('twelve', 12)" +
                "");
        connection.createStatement().execute("CREATE SCHEMA tpch");
        connection.createStatement().execute("CREATE TABLE tpch.orders(orderkey bigint primary key, custkey bigint)");
        connection.createStatement().execute("CREATE TABLE tpch.lineitem(orderkey bigint primary key, partkey bigint)");

        connection.commit();
    }

    @Override
    public void close()
            throws SQLException
    {
        connection.close();
    }

    public Connection getConnection()
    {
        return connection;
    }

    public JdbcClient getJdbcClient()
    {
        return jdbcClient;
    }

    public JdbcSplit getSplit(String schemaName, String tableName)
            throws InterruptedException
    {
        JdbcTableHandle jdbcTableHandle = jdbcClient.getTableHandle(new SchemaTableName(schemaName, tableName));
        PartitionResult partitions = jdbcClient.getPartitions(jdbcTableHandle, TupleDomain.all());
        SplitSource splits = jdbcClient.getPartitionSplits((JdbcPartition) getOnlyElement(partitions.getPartitions()));
        return (JdbcSplit) getOnlyElement(splits.getNextBatch(1000));
    }
}
