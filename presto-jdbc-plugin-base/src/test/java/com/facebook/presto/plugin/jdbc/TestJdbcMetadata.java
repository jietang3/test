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

import com.facebook.presto.spi.ColumnMetadata;
import com.facebook.presto.spi.ColumnType;
import com.facebook.presto.spi.ConnectorTableMetadata;
import com.facebook.presto.spi.SchemaTableName;
import com.facebook.presto.spi.TableNotFoundException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.facebook.presto.spi.ColumnType.LONG;
import static com.facebook.presto.spi.ColumnType.STRING;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test(singleThreaded = true)
public class TestJdbcMetadata
{
    private static final String CONNECTOR_ID = "TEST";

    private TestingDatabase database;
    private JdbcMetadata metadata;
    private JdbcTableHandle tableHandle;

    @BeforeMethod
    public void setUp()
            throws Exception
    {
        database = new TestingDatabase();
        metadata = new JdbcMetadata(new JdbcConnectorId(CONNECTOR_ID), database.getJdbcClient());
        tableHandle = metadata.getTableHandle(new SchemaTableName("example", "numbers"));
    }

    @AfterMethod
    public void tearDown()
            throws Exception
    {
        database.close();
    }

    @Test
    public void testCanHandle()
    {
        assertTrue(metadata.canHandle(new JdbcTableHandle(CONNECTOR_ID, new SchemaTableName("schema", "table"), "catalog", "schema", "table")));
        assertFalse(metadata.canHandle(new JdbcTableHandle("unknown", new SchemaTableName("schema", "table"), "catalog", "schema", "table")));
    }

    @Test
    public void testListSchemaNames()
    {
        assertTrue(metadata.listSchemaNames().containsAll(ImmutableSet.of("example", "tpch")));
    }

    @Test
    public void testGetTableHandle()
    {
        JdbcTableHandle tableHandle = metadata.getTableHandle(new SchemaTableName("example", "numbers"));
        assertEquals(metadata.getTableHandle(new SchemaTableName("example", "numbers")), tableHandle);
        assertNull(metadata.getTableHandle(new SchemaTableName("example", "unknown")));
        assertNull(metadata.getTableHandle(new SchemaTableName("unknown", "numbers")));
        assertNull(metadata.getTableHandle(new SchemaTableName("unknown", "unknown")));
    }

    @Test
    public void testGetColumnHandle()
    {
        // known column
        assertEquals(metadata.getColumnHandle(tableHandle, "text"),
                new JdbcColumnHandle(CONNECTOR_ID, "text", STRING, 0));

        // unknown column
        assertNull(metadata.getColumnHandle(tableHandle, "unknown"));

        // unknown table
        try {
            metadata.getColumnHandle(new JdbcTableHandle(CONNECTOR_ID, new SchemaTableName("example", "numbers"), "unknown", "unknown", "unknown"), "unknown");
            fail("Expected getColumnHandle of unknown table to throw a TableNotFoundException");
        }
        catch (TableNotFoundException expected) {
        }
        try {
            metadata.getColumnHandle(new JdbcTableHandle(CONNECTOR_ID, new SchemaTableName("example", "unknown"), null, "example", "unknown"), "unknown");
            fail("Expected getColumnHandle of unknown table to throw a TableNotFoundException");
        }
        catch (TableNotFoundException expected) {
        }
    }

    @Test
    public void testGetColumnHandles()
    {
        // known table
        assertEquals(metadata.getColumnHandles(tableHandle), ImmutableMap.of(
                "text", new JdbcColumnHandle(CONNECTOR_ID, "text", STRING, 0),
                "value", new JdbcColumnHandle(CONNECTOR_ID, "value", LONG, 1)));

        // unknown table
        try {
            metadata.getColumnHandles(new JdbcTableHandle(CONNECTOR_ID, new SchemaTableName("unknown", "unknown"), "unknown", "unknown", "unknown"));
            fail("Expected getColumnHandle of unknown table to throw a TableNotFoundException");
        }
        catch (TableNotFoundException expected) {
        }
        try {
            metadata.getColumnHandles(new JdbcTableHandle(CONNECTOR_ID, new SchemaTableName("example", "numbers"), null, "example", "unknown"));
            fail("Expected getColumnHandle of unknown table to throw a TableNotFoundException");
        }
        catch (TableNotFoundException expected) {
        }
    }

    @Test
    public void getTableMetadata()
    {
        // known table
        ConnectorTableMetadata tableMetadata = metadata.getTableMetadata(tableHandle);
        assertEquals(tableMetadata.getTable(), new SchemaTableName("example", "numbers"));
        assertEquals(tableMetadata.getColumns(), ImmutableList.of(
                new ColumnMetadata("text", STRING, 0, false),
                new ColumnMetadata("value", ColumnType.LONG, 1, false)));

        // unknown tables should produce null
        assertNull(metadata.getTableMetadata(new JdbcTableHandle(CONNECTOR_ID, new SchemaTableName("u", "numbers"), null, "unknown", "unknown")));
        assertNull(metadata.getTableMetadata(new JdbcTableHandle(CONNECTOR_ID, new SchemaTableName("example", "numbers"), null, "example", "unknown")));
        assertNull(metadata.getTableMetadata(new JdbcTableHandle(CONNECTOR_ID, new SchemaTableName("example", "numbers"), null, "unknown", "numbers")));
    }

    @Test
    public void testListTables()
    {
        // all schemas
        assertEquals(ImmutableSet.copyOf(metadata.listTables(null)), ImmutableSet.of(
                new SchemaTableName("example", "numbers"),
                new SchemaTableName("tpch", "orders"),
                new SchemaTableName("tpch", "lineitem")));

        // specific schema
        assertEquals(ImmutableSet.copyOf(metadata.listTables("example")), ImmutableSet.of(
                new SchemaTableName("example", "numbers")));
        assertEquals(ImmutableSet.copyOf(metadata.listTables("tpch")), ImmutableSet.of(
                new SchemaTableName("tpch", "orders"),
                new SchemaTableName("tpch", "lineitem")));

        // unknown schema
        assertEquals(ImmutableSet.copyOf(metadata.listTables("unknown")), ImmutableSet.of());
    }

    @Test
    public void getColumnMetadata()
    {
        assertEquals(metadata.getColumnMetadata(tableHandle, new JdbcColumnHandle(CONNECTOR_ID, "text", STRING, 0)),
                new ColumnMetadata("text", STRING, 0, false));

        // example connector assumes that the table handle and column handle are
        // properly formed, so it will return a metadata object for any
        // ExampleTableHandle and ExampleColumnHandle passed in.  This is on because
        // it is not possible for the Presto Metadata system to create the handles
        // directly.
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testCreateTable()
    {
        metadata.createTable(new ConnectorTableMetadata(
                new SchemaTableName("example", "foo"),
                ImmutableList.of(new ColumnMetadata("text", STRING, 0, false))));
    }

    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void testDropTableTable()
    {
        metadata.dropTable(tableHandle);
    }
}
