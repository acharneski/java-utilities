/*
 * Copyright (c) 2017 by Andrew Charneski.
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

package com.simiacryptus.util;

/**
 * The type Fast random.
 */
public class FastRandom {
  private static volatile double randomA = seed();
  
  private static double seed() {
    return Math.random();
  }
  
  private static final long mask = 0xABADC0DE;
  
  /**
   * Random double.
   *
   * @return the double
   */
  public static double random() {
    int i=0;
    while(true) {
      double prev = FastRandom.randomA;
      assert(Double.isFinite(prev));
      double next = 4 * prev * (1 - prev);
      if(!Double.isFinite(next)) next = seed();
      if(i++<3) FastRandom.randomA = next;
      else {
        //System.err.println(randomA);
        return next;
      }
    }
  }
}
