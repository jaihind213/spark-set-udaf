package io.github.jaihind213;

import static org.apache.datasketches.thetacommon.ThetaUtil.DEFAULT_UPDATE_SEED;

import java.io.Serializable;
import org.apache.datasketches.memory.Memory;
import org.apache.datasketches.theta.Sketch;
import org.apache.datasketches.theta.Sketches;
import org.apache.datasketches.theta.Union;
import org.apache.datasketches.theta.UpdateSketch;
import org.apache.spark.sql.Encoder;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.expressions.Aggregator;

public class SetUnionAggregator extends Aggregator<byte[], byte[], byte[]> implements Serializable {

  public static final long serialVersionUID = -610177748542263615L;

  private final int nominal;

  private final Long seed;

  public SetUnionAggregator(int nominal, Long seed) {
    this.nominal = nominal;
    this.seed = seed;
  }

  public SetUnionAggregator() {
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
  public byte[] reduce(byte[] b, byte[] a) {
    return merge(b, a);
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
