package com.wyb.flink.cdc;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.descriptors.Csv;
import org.apache.flink.table.descriptors.Kafka;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.table.factories.TableSourceFactory;
import org.apache.flink.types.Row;

import org.apache.flink.formats.json.debezium.DebeziumJsonFormatFactory;
import org.apache.flink.formats.json.*;
import org.apache.flink.table.descriptors.Json;

public class DebeziumCDC {

    public static void main(String[] args) throws Exception {

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().useOldPlanner().build();;
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, settings);;

        // tEnv.getConfig().setSqlDialect(SqlDialect.DEFAULT);

        // debezium 捕获到变化的数据会写入到这个 topic 中
        String topicName = "dbserver1.inventory.customers";
        String bootStrpServers = "localhost:9092";
        String zk = "localhost:2181";
        String groupID = "testGroup";

        // 目标数据库地址
        String url = "jdbc:mysql://localhost:3306/inventory";
        String userName = "mysqluser";
        String password = "mysqlpw";
        String mysqlSinkTable = "customers_copy";

        tEnv.executeSql("SHOW databases").print(); // This seems to work;
        // tEnv.executeSql("SHOW TABLES").print(); // This seems to work;


        String srcTab1 = "CREATE TABLE customers (\n" +
                " id int,\n" +
                " first_name STRING,\n" +
                " last_name STRING,\n" +
                " email STRING \n" +
                ") WITH (\n" +
                // " 'connector.type' = 'kafka',\n" +
                // " 'connector.version' = 'universal', \n" +
                // " 'connector.topic' = '" + topicName + "',\n" +
                // " 'connector.properties.bootstrap.servers' = '"+bootStrpServers+"',\n" +
                // // " 'connector.properties.zookeeper.connect' = ' "+ zk + "',\n" +
                // " 'connector.properties.group.id' = '" + groupID + "',\n" +
                // " 'connector.properties.debezium-json.schema-include' = 'true',\n" +
                // " 'format.typeformat' = 'debezium-json'\n" +
                // " 'format.typeformat' = 'json'\n" +

                "'connector' = 'kafka',\n" +
                "'topic' = 'dbserver1.inventory.customers',\n" +
                "'properties.bootstrap.servers' = 'localhost:9092',\n" +
                "'properties.group.id' = 'testGroup',\n" +
                // -- using 'debezium-json' as the format to interpret Debezium JSON messages
                //-- please use 'debezium-avro-confluent' if Debezium encodes messages in Avro format
                "'debezium-json.schema-include' = 'true',\n" +
                "'format' = 'debezium-json'\n" +
                ")";


        String srcTab2 = "CREATE TABLE customers\n" +
                "(id BIGINT,\n" +
                " first_name varchar(255),\n" +
                " last_name varchar(255),\n" +
                " email varchar(255)\n" +
                ") WITH (\n" +
                "  'connector' = 'kafka',\n" +
                "  'topic' = 'dbserver1.inventory.customers',\n" +
                "  'properties.bootstrap.servers' = 'localhost:9092',\n" +
                "  'properties.group.id' = 'testGroup',\n" +
                "  'scan.startup.mode' = 'earliest-offset',\n" +
                "  'value.debezium-json.schema-include' = 'true',\n" +
                "  'value.format' = 'debezium-json'\n" +
                ")\n";
        // System.out.println(srcTab);
        // 创建一个 Kafka 数据源的表
        // tEnv.executeSql(srcTab);



        String srcTab = "CREATE TABLE customers\n" +
                "(id string,\n" +
                " name varchar(255)\n" +
                ") WITH (\n" +
                "  'connector' = 'kafka',\n" +
                "  'topic' = 'test_topic',\n" +
                "  'properties.bootstrap.servers' = 'localhost:9092',\n" +
                "  'properties.group.id' = 'testGroup',\n" +
                "  'scan.startup.mode' = 'earliest-offset',\n" +
                "  'format.type' = 'csv'\n" +
                ")\n";
        System.out.println(srcTab);
        tEnv.executeSql(srcTab);

        // tEnv
        //     .connect(
        //         new Kafka()
        //             .version("universal")
        //             .topic("test_topic")
        //             .property("zookeeper.connect", "localhost:2181")
        //             .property("bootstrap.servers", "localhost:9092")
        //             .startFromEarliest()
        //     )
        //     .withFormat(new Csv().lineDelimiter("\n").fieldDelimiter(','))
        //     .withSchema(new Schema()
        //             .field("id", DataTypes.STRING())
        //             .field("name", DataTypes.STRING()))
        //     .inAppendMode()
        // .createTemporaryTable("customers");
        //
        // // Table result = tEnv.sqlQuery("SELECT id, UPPER(name) FROM customers");
        // //
        // // tEnv.toAppendStream(result, Row.class).print();
        //
        // tEnv.executeSql("SHOW TABLES").print(); // This seems to work;
        tEnv.executeSql("SELECT id, UPPER(name) as `u-name` FROM customers").print(); // This seems to work;


        env.execute();


        // tEnv.executeSql("SELECT id, UPPER(name) as u-name FROM customers").print(); // This seems to work;

        // Table result = tEnv.sqlQuery("SELECT id, UPPER(name) FROM customers");
        // tEnv.toRetractStream(result, Row.class).print();
        // tEnv.execute("test");

        // // 创建一个写入数据的 sink 表
        // tEnv.executeSql("CREATE TABLE customers_copy (\n" +
        //         " id int(11) NOT NULL ,\n" +
        //         " first_name STRING,\n" +
        //         " last_name STRING,\n" +
        //         " email STRING \n" +
        //         ") WITH (\n" +
        //         " 'connector.type' = 'jdbc',\n" +
        //         " 'connector.url' = '" + url + "',\n" +
        //         " 'connector.username' = '" + userName + "',\n" +
        //         " 'connector.password' = '" + password + "',\n" +
        //         " 'connector.driver' = 'com.mysql.jdbc.Driver',\n" +
        //         " 'connector.table-name' = '" + mysqlSinkTable + "'\n" +
        //         ")");
        //
        //
        // String updateSQL = "insert into customers_copy select * from customers";
        // TableResult result = tEnv.executeSql(updateSQL);


        // Thread.currentThread().getContextClassLoader()
        // Block get results
        // result.getJobClient()
        //         .get()
        //         .getJobExecutionResult().get();

        // tEnv.execute("SQL Job");
    }
}