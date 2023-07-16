package io.github.stuff_stuffs.river_net_gen.geo;

public class GeoColumn<T> {
    private final T[] data;
    private final int[] heights;

    public GeoColumn(final T[] data, final int[] heights) {
        this.data = data;
        this.heights = heights;
        if (data.length != heights.length) {
            throw new IllegalArgumentException();
        }
    }

    public int length() {
        return data.length;
    }

    public T data(final int index) {
        return data[index];
    }

    public int height(final int index) {
        return heights[index];
    }
}
