## Debezium on docker

## 1.Start Zookeeper

~~~ shell
$ docker run -d -it --rm --name zookeeper -p 2181:2181 -p 2888:2888 -p 3888:3888 debezium/zookeeper:1.5

~~~

## 2.Start Kafka

~~~ shell
$ docker run -d -it --rm --name kafka -p 9092:9092 --link zookeeper:zookeeper debezium/kafka:1.5

~~~

## 3.Start a MySQL database

~~~ shell
$ docker run -d -it --rm --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=debezium -e MYSQL_USER=mysqluser -e MYSQL_PASSWORD=mysqlpw debezium/example-mysql:1.5


~~~

## 4.Start Kafka Connect

```shell
$ docker run -d -it --rm --name connect -p 8083:8083 -e GROUP_ID=1 -e CONFIG_STORAGE_TOPIC=my_connect_configs -e OFFSET_STORAGE_TOPIC=my_connect_offsets -e STATUS_STORAGE_TOPIC=my_connect_statuses --link zookeeper:zookeeper --link kafka:kafka --link mysql:mysql debezium/connect:1.5

```

## 5.Registering a connector to monitor the `inventory` database

* Review the configuration of the Debezium MySQL connector that you will register.

Before registering the connector, you should be familiar with its configuration. In the next step, you will register the following connector:

```json
{
  "name": "inventory-connector",  #1 
  "config": {  #2
    "connector.class": "io.debezium.connector.mysql.MySqlConnector",
    "tasks.max": "1", #3 
    "database.hostname": "mysql",  #4 
    "database.port": "3306",
    "database.user": "debezium",
    "database.password": "dbz",
    "database.server.id": "184054",  #5
    "database.server.name": "dbserver1",  #5
    "database.include.list": "inventory",  #6
    "database.history.kafka.bootstrap.servers": "kafka:9092",  #7
    "database.history.kafka.topic": "schema-changes.inventory"  #7
  }
}
```

> 1. The name of the connector.
> 2. The connector’s configuration.
> 3. Only one task should operate at any one time. Because the MySQL connector reads the MySQL server’s `binlog`, using a single connector task ensures proper order and event handling. The Kafka Connect service uses connectors to start one or more tasks that do the work, and it automatically distributes the running tasks across the cluster of Kafka Connect services. If any of the services stop or crash, those tasks will be redistributed to running services.
> 4. The database host, which is the name of the Docker container running the MySQL server (`mysql`). Docker manipulates the network stack within the containers so that each linked container can be resolved with `/etc/hosts` using the container name for the host name. If MySQL were running on a normal network, you would specify the IP address or resolvable host name for this value.
> 5. A unique server ID and name. The server name is the logical identifier for the MySQL server or cluster of servers. This name will be used as the prefix for all Kafka topics.
> 6. Only changes in the `inventory` database will be detected.
> 7. The connector will store the history of the database schemas in Kafka using this broker (the same broker to which you are sending events) and topic name. Upon restart, the connector will recover the schemas of the database that existed at the point in time in the `binlog` when the connector should begin reading.



This command uses `localhost` to connect to the Docker host. If you are using a non-native Docker platform, replace `localhost` with the IP address of of your Docker host.

~~~ shell
$ curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d '{ "name": "inventory-connector", "config": { "connector.class": "io.debezium.connector.mysql.MySqlConnector", "tasks.max": "1", "database.hostname": "mysql", "database.port": "3306", "database.user": "debezium", "database.password": "dbz", "database.server.id": "184054", "database.server.name": "dbserver1", "database.include.list": "inventory", "database.history.kafka.bootstrap.servers": "kafka:9092", "database.history.kafka.topic": "dbhistory.inventory" } }'
~~~

windows users may need to escape the douvle-quotes

~~~ shell
curl -i -X POST -H "Accept:application/json" -H "Content-Type:application/json" localhost:8083/connectors/ -d "{ \"name\": \"inventory-connector\", \"config\": { \"connector.class\": \"io.debezium.connector.mysql.MySqlConnector\", \"tasks.max\": \"1\", \"database.hostname\": \"mysql\", \"database.port\": \"3306\", \"database.user\": \"debezium\", \"database.password\": \"dbz\", \"database.server.id\": \"184054\", \"database.server.name\": \"dbserver1\", \"database.include.list\": \"inventory\", \"database.history.kafka.bootstrap.servers\": \"kafka:9092\", \"database.history.kafka.topic\": \"dbhistory.inventory\" } }"
~~~



* use postman to register a connector 

url:

~~~ 
 localhost:8083/connectors/
~~~

header:

~~~ 
Accept：application/json
Content-Type：application/json
~~~

body:

~~~ json
{
    "name": "inventory-connector",
    "config": {
        "connector.class": "io.debezium.connector.mysql.MySqlConnector",
        "tasks.max": "1",
        "database.hostname": "mysql",
        "database.port": "3306",
        "database.user": "debezium",
        "database.password": "dbz",
        "database.server.id": "184054",
        "database.server.name": "dbserver1",
        "database.include.list": "inventory",
        "database.history.kafka.bootstrap.servers": "kafka:9092",
        "database.history.kafka.topic": "dbhistory.inventory"
    }
}
~~~

## 6.Viewing a *create* event 

~~~ shell
$ docker run -it --rm --name watcher --link zookeeper:zookeeper --link kafka:kafka debezium/kafka:1.5 watch-topic -a -k dbserver1.inventory.customers
~~~

## 7.Start a MySQL command line client

```shell
$ docker run -it --rm --name mysqlterm --link mysql --rm mysql:5.7 sh -c 'exec mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"'

# windows 命令：
$ docker run -it --rm --name mysqlterm --link mysql --rm mysql:5.7 sh -c "exec mysql -h$MYSQL_PORT_3306_TCP_ADDR -P$MYSQL_PORT_3306_TCP_PORT -uroot -p$MYSQL_ENV_MYSQL_ROOT_PASSWORD"

# 或者 拆分成两步：先进docker,再进mysql

$ docker run -d -it --rm --name mysqlterm --link mysql --rm mysql:5.7 sh
$ mysql -h"$MYSQL_PORT_3306_TCP_ADDR" -P"$MYSQL_PORT_3306_TCP_PORT" -uroot -p"$MYSQL_ENV_MYSQL_ROOT_PASSWORD"'

```

Verify that the MySQL command line client started.

and then watch the `watcher`

```shell
mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| information_schema |
| inventory          |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
5 rows in set (0.00 sec)

mysql> mysql> use inventory;
Reading table information for completion of table and column names
You can turn off this feature to get a quicker startup with -A

mysql> show tables;
+---------------------+
| Tables_in_inventory |
+---------------------+
| addresses           |
| customers           |
| geom                |
| orders              |
| products            |
| products_on_hand    |
+---------------------+
6 rows in set (0.00 sec)

mysql> SELECT * FROM customers;
+------+------------+-----------+-----------------------+
| id   | first_name | last_name | email                 |
+------+------------+-----------+-----------------------+
| 1001 | Sally      | Thomas    | sally.thomas@acme.com |
| 1002 | George     | Bailey    | gbailey@foobar.com    |
| 1003 | Edward     | Walker    | ed@walker.com         |
| 1004 | Anne211    | Kretchmar | annek@noanswer.org    |
+------+------------+-----------+-----------------------+
4 rows in set (0.00 sec)

mysql> UPDATE customers SET first_name='Anne211' WHERE id=1004;
Query OK, 1 row affected (0.01 sec)
Rows matched: 1  Changed: 1  Warnings: 0
```

## 8.use Flink cdc

利用flink Table API 读kafak 传输的debezium-json的数据成一张表，格式配置一直存在问题，具体参考 [Kafka Connector](https://ci.apache.org/projects/flink/flink-docs-release-1.12/dev/table/connect.html#kafka-connector)

~~~ java

~~~



## 9.stop all containers

~~~ shell
$ docker stop mysqlterm watcher connect mysql kafka zookeeper
~~~



