package io.github.stuff_stuffs.river_net_gen.river.neighbour.base;

import io.github.stuff_stuffs.river_net_gen.river.neighbour.Neighbourhood;
import io.github.stuff_stuffs.river_net_gen.util.Hex;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

public abstract class NeighbourhoodWalker<R, State, Input, Context> {
    private final Class<R> resultClass;
    private final Class<State> stateClass;

    protected NeighbourhoodWalker(final Class<R> resultClass, final Class<State> stateClass) {
        this.resultClass = resultClass;
        this.stateClass = stateClass;
    }

    protected abstract @Nullable State start(int s, Neighbourhood<Input> neighbourhood, Context context);

    protected abstract @Nullable State process(int s, State[] states, Neighbourhood<Input> neighbourhood, Context context);

    protected abstract R finish(int s, State val, Neighbourhood<Input> neighbourhood, Context context);

    public NeighbourhoodWalker.Result<R> walk(final Neighbourhood<Input> neighbourhood, final Context context) {
        final State[] states = (State[]) Array.newInstance(stateClass, 7);
        int count = 0;
        for (int i = 0; i < 7; i++) {
            final State start = start(i, neighbourhood, context);
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
                final State val = process(j, states, neighbourhood, context);
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
        final R finishedCenter = finish(center, states[center], neighbourhood, context);
        final R finishedUp = finish(up, states[up], neighbourhood, context);
        final R finishedUpRight = finish(upRight, states[upRight], neighbourhood, context);
        final R finishedDownRight = finish(downRight, states[downRight], neighbourhood, context);
        final R finishedDown = finish(down, states[down], neighbourhood, context);
        final R finishedDownLeft = finish(downLeft, states[downLeft], neighbourhood, context);
        final R finishedUpLeft = finish(upLeft, states[upLeft], neighbourhood, context);
        final R[] raw = (R[]) Array.newInstance(resultClass, 7);
        raw[center] = finishedCenter;
        raw[up] = finishedUp;
        raw[upRight] = finishedUpRight;
        raw[downRight] = finishedDownRight;
        raw[down] = finishedDown;
        raw[downLeft] = finishedDownLeft;
        raw[upLeft] = finishedUpLeft;
        return new NeighbourhoodWalker.Result<>(
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
