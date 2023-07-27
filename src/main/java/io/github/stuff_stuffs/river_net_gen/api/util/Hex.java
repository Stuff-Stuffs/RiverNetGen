package io.github.stuff_stuffs.river_net_gen.api.util;

import it.unimi.dsi.fastutil.HashCommon;

public final class Hex {
    private static final double SQRT3 = Math.sqrt(3);
    private static final double SQRT3_2 = Math.sqrt(3) * 0.5;
    private static final double SQRT3_3 = Math.sqrt(3) / 3.0;

    public static Coordinate fromCartesian(final double x, final double y) {
        final double qd = (2 / 3.0) * x;
        final double rd = (-1 / 3.0) * x + SQRT3_3 * y;
        return round(qd, rd);
    }

    public static Coordinate round(final double qd, final double rd) {
        final double sd = -(qd + rd);
        int q = (int) Math.round(qd);
        int r = (int) Math.round(rd);
        final int s = (int) Math.round(sd);

        final double dq = Math.abs(q - qd);
        final double dr = Math.abs(r - rd);
        final double ds = Math.abs(s - sd);
        if (dq > dr && dq > ds) {
            q = -r - s;
        } else if (dr > ds) {
            r = -q - s;
        }
        return new Coordinate(q, r);
    }

    public record Coordinate(int q, int r) {
        public double x() {
            return 1.5 * q;
        }

        public double y() {
            return SQRT3_2 * q + SQRT3 * r;
        }

        public Coordinate offset(final Direction direction) {
            return new Coordinate(q + direction.qOff, r + direction.rOff);
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Coordinate that)) {
                return false;
            }

            if (q != that.q) {
                return false;
            }
            return r == that.r;
        }

        @Override
        public int hashCode() {
            return HashCommon.mix(q) ^ HashCommon.murmurHash3(r);
        }
    }

    public enum Direction {
        UP(0, -1),
        UP_RIGHT(1, -1),
        DOWN_RIGHT(1, 0),
        DOWN(0, 1),
        DOWN_LEFT(-1, 1),
        UP_LEFT(-1, 0);
        public final int qOff;
        public final int rOff;

        Direction(final int qOff, final int rOff) {
            this.qOff = qOff;
            this.rOff = rOff;
        }

        public Direction opposite() {
            return switch (this) {
                case UP -> DOWN;
                case UP_RIGHT -> DOWN_LEFT;
                case DOWN_RIGHT -> UP_LEFT;
                case DOWN -> UP;
                case DOWN_LEFT -> UP_RIGHT;
                case UP_LEFT -> DOWN_RIGHT;
            };
        }

        public Direction rotateCC() {
            return switch (this) {
                case UP -> UP_LEFT;
                case UP_RIGHT -> UP;
                case DOWN_RIGHT -> UP_RIGHT;
                case DOWN -> DOWN_RIGHT;
                case DOWN_LEFT -> DOWN;
                case UP_LEFT -> DOWN_LEFT;
            };
        }

        public Direction rotateC() {
            return switch (this) {
                case UP -> UP_RIGHT;
                case UP_RIGHT -> DOWN_RIGHT;
                case DOWN_RIGHT -> DOWN;
                case DOWN -> DOWN_LEFT;
                case DOWN_LEFT -> UP_LEFT;
                case UP_LEFT -> UP;
            };
        }

        public static Direction fromOffset(final int q, final int r) {
            if (q == 0 && r != 0) {
                return r > 0 ? DOWN : UP;
            }
            if (q != 0 && r == 0) {
                return q > 0 ? DOWN_RIGHT : UP_LEFT;
            }
            if (q == -r) {
                return q > 0 ? UP_RIGHT : DOWN_LEFT;
            }
            throw new IllegalArgumentException();
        }
    }

    private Hex() {
    }
}
