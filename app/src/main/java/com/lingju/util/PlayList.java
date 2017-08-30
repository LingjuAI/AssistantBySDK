package com.lingju.util;

import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Random;


public class PlayList<E> extends ArrayList<E> {

    /**
     *
     */
    private static final long serialVersionUID = -6989206326150411407L;

    public interface PlayMode {
        public final static int ORDER = 0;
        public final static int RANDOM = 1;
        public final static int SINGLE = 2;
    }

    public final static String PLAY_MODE = "play_mode";
    private RandomIterator rIterator;
    private final OrderIterator oIterator = new OrderIterator();
    private int playMode;

    public PlayList() {
        super();
    }

    public PlayList(int capacity) {
        super(capacity);
    }

    public PlayList(Collection<? extends E> collection) {
        super(collection);
        resetIterator();
    }

    public void setPlayMode(int playMode) {
        if (playMode >= 0 && playMode < 3) {
            this.playMode = playMode;
        }
    }

    @Override
    public boolean add(E object) {
        clearIterator();
        return super.add(object);
    }

    @Override
    public void add(int index, E object) {
        clearIterator();
        super.add(index, object);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        clearIterator();
        if (super.addAll(collection)) {
            resetIterator();
            return true;
        }
        return false;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        clearIterator();
        if (super.addAll(index, collection)) {
            resetIterator();
            return true;
        }
        return false;
    }

    public int getPlayMode() {
        return playMode;
    }

    private void clearIterator() {
        rIterator = null;
        oIterator.currentIndex = 0;
        oIterator.size = size();
    }

    public void resetIterator() {
        oIterator.currentIndex = 0;
        oIterator.size = size();
        if (rIterator != null && rIterator.size == size())
            return;
        rIterator = new RandomIterator();
    }

    public E getCurrent() {
        if (rIterator == null)
            return null;
        switch (playMode) {
            case PlayMode.ORDER:
            case PlayMode.SINGLE:
                return oIterator.current();
            case PlayMode.RANDOM:
                oIterator.setCurrentIndex(rIterator.rIndexs[rIterator.currentIndex]);
                return rIterator.current();
        }
        return null;
    }

    public int getCurrentOrderIndex() {
        if (rIterator == null)
            return -1;
        switch (playMode) {
            case PlayMode.ORDER:
            case PlayMode.SINGLE:
                return oIterator.currentIndex;
            case PlayMode.RANDOM:
                // return rIterator.currentIndex;
                return oIterator.currentIndex;
        }
        return -1;
    }

    public E getNext() {
        if (rIterator == null)
            return null;
        switch (playMode) {
            case PlayMode.ORDER:
                oIterator.next();
                rIterator.setCurrentIndex(oIterator.currentIndex);
                return oIterator.current();
            case PlayMode.RANDOM:
                rIterator.next();
                oIterator.setCurrentIndex(rIterator.rIndexs[rIterator.currentIndex]);
                return rIterator.current();
            case PlayMode.SINGLE:
                return oIterator.current();
        }
        return null;
    }

    /**
     * 获取index位置的元素，并将迭代器的当前索引置为index
     *
     * @param index
     * @return
     */
    public E getAndMark(int index) {
        if (index >= size())
            return null;
        oIterator.currentIndex = index;
        rIterator.setCurrentIndex(oIterator.currentIndex);
        return super.get(index);
    }

    public E getPre() {
        if (rIterator == null)
            return null;
        switch (playMode) {
            case PlayMode.ORDER:
                oIterator.pre();
                rIterator.setCurrentIndex(oIterator.currentIndex);
                return oIterator.current();
            case PlayMode.RANDOM:
                rIterator.pre();
                oIterator.setCurrentIndex(rIterator.rIndexs[rIterator.currentIndex]);
                return rIterator.current();
            case PlayMode.SINGLE:
                return oIterator.current();
        }
        return null;
    }


    @Override
    public Iterator<E> iterator() {
        return super.iterator();
    }

    private class RandomIterator {
        private int[] rIndexs;
        private final int size;
        private int currentIndex;

        public RandomIterator() {
            size = PlayList.this.size();
            if (size == 0) {
                rIndexs = new int[0];
                return;
            }
            rIndexs = new int[size];
            for (int i = 0; i < size; i++) {
                rIndexs[i] = i;
            }
            Random rd = new Random(System.currentTimeMillis());
            for (int i = 0; i < size; i += 2) {
                swap(rIndexs, i, rd.nextInt(size));
            }
        }

        private void swap(int[] is, int first, int second) {
            if (first == second)
                first = size - 1;
            is[first] = is[first] ^ is[second];
            is[second] = is[first] ^ is[second];
            is[first] = is[first] ^ is[second];
        }

        void setCurrentIndex(int index) {
            if (index >= 0 && index < size) {
                for (int i = 0; i < size; i++) {
                    if (rIndexs[i] == index) {
                        this.currentIndex = i;
                        break;
                    }
                }
            }
        }

        public int currentIndex() {
            return currentIndex;
        }

        public E current() {
            return PlayList.this.get(rIndexs[currentIndex]);
        }

        public boolean isEnd() {
            return currentIndex == (size - 1);
        }

        public boolean hasNext() {
            return size > 0;
        }

        public E next() {
            return PlayList.this.get(rIndexs[currentIndex = (currentIndex + 1) % size]);
        }

        public E pre() {
            return PlayList.this.get(rIndexs[currentIndex = (size + currentIndex - 1) % size]);
        }
    }

    private class OrderIterator {
        private int size;
        private int currentIndex;

        public OrderIterator() {
            size = PlayList.this.size();
        }

        void setCurrentIndex(int index) {
            if (index >= 0 && index < size)
                this.currentIndex = index;
        }

        public int currentIndex() {
            return currentIndex;
        }

        public E current() {
            return PlayList.this.get(currentIndex);
        }

        public boolean isEnd() {
            return currentIndex == (size - 1);
        }

        public boolean hasNext() {
            return size > 0;
        }

        public E next() {
            return PlayList.this.get(currentIndex = (currentIndex + 1) % size);
        }

        public E pre() {
            return PlayList.this.get(currentIndex = (size + currentIndex - 1) % size);
        }
    }


}
