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

import com.facebook.presto.spi.Connector;
import com.facebook.presto.spi.ConnectorHandleResolver;
import com.facebook.presto.spi.ConnectorMetadata;
import com.facebook.presto.spi.ConnectorOutputHandleResolver;
import com.facebook.presto.spi.ConnectorRecordSetProvider;
import com.facebook.presto.spi.ConnectorRecordSinkProvider;
import com.facebook.presto.spi.ConnectorSplitManager;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class JdbcConnector
        implements Connector
{
    private final JdbcMetadata jdbcMetadata;
    private final JdbcSplitManager jdbcSplitManager;
    private final JdbcRecordSetProvider jdbcRecordSetProvider;
    private final JdbcHandleResolver jdbcHandleResolver;
    private final JdbcRecordSinkProvider jdbcRecordSinkProvider;
    private final JdbcOutputHandleResolver jdbcOutputHandleResolver;

    @Inject
    public JdbcConnector(
            JdbcMetadata jdbcMetadata,
            JdbcSplitManager jdbcSplitManager,
            JdbcRecordSetProvider jdbcRecordSetProvider,
            JdbcHandleResolver jdbcHandleResolver,
            JdbcRecordSinkProvider jdbcRecordSinkProvider,
            JdbcOutputHandleResolver jdbcOutputHandleResolver)
    {
        this.jdbcMetadata = checkNotNull(jdbcMetadata, "jdbcMetadata is null");
        this.jdbcSplitManager = checkNotNull(jdbcSplitManager, "jdbcSplitManager is null");
        this.jdbcRecordSetProvider = checkNotNull(jdbcRecordSetProvider, "jdbcRecordSetProvider is null");
        this.jdbcHandleResolver = checkNotNull(jdbcHandleResolver, "jdbcHandleResolver is null");
        this.jdbcRecordSinkProvider = checkNotNull(jdbcRecordSinkProvider, "jdbcRecordSinkProvider is null");
        this.jdbcOutputHandleResolver = checkNotNull(jdbcOutputHandleResolver, "jdbcOutputHandleResolver is null");
    }

    @Override
    public ConnectorMetadata getMetadata()
    {
        return jdbcMetadata;
    }

    @Override
    public ConnectorSplitManager getSplitManager()
    {
        return jdbcSplitManager;
    }

    @Override
    public ConnectorRecordSetProvider getRecordSetProvider()
    {
        return jdbcRecordSetProvider;
    }

    @Override
    public ConnectorHandleResolver getHandleResolver()
    {
        return jdbcHandleResolver;
    }

    @Override
    public ConnectorRecordSinkProvider getRecordSinkProvider()
    {
        return jdbcRecordSinkProvider;
    }

    @Override
    public ConnectorOutputHandleResolver getOutputHandleResolver()
    {
        return jdbcOutputHandleResolver;
    }
}
