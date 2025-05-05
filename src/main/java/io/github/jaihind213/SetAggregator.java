package io.github.jaihind213;

import static org.apache.datasketches.thetacommon.ThetaUtil.DEFAULT_UPDATE_SEED;

import java.io.Serializable;
import org.apache.datasketches.memory.Memory;
import org.apache.datasketches.theta.*;
import org.apache.spark.sql.*;
import org.apache.spark.sql.api.java.UDF1;
import org.apache.spark.sql.expressions.Aggregator;
import org.apache.spark.sql.types.DataTypes;

/** Aggregator implementing addition of an element to a set sketch. */
public class SetAggregator extends Aggregator<String, byte[], byte[]> implements Serializable {
  private static final long serialVersionUID = -610177748542263615L;
  private final int nominal;

  private final Long seed;
  public static final String SET_SKETCH_UDAF_FUNCTION_NAME = "set_sketch";

  public SetAggregator(Integer nominalEntries, Long seed) {
    this.nominal = nominalEntries <= 0 ? 4096 : nominalEntries;
    // note: citus hll functions uses 0 as default seed.
    this.seed = seed < 0 ? DEFAULT_UPDATE_SEED : seed;
    System.out.println("Set-Aggregator: Using nominal entries: " + nominal);
    System.out.println("Set-Aggregator: Using seed: " + seed);
  }

  public SetAggregator() {
    this(
        Integer.parseInt(System.getProperty("SKETCH_NOMINAL_ENTRIES", "4096")),
        Long.parseLong(System.getProperty("SKETCH_SEED", String.valueOf(DEFAULT_UPDATE_SEED))));
  }

  @Override
  public byte[] zero() {
    UpdateSketch zero =
        Sketches.updateSketchBuilder().setNominalEntries(nominal).setSeed(seed).build();
    return zero.toByteArray();
  }

  @Override
  public byte[] reduce(byte[] existingSketchBytes, String elem) {
    if (elem == null) {
      return existingSketchBytes;
    }

    UpdateSketch existingSketch =
        ((UpdateSketch) Sketches.heapifySketch(Memory.wrap(existingSketchBytes), seed));
    existingSketch.update(elem);
    return existingSketch.toByteArray();
  }

  @Override
  public byte[] merge(byte[] b1, byte[] b2) {
    Sketch s1 = ((Sketch) Sketches.heapifySketch(Memory.wrap(b1), seed));
    Sketch s2 = ((Sketch) Sketches.heapifySketch(Memory.wrap(b2), seed));
    Union union =
        Sketches.setOperationBuilder().setNominalEntries(nominal).setSeed(seed).buildUnion();
    return union.union(s1, s2).toByteArray();
  }

  @Override
  public byte[] finish(byte[] reduction) {
    Sketch s1 = ((Sketch) Sketches.heapifySketch(Memory.wrap(reduction), seed));
    return s1.compact().toByteArray();
  }

  @Override
  public Encoder<byte[]> bufferEncoder() {
    return Encoders.BINARY();
  }

  @Override
  public Encoder<byte[]> outputEncoder() {
    return Encoders.BINARY();
  }

  public static class EstimateSetUdf implements UDF1<byte[], Double> {

    private static final long serialVersionUID = 3673392114521265088L;

    public EstimateSetUdf() {}

    @Override
    public Double call(byte[] setSketch) throws Exception {
      return Sketches.heapifyCompactSketch(Memory.wrap(setSketch)).getEstimate();
    }
  }

  public static void register(SparkSession spark, int nominalEntries, long seed) {
    SetAggregator setAggregator = new SetAggregator();
    spark
        .udf()
        .register(SET_SKETCH_UDAF_FUNCTION_NAME, functions.udaf(setAggregator, Encoders.STRING()));

    SetUnionAggregator setUnionAggregator = new SetUnionAggregator(nominalEntries, seed);
    spark.udf().register("set_union", functions.udaf(setUnionAggregator, Encoders.BINARY()));

    spark.udf().register("estimate_set", new SetAggregator.EstimateSetUdf(), DataTypes.DoubleType);
    System.err.println("SPARK SET UDF REGISTRATION COMPLETE.........");
  }
}
