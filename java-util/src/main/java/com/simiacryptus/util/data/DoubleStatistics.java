/*
 * Copyright (c) 2018 by Andrew Charneski.
 *
 * The author licenses this file to you under the
 * Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.simiacryptus.util.data;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.stream.Collector;

/**
 * From: http://stackoverflow.com/questions/36263352/java-streams-standard-deviation Author: Tunaki
 */
public class DoubleStatistics extends DoubleSummaryStatistics {
  
  /**
   * The Collector.
   */
  @javax.annotation.Nonnull
  public static Collector<Double, com.simiacryptus.util.data.DoubleStatistics, com.simiacryptus.util.data.DoubleStatistics> COLLECTOR = Collector.of(
    com.simiacryptus.util.data.DoubleStatistics::new,
    com.simiacryptus.util.data.DoubleStatistics::accept,
    com.simiacryptus.util.data.DoubleStatistics::combine,
    d -> d
  );
  
  /**
   * The Numbers.
   */
  @javax.annotation.Nonnull
  public static Collector<Number, com.simiacryptus.util.data.DoubleStatistics, com.simiacryptus.util.data.DoubleStatistics> NUMBERS = Collector.of(
    com.simiacryptus.util.data.DoubleStatistics::new,
    (a, n) -> a.accept(n.doubleValue()),
    com.simiacryptus.util.data.DoubleStatistics::combine,
    d -> d
  );
  
  private double simpleSumOfSquare; // Used to compute right sum for non-finite inputs
  private double sumOfSquare = 0.0d;
  private double sumOfSquareCompensation; // Low order bits of sum
  
  @Override
  public synchronized void accept(final double value) {
    super.accept(value);
    final double squareValue = value * value;
    simpleSumOfSquare += squareValue;
    sumOfSquareWithCompensation(squareValue);
  }
  
  /**
   * Accept double statistics.
   *
   * @param value the value
   * @return the double statistics
   */
  @javax.annotation.Nonnull
  public com.simiacryptus.util.data.DoubleStatistics accept(@javax.annotation.Nonnull final double[] value) {
    Arrays.stream(value).forEach(this::accept);
    return this;
  }
  
  /**
   * Combine double statistics.
   *
   * @param other the other
   * @return the double statistics
   */
  @javax.annotation.Nonnull
  public com.simiacryptus.util.data.DoubleStatistics combine(@javax.annotation.Nonnull final com.simiacryptus.util.data.DoubleStatistics other) {
    super.combine(other);
    simpleSumOfSquare += other.simpleSumOfSquare;
    sumOfSquareWithCompensation(other.sumOfSquare);
    sumOfSquareWithCompensation(other.sumOfSquareCompensation);
    return this;
  }
  
  /**
   * Gets standard deviation.
   *
   * @return the standard deviation
   */
  public final double getStandardDeviation() {
    return getCount() > 0 ? Math.sqrt(getSumOfSquare() / getCount() - Math.pow(getAverage(), 2)) : 0.0d;
  }
  
  /**
   * Gets sum of square.
   *
   * @return the sum of square
   */
  public double getSumOfSquare() {
    final double tmp = sumOfSquare + sumOfSquareCompensation;
    if (Double.isNaN(tmp) && Double.isInfinite(simpleSumOfSquare)) {
      return simpleSumOfSquare;
    }
    return tmp;
  }
  
  private void sumOfSquareWithCompensation(final double value) {
    final double tmp = value - sumOfSquareCompensation;
    final double velvel = sumOfSquare + tmp; // Little wolf of rounding error
    sumOfSquareCompensation = velvel - sumOfSquare - tmp;
    sumOfSquare = velvel;
  }
  
  @Override
  public String toString() {
    return toString(1);
  }
  
  /**
   * To string string.
   *
   * @param scale the scale
   * @return the string
   */
  public String toString(final double scale) {
    return String.format("%.4e +- %.4e [%.4e - %.4e] (%d#)", getAverage() * scale, getStandardDeviation() * scale, getMin() * scale, getMax() * scale, getCount());
  }
}
