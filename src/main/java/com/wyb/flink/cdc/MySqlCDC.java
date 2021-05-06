package com.wyb.flink.cdc;

import com.alibaba.ververica.cdc.connectors.mysql.MySQLSource;
import com.alibaba.ververica.cdc.debezium.StringDebeziumDeserializationSchema;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.descriptors.Schema;
import org.apache.flink.types.Row;

import com.alibaba.ververica.cdc.connectors.mysql.MySQLSource;


public class MySqlCDC {
    public static void main(String[] args) throws Exception {

        EnvironmentSettings fsSettings = EnvironmentSettings.newInstance().useOldPlanner().inStreamingMode().build();
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        StreamTableEnvironment tEnv = StreamTableEnvironment.create(env, fsSettings);



        tEnv.getConfig().setSqlDialect(SqlDialect.DEFAULT);

        String sqltext = "CREATE TABLE mysql_binlog (\n" +
                " id INT NOT NULL,\n" +
                " name STRING,\n" +
                " description STRING,\n" +
                " weight DECIMAL(10,3)\n" +
                ") WITH (\n" +
                " 'connector' = 'mysql-cdc',\n" +
                " 'hostname' = 'localhost',\n" +
                " 'port' = '3306',\n" +
                " 'username' = 'root',\n" +
                " 'password' = '123456',\n" +
                " 'database-name' = 'inventory',\n" +
                " 'table-name' = 'products'\n" +
                ")";

        // tEnv.executeSql(sqltext);

        // define a dynamic aggregating query
        Table result = tEnv.sqlQuery("SELECT id, UPPER(name), description, weight FROM mysql_binlog");

        // print the result to the console
        tEnv.toRetractStream(result, Row.class).print();


    }
}
