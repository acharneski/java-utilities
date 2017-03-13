package com.simiacryptus.util.test;

import org.apache.commons.compress.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Shakespeare extends TestDocument {
    public static String url = "http://www.gutenberg.org/cache/epub/100/pg100.txt";
    public static String file = "Shakespeare.txt";
    private static final ArrayList<Shakespeare> queue = new ArrayList<>();
    private static volatile Thread thread;

    public static void clear() throws InterruptedException {
        if (thread != null) {
            synchronized (WikiArticle.class) {
                if (thread != null) {
                    thread.interrupt();
                    thread.join();
                    thread = null;
                    queue.clear();
                }
            }
        }
    }
    public static Stream<Shakespeare> load() {
        if (thread == null) {
            synchronized (WikiArticle.class) {
                if (thread == null) {
                    thread = new Thread(Shakespeare::read);
                    thread.setDaemon(true);
                    thread.start();
                }
            }
        }
        Iterator<Shakespeare> iterator = new AsyncListIterator<>(queue, thread);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT), false).filter(x -> x != null);
    }

    private static void read() {
        try {
            InputStream in = Spool.load(url, file);
            String txt = new String(IOUtils.toByteArray(in), "UTF-8").replaceAll("\r", "");
            for(String paragraph : txt.split("\n\\s*\n")) {
                    queue.add(new Shakespeare(paragraph));
            }
        } catch (final IOException e) {
            // Ignore... end of stream
        } catch (final RuntimeException e) {
            if(!(e.getCause() instanceof InterruptedException)) throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Shakespeare(String text) {
        super(text, text);
    }
}