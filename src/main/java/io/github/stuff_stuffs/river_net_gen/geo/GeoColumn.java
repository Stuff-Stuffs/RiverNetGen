package io.github.stuff_stuffs.river_net_gen.geo;

public class GeoColumn {
    private final int[] data;
    private final int[] thicknesses;
    private final int[] heights;

    public GeoColumn(final int[] data, final int[] thicknesses) {
        this.data = data;
        this.thicknesses = thicknesses;
        heights = new int[thicknesses.length];
        int sum = 0;
        for (int i = 0; i < thicknesses.length; i++) {
            heights[i] = sum;
            sum = sum + thicknesses[i];
        }
        if (data.length != thicknesses.length) {
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
        return thicknesses[index];
    }

    public int height(final int index) {
        return heights[index];
    }
}
