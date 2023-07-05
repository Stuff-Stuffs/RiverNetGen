package io.github.stuff_stuffs.river_net_gen.api.neighbour.base;

import io.github.stuff_stuffs.river_net_gen.api.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

public abstract class NeighbourhoodWalker<T, S, R, C> {
    private final Class<T> resultClass;
    private final Class<S> stateClass;

    protected NeighbourhoodWalker(final Class<T> resultClass, final Class<S> stateClass) {
        this.resultClass = resultClass;
        this.stateClass = stateClass;
    }

    protected abstract @Nullable S start(int s, Neighbourhood<R> neighbourhood, C context);

    protected abstract @Nullable S process(int s, S[] states, Neighbourhood<R> neighbourhood, C context);

    protected abstract T finish(int s, S val, Neighbourhood<R> neighbourhood, C context);

    public Result<T> walk(final Neighbourhood<R> neighbourhood, final C context) {
        final S[] states = (S[]) Array.newInstance(stateClass, 7);
        int count = 0;
        for (int i = 0; i < 7; i++) {
            final S start = start(i, neighbourhood, context);
            if (start != null) {
                count++;
            }
            states[i] = start;
        }
        if (count == 0) {
            throw new RuntimeException();
        }
        outer:
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                if (states[j] != null) {
                    continue;
                }
                final S val = process(j, states, neighbourhood, context);
                if (val != null) {
                    states[j] = val;
                    count++;
                    if (count == 7) {
                        break outer;
                    }
                }
            }
        }
        final int center = neighbourhood.center();
        final int up = neighbourhood.offset(center, Hex.Direction.UP);
        final int upRight = neighbourhood.offset(center, Hex.Direction.UP_RIGHT);
        final int downRight = neighbourhood.offset(center, Hex.Direction.DOWN_RIGHT);
        final int down = neighbourhood.offset(center, Hex.Direction.DOWN);
        final int downLeft = neighbourhood.offset(center, Hex.Direction.DOWN_LEFT);
        final int upLeft = neighbourhood.offset(center, Hex.Direction.UP_LEFT);
        final T finishedCenter = finish(center, states[center], neighbourhood, context);
        final T finishedUp = finish(up, states[up], neighbourhood, context);
        final T finishedUpRight = finish(upRight, states[upRight], neighbourhood, context);
        final T finishedDownRight = finish(downRight, states[downRight], neighbourhood, context);
        final T finishedDown = finish(down, states[down], neighbourhood, context);
        final T finishedDownLeft = finish(downLeft, states[downLeft], neighbourhood, context);
        final T finishedUpLeft = finish(upLeft, states[upLeft], neighbourhood, context);
        final T[] raw = (T[]) Array.newInstance(resultClass, 7);
        raw[center] = finishedCenter;
        raw[up] = finishedUp;
        raw[upRight] = finishedUpRight;
        raw[downRight] = finishedDownRight;
        raw[down] = finishedDown;
        raw[downLeft] = finishedDownLeft;
        raw[upLeft] = finishedUpLeft;
        return new Result<>(
                finishedCenter,
                finishedUp,
                finishedUpRight,
                finishedDownRight,
                finishedDown,
                finishedDownLeft,
                finishedUpLeft,
                raw
        );
    }


    public record Result<T>(T center, T up, T upRight, T downRight, T down, T downLeft, T upLeft, T[] raw) {
        public T get(final Hex.@Nullable Direction direction) {
            if (direction == null) {
                return center;
            }
            return switch (direction) {
                case UP -> up;
                case UP_RIGHT -> upRight;
                case DOWN_RIGHT -> downRight;
                case DOWN -> down;
                case DOWN_LEFT -> downLeft;
                case UP_LEFT -> upLeft;
            };
        }
    }
}
