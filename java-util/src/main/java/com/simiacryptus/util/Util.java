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

import com.simiacryptus.util.io.BinaryChunkIterator;
import com.simiacryptus.util.test.LabeledObject;
import com.simiacryptus.util.io.TeeInputStream;
import org.apache.commons.io.output.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.net.ssl.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

public class Util {
  
  public static final ThreadLocal<Random> R = new ThreadLocal<Random>() {
    public final Random r = new Random(System.nanoTime());
    
    @Override
    protected Random initialValue() {
      return new Random(this.r.nextLong());
    }
  };
  private final static java.util.concurrent.atomic.AtomicInteger idcounter = new java.util.concurrent.atomic.AtomicInteger(0);
  private final static String jvmId = UUID.randomUUID().toString();
  
  public static void add(final DoubleSupplier f, final double[] data) {
    for (int i = 0; i < data.length; i++) {
      data[i] += f.getAsDouble();
    }
  }
  
  public static Stream<byte[]> binaryStream(final String path, final String name, final int skip, final int recordSize) throws IOException {
    File file = new File(path, name);
    byte[] fileData = org.apache.commons.io.IOUtils.toByteArray(new java.io.BufferedInputStream(new GZIPInputStream(new java.io.BufferedInputStream(new FileInputStream(file)))));
    final DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(fileData));
    in.skip(skip);
    return Util.toIterator(new BinaryChunkIterator(in, recordSize));
  }
  
  public static String[] currentStack() {
    return java.util.stream.Stream.of(Thread.currentThread().getStackTrace()).map(Object::toString).toArray(i -> new String[i]);
  }
  
  public static byte[] read(final DataInputStream i, final int s) throws IOException {
    final byte[] b = new byte[s];
    int pos = 0;
    while (b.length > pos) {
      final int read = i.read(b, pos, b.length - pos);
      if (0 == read)
        throw new RuntimeException();
      pos += read;
    }
    return b;
  }
  
  public static void report(final Stream<String> fragments) throws FileNotFoundException, IOException {
    final File outDir = new File("reports");
    outDir.mkdirs();
    final StackTraceElement caller = getLast(Arrays.stream(Thread.currentThread().getStackTrace())//
                                                 .filter(x -> x.getClassName().contains("simiacryptus")));
    final File report = new File(outDir, caller.getClassName() + "_" + caller.getLineNumber() + ".html");
    final PrintStream out = new PrintStream(new FileOutputStream(report));
    out.println("<html><head></head><body>");
    fragments.forEach(out::println);
    out.println("</body></html>");
    out.close();
    Desktop.getDesktop().browse(report.toURI());
  }
  
  public static <T> T getLast(Stream<T> stream) {
    List<T> collect = stream.collect(Collectors.toList());
    T last = collect.get(collect.size() - 1);
    return last;
  }
  
  public static void report(final String... fragments) throws FileNotFoundException, IOException {
    Util.report(Stream.of(fragments));
  }
  
  public static String toInlineImage(final BufferedImage img, final String alt) {
    return Util.toInlineImage(new LabeledObject<BufferedImage>(img, alt));
  }
  
  public static String toInlineImage(final LabeledObject<BufferedImage> img) {
    final ByteArrayOutputStream b = new ByteArrayOutputStream();
    try {
      ImageIO.write(img.data, "PNG", b);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
    final byte[] byteArray = b.toByteArray();
    final String encode = Base64.getEncoder().encodeToString(byteArray);
    return "<img src=\"data:image/png;base64," + encode + "\" alt=\"" + img.label + "\" />";
  }
  
  public static <T> Stream<T> toIterator(final Iterator<T> iterator) {
    return StreamSupport.stream(Spliterators.spliterator(iterator, 1, Spliterator.ORDERED), false);
  }
  
  public static <T> Stream<T> toStream(final Iterator<T> iterator) {
    return Util.toStream(iterator, 0);
  }
  
  public static <T> Stream<T> toStream(final Iterator<T> iterator, final int size) {
    return Util.toStream(iterator, size, false);
  }
  
  public static <T> Stream<T> toStream(final Iterator<T> iterator, final int size, final boolean parallel) {
    return StreamSupport.stream(Spliterators.spliterator(iterator, size, Spliterator.ORDERED), parallel);
  }
  
  public static UUID uuid() {
    String index = Integer.toHexString(idcounter.incrementAndGet());
    while (index.length() < 8) {
      index = "0" + index;
    }
    final String tempId = jvmId.substring(0, jvmId.length() - index.length()) + index;
    return UUID.fromString(tempId);
  }
  
  public static TemporalUnit cvt(TimeUnit units) {
    switch (units) {
      case DAYS:
        return ChronoUnit.DAYS;
      case HOURS:
        return ChronoUnit.HOURS;
      case MINUTES:
        return ChronoUnit.MINUTES;
      case SECONDS:
        return ChronoUnit.SECONDS;
      case NANOSECONDS:
        return ChronoUnit.NANOS;
      case MICROSECONDS:
        return ChronoUnit.MICROS;
      case MILLISECONDS:
        return ChronoUnit.MILLIS;
      default:
        throw new IllegalArgumentException(units.toString());
    }
  }
  
  public static BufferedImage resize(BufferedImage image) {
    int width = image.getWidth();
    if (width < 800) return image;
    int height = image.getHeight() * width / image.getWidth();
    BufferedImage rerender = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics gfx = rerender.getGraphics();
    RenderingHints hints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    ((Graphics2D) gfx).setRenderingHints(hints);
    gfx.drawImage(image, 0, 0, rerender.getWidth(), rerender.getHeight(), null);
    return rerender;
  }
  
  public static String pathTo(File from, File to) {
    Path fromUrl = from.toPath();
    Path toUrl = to.toPath();
    return fromUrl.relativize(toUrl).toString().replaceAll("\\\\", "/");
  }
  
  public static String mkString(String separator, String... strs) {
    return Arrays.asList(strs).stream().collect(Collectors.joining(separator));
  }
  
  public static void layout(Component c) {
    c.doLayout();
    if (c instanceof Container) {
      Arrays.stream(((Container) c).getComponents()).forEach(Util::layout);
    }
  }
  
  public static BufferedImage toImage(Component component) {
    layout(component);
    BufferedImage img = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
    Graphics2D g = img.createGraphics();
    g.setColor(component.getForeground());
    g.setFont(component.getFont());
    component.print(g);
    return img;
  }
  
  public static InputStream cache(URI url) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    return cache(url.toString(), new File(url.getPath()).getName());
  }
  
  public static InputStream cache(String url, String file) throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
    if (new File(file).exists()) {
      return new FileInputStream(file);
    } else {
      TrustManager[] trustManagers = new TrustManager[]{
          new X509TrustManager() {
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
              return new X509Certificate[0];
            }
            
            public void checkClientTrusted(
                                              X509Certificate[] certs, String authType) {
            }
            
            public void checkServerTrusted(
                                              X509Certificate[] certs, String authType) {
            }
          }
      };
      SSLContext ctx = SSLContext.getInstance("TLS");
      ctx.init(null, trustManagers, null);
      SSLSocketFactory sslFactory = ctx.getSocketFactory();
      URLConnection urlConnection = new URL(url).openConnection();
      if (urlConnection instanceof javax.net.ssl.HttpsURLConnection) {
        HttpsURLConnection conn = (HttpsURLConnection) urlConnection;
        conn.setSSLSocketFactory(sslFactory);
        conn.setRequestMethod("GET");
      }
      InputStream inputStream = urlConnection.getInputStream();
      FileOutputStream cache = new FileOutputStream(file);
      return new TeeInputStream(inputStream, cache);
    }
  }
}