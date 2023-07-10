package io.github.stuff_stuffs.river_net_gen.api.neighbour.base;

import io.github.stuff_stuffs.river_net_gen.api.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;

public abstract class NeighbourWeightedWalker<Result, PartialResult, Input, Context> {
    private static final Hex.Direction[] DIRECTIONS = Hex.Direction.values();

    protected abstract Result finish(PartialResult partialResult, Neighbourhood<Input> neighbourhood, Context context, int seed);

    protected abstract PartialResult connect(int from, int to, Hex.Direction outgoing, PartialResult partialResult, Neighbourhood<Input> neighbourhood, Context context, int seed);

    protected abstract PartialResult init(int start, Neighbourhood<Input> neighbourhood, Context context, int seed);

    protected abstract int start(Neighbourhood<Input> neighbourhood, Context context, int seed);

    protected abstract double weight(int from, int to, Hex.Direction fromDirection, PartialResult partialResult, Neighbourhood<Input> neighbourhood, Context context, int seed);

    public Result walk(final Neighbourhood<Input> neighbourhood, final Context context, final int seed) {
        final Hex.Direction[] prev = new Hex.Direction[7];
        final double[] weights = new double[]{Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN};
        final boolean[] visited = new boolean[7];
        final int start = start(neighbourhood, context, seed);
        weights[start] = 0.0;
        int count = 0;
        PartialResult partialResult = init(start, neighbourhood, context, seed);
        while (count < 7) {
            double selectedWeight = Double.POSITIVE_INFINITY;
            int selected = -1;
            for (int i = 0; i < 7; i++) {
                if (!visited[i] & weights[i] < selectedWeight) {
                    selectedWeight = weights[i];
                    selected = i;
                }
            }
            if (selected == -1) {
                throw new IllegalStateException();
            }
            final Hex.Direction previous = prev[selected];
            if (previous != null) {
                partialResult = connect(neighbourhood.offset(selected, previous), selected, previous, partialResult, neighbourhood, context, seed);
            }
            visited[selected] = true;
            count++;
            if (count < 7) {
                for (final Hex.Direction direction : DIRECTIONS) {
                    final int n = neighbourhood.offset(selected, direction);
                    if (!neighbourhood.inCenterCluster(n) || visited[n]) {
                        continue;
                    }
                    final double weight = selectedWeight + weight(selected, n, direction, partialResult, neighbourhood, context, seed);
                    if (!(weights[n] < weight)) {
                        weights[n] = weight;
                        prev[n] = direction.opposite();
                    }
                }
            }
        }
        return finish(partialResult, neighbourhood, context, seed);
    }
}
