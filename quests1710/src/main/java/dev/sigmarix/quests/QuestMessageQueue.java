package dev.sigmarix.quests;

import java.util.ArrayDeque;
import java.util.function.ToIntFunction;

/** Bound both message count and retained JSON characters, including large books. */
public final class QuestMessageQueue<T> {
    private final ArrayDeque<T> queue=new ArrayDeque<T>();
    private final int capacity;
    private final long maxCharacters;
    private final ToIntFunction<T> size;
    private long characters;
    public QuestMessageQueue(int capacity,long maxCharacters,ToIntFunction<T> size) {
        this.capacity=capacity;this.maxCharacters=maxCharacters;this.size=size;
    }
    public synchronized boolean offer(T value) {
        int count=size.applyAsInt(value);
        if(count<0 || queue.size()>=capacity || count>maxCharacters-characters) return false;
        queue.add(value);characters+=count;return true;
    }
    public synchronized T poll() {
        T value=queue.poll();if(value!=null) characters-=size.applyAsInt(value);return value;
    }
    public synchronized void clear() { queue.clear();characters=0; }
}
