package com.simiacryptus.util.text;

import com.simiacryptus.util.data.SerialArrayList;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class IndexNode extends TrieNode {

  public IndexNode(CharTrie charTrieIndex, short depth, int index, TrieNode parent) {
    super(charTrieIndex, depth, index, parent);
  }

  public Map<String, List<Cursor>> getCursorsByDocument() {
    return this.getCursors().collect(Collectors.groupingBy((Cursor x) -> x.getDocument()));
  }

  public Stream<Cursor> getCursors() {
    return IntStream.range(0, getData().cursorCount).mapToObj(i -> {
      return new Cursor((CharTrieIndex)this.trie, ((CharTrieIndex)this.trie).cursors.get(i + getData().firstCursorIndex), depth);
    });
  }

  public TrieNode split() {
    if (getData().firstChildIndex < 0) {
      Map<Character, SerialArrayList<CursorData>> sortedChildren = getCursors().parallel()
          .collect(Collectors.groupingBy(y -> y.next().getToken(),
              Collectors.reducing(new SerialArrayList<>(CursorType.INSTANCE, 0),
                  cursor -> new SerialArrayList<>(CursorType.INSTANCE, cursor.data),
                  (left, right) -> left.add(right))));
      int cursorWriteIndex = getData().firstCursorIndex;
      ArrayList<NodeData> childNodes = new ArrayList<>(sortedChildren.size());
      List<Map.Entry<Character, SerialArrayList<CursorData>>> collect = sortedChildren.entrySet().stream()
          .sorted(Comparator.comparing(e -> e.getKey())).collect(Collectors.toList());
      for (Map.Entry<Character, SerialArrayList<CursorData>> e : collect) {
        int length = e.getValue().length();
        ((CharTrieIndex)this.trie).cursors.putAll(e.getValue(), cursorWriteIndex);
        childNodes.add(new NodeData(e.getKey(), (short) -1, -1, length, cursorWriteIndex));
        cursorWriteIndex += length;
      }
      this.trie.nodes.update(index, data -> data
          .setFirstChildIndex(this.trie.nodes.addAll(childNodes))
          .setNumberOfChildren((short) childNodes.size())
          );
      return new IndexNode(this.trie, depth, index, parent);
    } else {
      return this;
    }
  }

  @Override
  public IndexNode godparent() {
    return (IndexNode) super.godparent();
  }

  @Override
  public IndexNode refresh() {
    return (IndexNode) super.refresh();
  }

  public IndexNode visitFirstIndex(Consumer<? super IndexNode> visitor) {
    visitor.accept(this);
    IndexNode refresh = refresh();
    refresh.getChildren().forEach(n -> n.visitFirstIndex(visitor));
    return refresh;
  }

  public IndexNode visitLastIndex(Consumer<? super IndexNode> visitor) {
    getChildren().forEach(n -> n.visitLastIndex(visitor));
    visitor.accept(this);
    return refresh();
  }

  @Override
  public Stream<? extends IndexNode> getChildren() {
    if (getData().firstChildIndex >= 0) {
      return IntStream.range(0, getData().numberOfChildren)
          .mapToObj(i -> new IndexNode(this.trie, (short) (depth + 1), getData().firstChildIndex + i, this));
    } else {
      return Stream.empty();
    }
  }

  @Override
  public Optional<? extends IndexNode> getChild(char token) {
    return super.getChild(token).map(x->(IndexNode)x);
  }

  @Override
  public IndexNode traverse(String str) {
    return (IndexNode) super.traverse(str);
  }

  @Override
  public IndexNode traverse(long cursorId) {
    return (IndexNode) super.traverse(cursorId);
  }
}