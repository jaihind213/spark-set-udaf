# spark-set-udaf
A library offering spark user defined aggregation functions (udaf) on apache theta sketches

## Installation

```XML
<dependency>
    <groupId>io.github.jaihind213</groupId>
    <artifactId>spark-set-udaf</artifactId>
    <version>spark3.5.2-scala2.13-1.0.0</version>
</dependency>
```

## How to use 

Refer to the test cases for examples of how to use the library.

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
