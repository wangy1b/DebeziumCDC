package com.wyb.flink.connectors;

import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.format.DecodingFormat;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.connector.source.abilities.SupportsFilterPushDown;
import org.apache.flink.table.connector.source.abilities.SupportsProjectionPushDown;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;

/**
 * The {@link SocketDynamicTableSource} is used during planning.
 *
 * <p>In our example, we don't implement any of the available ability interfaces such as {@link
 * SupportsFilterPushDown} or {@link SupportsProjectionPushDown}. Therefore, the main logic can be
 * found in {@link #getScanRuntimeProvider(ScanContext)} where we instantiate the required {@link
 * SourceFunction} and its {@link DeserializationSchema} for runtime. Both instances are
 * parameterized to return internal data structures (i.e. {@link RowData}).
 */
public final class SocketDynamicTableSource implements ScanTableSource {

    private final String hostname;
    private final int port;
    private final byte byteDelimiter;
    private final DecodingFormat<DeserializationSchema<RowData>> decodingFormat;
    private final DataType producedDataType;

    public SocketDynamicTableSource(
            String hostname,
            int port,
            byte byteDelimiter,
            DecodingFormat<DeserializationSchema<RowData>> decodingFormat,
            DataType producedDataType) {
        this.hostname = hostname;
        this.port = port;
        this.byteDelimiter = byteDelimiter;
        this.decodingFormat = decodingFormat;
        this.producedDataType = producedDataType;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        // in our example the format decides about the changelog mode
        // but it could also be the source itself
        return decodingFormat.getChangelogMode();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext runtimeProviderContext) {

        // create runtime classes that are shipped to the cluster

        final DeserializationSchema<RowData> deserializer =
                decodingFormat.createRuntimeDecoder(runtimeProviderContext, producedDataType);

        final SourceFunction<RowData> sourceFunction =
                new SocketSourceFunction(hostname, port, byteDelimiter, deserializer);

        return SourceFunctionProvider.of(sourceFunction, false);
    }

    @Override
    public DynamicTableSource copy() {
        return new SocketDynamicTableSource(
                hostname, port, byteDelimiter, decodingFormat, producedDataType);
    }

    @Override
    public String asSummaryString() {
        return "Socket Table Source";
    }
}