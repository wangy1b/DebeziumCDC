package com.wyb.flink.cdc;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import com.alibaba.ververica.cdc.debezium.StringDebeziumDeserializationSchema;
import com.alibaba.ververica.cdc.connectors.mysql.MySQLSource;
import org.apache.flink.table.plan.nodes.datastream.TimeCharacteristic;

public class MySqlBinlogSourceExample {
    public static void main(String[] args) throws Exception {
        SourceFunction<String> sourceFunction = MySQLSource.<String>builder()
                .hostname("localhost")
                .port(3306)
                .databaseList("inventory") // monitor all tables under inventory database
                // .username("flinkuser")
                // .password("flinkpw")
                // .username("mysqluser")
                // .password("mysqlpw")
                .username("root")
                .password("123456")
                // .tableList("products") //如果不写则监控库下的所有表，需要使用【库名.表名】
                .deserializer(new StringDebeziumDeserializationSchema()) // converts SourceRecord to String
                .build();

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime());

        env.addSource(sourceFunction).print().setParallelism(1); // use parallelism 1 for sink to keep message ordering

        env.execute();
    }
}