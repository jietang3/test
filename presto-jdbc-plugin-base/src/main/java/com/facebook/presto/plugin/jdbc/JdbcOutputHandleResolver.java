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

import com.facebook.presto.spi.ConnectorOutputHandleResolver;
import com.facebook.presto.spi.OutputTableHandle;

import javax.inject.Inject;

import static com.google.common.base.Preconditions.checkNotNull;

public class JdbcOutputHandleResolver
        implements ConnectorOutputHandleResolver
{
    private final String connectorId;

    @Inject
    public JdbcOutputHandleResolver(JdbcConnectorId connectorId)
    {
        this.connectorId = checkNotNull(connectorId, "connectorId is null").toString();
    }

    @Override
    public boolean canHandle(OutputTableHandle handle)
    {
        return (handle instanceof JdbcOutputTableHandle) && ((JdbcOutputTableHandle) handle).getConnectorId().equals(connectorId);
    }

    @Override
    public Class<? extends OutputTableHandle> getOutputTableHandleClass()
    {
        return JdbcOutputTableHandle.class;
    }
}
