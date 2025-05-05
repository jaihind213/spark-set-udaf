package io.github.jaihind213;

import static org.apache.datasketches.thetacommon.ThetaUtil.DEFAULT_UPDATE_SEED;

import java.io.Serializable;
import org.apache.datasketches.memory.Memory;
import org.apache.datasketches.theta.*;
import org.apache.spark.sql.*;
import org.apache.spark.sql.expressions.Aggregator;

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
}
