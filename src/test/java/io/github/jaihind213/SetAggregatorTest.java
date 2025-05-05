package io.github.jaihind213;

import static io.github.jaihind213.SetAggregator.SET_SKETCH_UDAF_FUNCTION_NAME;

import java.util.ArrayList;
import java.util.List;
import org.apache.datasketches.memory.Memory;
import org.apache.datasketches.theta.CompactSketch;
import org.apache.datasketches.theta.Intersection;
import org.apache.datasketches.theta.Sketches;
import org.apache.datasketches.theta.Union;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.junit.Assert;
import org.junit.Test;

public class SetAggregatorTest {

  @Test
  public void testSetAggregation() {
    int nominal = 4096;
    long seed = 9001;

    SparkSession spark = SparkSession.builder().appName("Test").master("local[*]").getOrCreate();
    try {
      StructType schema =
          new StructType()
              .add("city", DataTypes.StringType, false)
              .add("count_", DataTypes.IntegerType, false);
      List<Row> rows = new ArrayList<>();
      final int numRows = Integer.parseInt(System.getProperty("TEST_NUM_ENTRIES", "1000000"));

      // superset creation = set1
      for (int i = 0; i < numRows; i++) {
        rows.add(RowFactory.create("city_" + i, i));
      }

      Dataset<Row> dataFrame1 = spark.createDataFrame(rows, schema);
      dataFrame1.createOrReplaceTempView("city1");

      // subset creation set2
      rows.clear();
      for (int i = 0; i < (numRows / 2); i++) {
        rows.add(RowFactory.create("city_" + i, i));
      }
      Dataset<Row> dataFrame2 = spark.createDataFrame(rows, schema);
      dataFrame2.createOrReplaceTempView("city2");

      // register udaff
      SetAggregator setAggregator = new SetAggregator();
      spark
          .udf()
          .register(
              SET_SKETCH_UDAF_FUNCTION_NAME, functions.udaf(setAggregator, Encoders.STRING()));

      Dataset<Row> df1 = spark.sql("select set_sketch(city) as city_set from city1");
      byte[] set1Bytes = (byte[]) df1.first().get(0);
      CompactSketch sketch1 = Sketches.heapifyCompactSketch(Memory.wrap(set1Bytes));
      double setAdditionPercentError = getPercentError(sketch1.getEstimate(), numRows);
      System.out.println("% error set addition: " + setAdditionPercentError);
      Assert.assertTrue("set addition error should be less than 3%", setAdditionPercentError < 3.0);

      Dataset<Row> df2 = spark.sql("select set_sketch(city) as city_set from city2");

      byte[] setBytes2 = (byte[]) df2.first().get(0);
      CompactSketch sketch2 = Sketches.heapifyCompactSketch(Memory.wrap(setBytes2));

      Union union =
          Sketches.setOperationBuilder().setNominalEntries(nominal).setSeed(seed).buildUnion();
      CompactSketch unionSketch = union.union(sketch1, sketch2);
      double setUnionPctError = getPercentError(unionSketch.getEstimate(), numRows);
      Assert.assertTrue("set union error should be less than 3%", setUnionPctError < 3.0);

      Intersection intersection =
          Sketches.setOperationBuilder()
              .setNominalEntries(nominal)
              .setSeed(seed)
              .buildIntersection();
      CompactSketch intersectionSketch = intersection.intersect(sketch1, sketch2);
      double setIntersectinPctError =
          getPercentError(intersectionSketch.getEstimate(), numRows / 2);
      Assert.assertTrue(
          "set intersection error should be less than 3%", setIntersectinPctError < 3.0);

    } finally {
      spark.close();
    }
  }

  private static double getPercentError(double actual, int numRows) {
    double e = Math.abs((double) (numRows - actual) / numRows) * 100;
    return e < 0 ? e * -1 : e;
  }
}
