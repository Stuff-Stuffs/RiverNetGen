package io.github.stuff_stuffs.river_net_gen.geo;

public class GeoColumnInterpolator2d<T> {
    private static final int MAX_SLOPE = 64;
    private final GeoColumn<T> first;
    private final GeoColumn<T> second;
    private final MatchResult result;

    public GeoColumnInterpolator2d(final GeoColumn<T> first, final GeoColumn<T> second) {
        this.first = first;
        this.second = second;
        result = match(first, second);
    }

    public T interpolate(final double x, final double y) {
        final double[] slopes = result.slopes;
        final double[] heights = result.heights;
        int match = -2;
        for (int i = 0; i < slopes.length; i++) {
            if (x * slopes[i] + heights[i]>= y) {
                match = i - 1;
                break;
            }
        }
        if (match == -2) {
            if (first.length() > second.length()) {
                return first.data(first.length() - 1);
            } else {
                return second.data(second.length() - 1);
            }
        } else if (match == -1) {
            return first.data(0);
        } else {
            if (match < first.length()) {
                return first.data(match);
            } else {
                return second.data(match);
            }
        }
    }

    private static MatchResult match(final GeoColumn<?> first, final GeoColumn<?> second) {
        final int fLength = first.length();
        final int count = Math.max(fLength, second.length());
        final int[] fMatches = new int[count];
        final double[] fSlopes = new double[count];
        final double[] heights = new double[count];
        double lastSlope = 0;
        int last = 0;
        int lastHeight = 0;
        int firstHeight = 0;
        int prevSuccess = 0;
        int prevSuccessHeight = 0;
        for (int i = 0; i < fLength; i++) {
            int matched = -1;
            final Object data = first.data(i);
            int s = lastHeight;
            for (int j = last; j < fLength && (firstHeight >= s || s - firstHeight < MAX_SLOPE); j++) {
                if (second.data(j).equals(data)) {
                    matched = j;
                    break;
                }
                s = s + second.height(j);
            }
            if (matched == -1) {
                continue;
            }
            final double slope = firstHeight - s;
            if (prevSuccess + 1 < i) {
                int s0 = prevSuccessHeight;
                for (int j = prevSuccess + 1; j < i; j++) {
                    final double alpha = (j - prevSuccess) / (double) (i - prevSuccess);
                    heights[j] = s0;
                    fMatches[j] = -1;
                    fSlopes[j] = lastSlope + alpha * alpha * (slope - lastSlope);
                    s0 = s0 + first.height(j);
                }
            }
            heights[i] = firstHeight;
            fMatches[i] = matched;
            fSlopes[i] = slope;
            lastSlope = slope;
            last = matched + 1;
            lastHeight = s;
            prevSuccessHeight = firstHeight;
            firstHeight = firstHeight + first.height(i);
            prevSuccess = i;
        }
        for (int i = prevSuccess; i < count; i++) {
            fMatches[i] = -1;
            fSlopes[i] = lastSlope;
            if (i < fLength) {
                heights[i] = lastHeight;
                lastHeight = lastHeight + first.height(i);
            } else {
                heights[i] = lastHeight + lastSlope;
                lastHeight = lastHeight + second.height(i);
            }
        }
        return new MatchResult(fMatches, fSlopes, heights);
    }

    private record MatchResult(int[] matches, double[] slopes, double[] heights) {
    }
}
