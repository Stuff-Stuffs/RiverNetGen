package io.github.stuff_stuffs.river_net_gen.geo;

public class GeoColumn {
    private final int[] data;
    private final int[] thickness;
    private final int[] heights;

    public GeoColumn(final int[] data, final int[] thickness) {
        this.data = data;
        this.thickness = thickness;
        heights = new int[thickness.length];
        int sum = 0;
        for (int i = 0; i < thickness.length; i++) {
            heights[i] = sum;
            sum = sum + thickness[i];
        }
        if (data.length != thickness.length) {
            throw new IllegalArgumentException();
        }
    }

    public int length() {
        return data.length;
    }

    public int data(final int index) {
        return data[index];
    }

    public int thickness(final int index) {
        return thickness[index];
    }

    public int height(final int index) {
        return heights[index];
    }
}
