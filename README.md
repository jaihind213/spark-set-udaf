# spark-set-udaf
A library offering spark user defined aggregation functions (udaf) on apache theta sketches

## Installation java 17

```XML
<dependency>
    <groupId>io.github.jaihind213</groupId>
    <artifactId>spark-set-udaf</artifactId>
    <version>spark3.5.2-scala2.13-1.0.1</version>
</dependency>
```

## Installation java 11
```
<dependency>
    <groupId>io.github.jaihind213</groupId>
    <artifactId>spark-set-udaf</artifactId>
    <version>spark3.5.2-scala2.13-1.0.1-jdk11</version>
</dependency>
```

## How to use 

Refer to the test cases for examples of how to use the library.

### Java
```java
        SetAggregator setAggregator = new SetAggregator();
        spark.udf().register("set_sketch", functions.udaf(setAggregator, Encoders.STRING()));
        // generate a set sketch of the city column
        Dataset<Row> df1 = spark.sql("select set_sketch(city) as city_set from city1");
        
        //estimate
        spark.udf().register("estimate_set",new SetAggregator.EstimateSetUdf(),DataTypes.DoubleType);
        Dataset<Row> estimateDf = spark.sql("select estimate_set(set_sketch(city)) as city_set from city1");
        
        //union
        SetUnionAggregator setUnionAggregator = new SetUnionAggregator(nominal, seed);
        spark.udf().register("set_union", functions.udaf(setUnionAggregator, Encoders.BINARY()));

        String sql = "SELECT estimate_set(set_union(sets)) AS estimate\n" +
        "FROM (\n" +
        "  SELECT set_sketch(city) AS sets FROM city1\n" +
        "  UNION ALL\n" +
        "  SELECT set_sketch(city) AS sets FROM city2\n" +
        ") sub;";
        ataset<Row> unionEstimateDf = spark.sql(sql);
```


### Python
```python
    spark = (
            SparkSession.builder.appName("app")
            
            .config("spark.jars", "path_to_fat_jar")
            .config("spark.driver.extraClassPath", "path to spark-set-udaf-spark3.5.2-scala2.13-1.0.1-jar-with-dependencies.jar")
            .config("spark.executor.extraClassPath", "path to spark-set-udaf-spark3.5.2-scala2.13-1.0.1-jar-with-dependencies.jar")
            .config("spark.sql.session.timeZone", "GMT")
            .config("spark.driver.extraJavaOptions", "-Duser.timezone=GMT  -Dfile.encoding=UTF-8 --illegal-access=permit --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED --add-exports java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -XX:+UseG1GC")
            .config("spark.executor.extraJavaOptions", "-Duser.timezone=GMT  -Dfile.encoding=UTF-8 --illegal-access=permit --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED --add-exports java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/sun.util.calendar=ALL-UNNAMED --add-opens java.base/java.lang.invoke=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED -XX:+UseG1GC")
            .getOrCreate()
        )
    # Register the UDFs
    jvm = spark._jvm
    jvm_spark = spark._jsparkSession
    jvm.io.github.jaihind213.SetAggregator.register(jvm_spark, 4096, 9001)

    # create df with random string data
    df = spark.createDataFrame([("a",), ("b",), ("c",)], ["col1"])
    df.createOrReplaceTempView("foo")
    spark.sql("select estimate_set(set_sketch(col1)) as estimate_set from foo").show(truncate=False)

```
