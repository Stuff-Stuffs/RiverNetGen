package io.github.stuff_stuffs.river_net_gen.geo;

public class GeoColumn<T> {
    private final T[] data;
    private final int[] heights;

    public GeoColumn(final T[] data, final int[] heights) {
        this.data = data;
        this.heights = new int[heights.length];
        int sum = 0;
        for (int i = 0; i < heights.length; i++) {
            this.heights[i] = sum;
            sum = sum + heights[i];
        }
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
