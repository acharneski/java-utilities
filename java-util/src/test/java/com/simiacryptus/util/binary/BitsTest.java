package com.simiacryptus.util.binary;

import com.simiacryptus.util.test.TestCategories;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Random;

public class BitsTest
{
  Random random = new Random();
  
  private long randomLong()
  {
    return this.random.nextLong() >> this.random.nextInt(62);
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testConcatenate() throws JSONException, IOException
  {
    for (int i = 0; i < 1000; i++)
    {
      this.testConcatenate(this.randomLong(), this.randomLong());
    }
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testDivide() throws JSONException, IOException
  {
    Assert.assertEquals("1", Bits.divide(2, 2, 10).toBitString());
    Assert.assertEquals("0", Bits.divide(0, 2, 10).toBitString());
    Assert.assertEquals("01", Bits.divide(1, 2, 10).toBitString());
    Assert.assertEquals("0011001100", Bits.divide(2, 5, 10).toBitString());
    Assert.assertEquals("01", Bits.divide(2, 4, 10).toBitString());
    Assert.assertEquals("0001", Bits.divide(171, 1368, 15).toBitString());
    Assert.assertEquals("000010001000001", Bits.divide(91, 1368, 15).toBitString());
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testBitStream() throws JSONException, IOException
  {
    Bits totalBits = BitOutputStream.toBits(out->{
      try {
        out.write(Bits.divide(1, 2, 10));
        out.write(Bits.divide(1, 2, 10));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    });
    Assert.assertEquals("0101", totalBits.toBitString());
  }

  @Test
  @Category(TestCategories.UnitTest.class)
  public void testInterval() throws JSONException, IOException
  {
    Assert.assertEquals("1", new Interval(1, 2, 3).toBits().toBitString());

    Assert.assertEquals("01", new Interval(0, 1, 2).toBits().toBitString());
    Assert.assertEquals("11", new Interval(1, 1, 2).toBits().toBitString());
    
    Assert.assertEquals("001", new Interval(0, 1, 3).toBits().toBitString());
    Assert.assertEquals("011", new Interval(1, 1, 3).toBits().toBitString());
    Assert.assertEquals("11", new Interval(2, 1, 3).toBits().toBitString());
    
    Assert.assertEquals("0001", new Interval(0, 1, 5).toBits().toBitString());
    Assert.assertEquals("010", new Interval(1, 1, 5).toBits().toBitString());
    Assert.assertEquals("0111", new Interval(2, 1, 5).toBits().toBitString());
    Assert.assertEquals("101", new Interval(3, 1, 5).toBits().toBitString());
    Assert.assertEquals("111", new Interval(4, 1, 5).toBits().toBitString());

    Assert.assertEquals("001", new Interval(0, 2, 5).toBits().toBitString());
    Assert.assertEquals("00011", new Interval(91, 80, 1368).toBits().toBitString());
  }
  
  private void testConcatenate(final long a, final long b)
  {
    final String asStringA = 0 == a ? "" : Long.toBinaryString(a);
    final String asStringB = 0 == b ? "" : Long.toBinaryString(b);
    final String asString = asStringA + asStringB;
    final Bits bitsA = new Bits(a);
    final Bits bitsB = new Bits(b);
    final Bits bits = bitsA.concatenate(bitsB);
    Assert.assertEquals(String.format("Concatenate %s and %s", a, b), asString,
        bits.toBitString());
  }
  
  @Test
  @Category(TestCategories.UnitTest.class)
  public void testFixedLength() throws JSONException, IOException
  {
    for (int i = 0; i < 1000; i++)
    {
      this.testFixedLength(this.randomLong());
    }
  }
  
  private void testFixedLength(final long value)
  {
    String asString = 0 == value ? "" : Long.toBinaryString(value);
    final Bits bits = new Bits(value, 64);
    while (asString.length() < 64)
    {
      asString = "0" + asString;
    }
    Assert.assertEquals("toLong for " + value, value, bits.toLong());
    Assert.assertEquals("toString for " + value, asString, bits.toBitString());
  }
  
  @Test
  @Category(TestCategories.UnitTest.class)
  public void testHardcoded() throws JSONException, IOException
  {
    Assert.assertEquals(new Bits(0), new Bits(0));
    Assert.assertEquals("", new Bits(0).toBitString());
    Assert.assertEquals("1", new Bits(1).toBitString());
    Assert.assertEquals("100", new Bits(4).toBitString());
    Assert.assertEquals("10001", new Bits(17).toBitString());
    Assert.assertEquals("100", new Bits(17).range(0, 3).toBitString());
    Assert.assertEquals("01", new Bits(17).range(3).toBitString());
    Assert.assertEquals("111", new Bits(7).toBitString());
    Assert.assertEquals("10111", new Bits(2).concatenate(new Bits(7))
        .toBitString());
    Assert.assertEquals("00110", new Bits(6l, 5).toBitString());
    Assert.assertEquals("111000000", new Bits(7l).leftShift(6).toBitString());
    Assert.assertEquals("1110", new Bits(7l).leftShift(6).range(0, 4)
        .toBitString());
    Assert.assertEquals("00000", new Bits(7l).leftShift(6).range(4)
        .toBitString());
    Assert.assertEquals("110", new Bits(6l).toBitString());
    Assert.assertEquals("11100", new Bits(7l).leftShift(2).toBitString());
    Assert.assertEquals("11000",
        new Bits(7l).leftShift(2).bitwiseAnd(new Bits(6l)).toBitString());
    Assert.assertEquals("11100",
        new Bits(7l).leftShift(2).bitwiseOr(new Bits(6l)).toBitString());
    Assert.assertEquals("00100",
        new Bits(7l).leftShift(2).bitwiseXor(new Bits(6l)).toBitString());
    Assert.assertEquals(2, new Bits(7l, 16).getBytes().length);
  }
  
  @Test
  @Category(TestCategories.UnitTest.class)
  public void testSubrange() throws JSONException, IOException
  {
    for (int i = 0; i < 1000; i++)
    {
      final long value = this.random.nextLong();
      final Bits bits = new Bits(value);
      this.testSubrange(bits);
    }
  }
  
  private void testSubrange(final Bits bits)
  {
    final String asString = bits.toBitString();
    for (int j = 0; j < 10; j++)
    {
      final int from = this.random.nextInt(asString.length());
      final int to = from + this.random.nextInt(asString.length() - from);
      this.testSubrange(bits, asString, from, to);
    }
  }
  
  private void testSubrange(final Bits bits, final String asString,
      final int from, final int to)
  {
    final String subStr = asString.substring(from, to);
    final Bits subBits = bits.range(from, to - from);
    Assert.assertEquals(
        String.format("Substring (%s,%s) of %s", from, to, bits), subStr,
        subBits.toBitString());
  }
  
  @Test
  @Category(TestCategories.UnitTest.class)
  public void testToString() throws JSONException, IOException
  {
    for (int i = 0; i < 1000; i++)
    {
      this.testToString(this.randomLong());
    }
  }
  
  private void testToString(final long value)
  {
    final String asString = 0 == value ? "" : Long.toBinaryString(value);
    final Bits bits = new Bits(value);
    Assert.assertEquals("toLong for " + value, value, bits.toLong());
    Assert.assertEquals("toString for " + value, asString, bits.toBitString());
  }
  
}