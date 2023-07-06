package io.github.stuff_stuffs.river_net_gen.impl.util;

import io.github.stuff_stuffs.river_net_gen.api.util.Hex;
import io.github.stuff_stuffs.river_net_gen.api.util.SHM;
import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;

import java.util.Arrays;

public final class SHMImpl implements SHM {
    public static final int MAX_LEVEL = 16;
    private static final byte[] ENCODE = new byte[]{0, 5, 1, 6, 3, 4, 2};
    private static final Hex.Coordinate[] DECODE;
    static final byte[] ADDITION_TABLE;

    static {
        DECODE = new Hex.Coordinate[7];
        DECODE[0] = new Hex.Coordinate(0, 0);
        final int[] code = new int[]{0, -1, 1};
        for (int i = 1; i < 7; i++) {
            DECODE[i] = new Hex.Coordinate(code[0], code[1]);
            rotate(code);
        }
        final byte[][] BASE_7_TABLE = {{0, 1, 2, 3, 4, 5, 6}, {1, 63, 15, 2, 0, 6, 64}, {2, 15, 14, 26, 3, 0, 1}, {3, 2, 26, 25, 31, 4, 0}, {4, 0, 3, 31, 36, 42, 5}, {5, 6, 0, 4, 42, 41, 53}, {6, 64, 1, 0, 5, 53, 52}};
        ADDITION_TABLE = new byte[49];
        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 7; j++) {
                final byte res = BASE_7_TABLE[i][j];
                if (res < 7) {
                    ADDITION_TABLE[i * 7 + j] = res;
                } else {
                    final int sum = res % 10;
                    final int carry = res / 10;
                    ADDITION_TABLE[i * 7 + j] = (byte) (sum | (carry << 3));
                }
            }
        }
    }

    private static void rotate(final int[] code) {
        final int c0 = code[0];
        final int c1 = code[1];
        final int c2 = code[2];

        code[0] = -c2;
        code[1] = -c0;
        code[2] = -c1;
    }

    private final int defaultLevel;
    private final byte[] buffer0 = new byte[MAX_LEVEL];
    private final byte[] buffer1 = new byte[MAX_LEVEL];

    public static byte idFromDirection(final Hex.Direction direction) {
        return switch (direction) {
            case UP -> 5;
            case UP_RIGHT -> 6;
            case DOWN_RIGHT -> 1;
            case DOWN -> 2;
            case DOWN_LEFT -> 3;
            case UP_LEFT -> 4;
        };
    }

    public static Coordinate offset(final Hex.Direction direction, final int level) {
        final byte[] data = new byte[level + 1];
        data[data.length - 1] = idFromDirection(direction);
        return new SHMImpl.CoordinateImpl(data);
    }

    public static Coordinate shift(final Coordinate coordinate, final int shift) {
        if (shift == 0 || coordinate.level() == 0) {
            return coordinate;
        }
        final CoordinateImpl impl = (CoordinateImpl) coordinate;
        final byte[] data = new byte[impl.level() + shift];
        System.arraycopy(impl.data, 0, data, shift, impl.level());
        return new CoordinateImpl(data);
    }

    @Override
    public CoordinateImpl fromHex(final Hex.Coordinate coordinate) {
        return fromHex(coordinate, defaultLevel);
    }

    @Override
    public CoordinateImpl fromHex(final Hex.Coordinate coordinate, final int level) {
        int q = coordinate.q();
        int r = coordinate.r();
        int l = 0;
        int lastNonZero = 0;
        while ((q | r) != 0 && l < level) {
            final int proj = (q - 2 * r) % 7;
            final byte d = ENCODE[proj < 0 ? proj + 7 : proj];
            buffer0[l++] = d;
            if (d != 0) {
                lastNonZero = l;
            }
            final Hex.Coordinate dir = DECODE[d];
            q = q - dir.q();
            r = r - dir.r();
            final int s = -(q + r);
            final int qt = (5 * q - 4 * r + 2 * s) / 21;
            final int rt = (2 * q + 5 * r - 4 * s) / 21;
            q = qt;
            r = rt;
        }
        return new CoordinateImpl(Arrays.copyOf(buffer0, lastNonZero));
    }

    @Override
    public Hex.Coordinate toHex(final Coordinate coordinate) {
        final int level = coordinate.level();
        int q = 0;
        int r = 0;
        for (int i = level - 1; i >= 0; i--) {
            final int s = -(q + r);
            final int qt = (11 * q + 8 * r + 2 * s) / 3;
            final int rt = (2 * q + 11 * r + 8 * s) / 3;
            final Hex.Coordinate dir = DECODE[coordinate.get(i)];
            q = qt + dir.q();
            r = rt + dir.r();
        }
        return new Hex.Coordinate(q, r);
    }

    @Override
    public CoordinateImpl add(final Coordinate first, final Coordinate second) {
        final int firstLevel = first.level();
        int len = Math.max(firstLevel, second.level());
        Arrays.fill(buffer1, (byte) 0);
        copyData(buffer1, first);
        int lastNonZero = -1;
        for (int i = 0; i < len; i++) {
            final byte sum = ADDITION_TABLE[buffer1[i] * 7 + second.get(i)];
            byte b = (byte) (sum & 7);
            if (b != 0) {
                lastNonZero = i;
            }
            buffer0[i] = b;
            int carry = sum >>> 3;
            int j = i + 1;
            while (carry > 0 && j < MAX_LEVEL) {
                final byte carryByte = ADDITION_TABLE[buffer1[j] * 7 + carry];
                b = (byte) (carryByte & 7);
                buffer1[j] = b;
                if (b != 0) {
                    len = Math.max(len, j + 1);
                }
                carry = carryByte >>> 3;
                j++;
            }
        }
        return new CoordinateImpl(Arrays.copyOf(buffer0, lastNonZero + 1));
    }

    public static final class CoordinateImpl implements Coordinate {
        private final byte[] data;

        public CoordinateImpl(final byte[] data) {
            this.data = data;
        }

        @Override
        public int level() {
            return data.length;
        }

        @Override
        public byte get(final int level) {
            if (level >= data.length) {
                return 0;
            }
            return data[level];
        }
    }

    public static int innerHash(final Coordinate coordinate, final int level) {
        long s = 0;
        final int min = Math.min(coordinate.level(), level);
        for (int i = 0; i < min; i++) {
            s = s ^ HashCommon.mix(s ^ coordinate.get(i));
        }
        return (int) (s ^ (s >>> 32));
    }

    public static int outerHash(final Coordinate coordinate, final int level) {
        long s = 0;
        final int max = coordinate.level();
        for (int i = level; i < max; i++) {
            s = s ^ HashCommon.mix(s ^ coordinate.get(i));
        }
        return (int) (s ^ (s >>> 32));
    }

    public static boolean innerEquals(final Coordinate first, final Coordinate second, final int level) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        final int longest = Math.min(Math.max(first.level(), second.level()), level);
        for (int i = 0; i < longest; i++) {
            if (first.get(i) != second.get(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean outerEquals(final Coordinate first, final Coordinate second, final int level) {
        if (first == second) {
            return true;
        }
        if (first == null || second == null) {
            return false;
        }
        final int max = first.level();
        if (max != second.level()) {
            return false;
        }
        for (int i = level; i < max; i++) {
            if (first.get(i) != second.get(i)) {
                return false;
            }
        }
        return true;
    }

    public static Hash.Strategy<Coordinate> innerStrategy(final int level) {
        return new Hash.Strategy<>() {
            @Override
            public int hashCode(final Coordinate o) {
                return SHMImpl.innerHash(o, level);
            }

            @Override
            public boolean equals(final Coordinate a, final Coordinate b) {
                return SHMImpl.innerEquals(a, b, level);
            }
        };
    }

    public static Hash.Strategy<Coordinate> outerStrategy(final int level) {
        return new Hash.Strategy<>() {
            @Override
            public int hashCode(final Coordinate o) {
                return SHMImpl.outerHash(o, level);
            }

            @Override
            public boolean equals(final Coordinate a, final Coordinate b) {
                return SHMImpl.outerEquals(a, b, level);
            }
        };
    }

    public static CoordinateImpl outerTruncate(final Coordinate coordinate, final int level) {
        if (coordinate.level() <= level) {
            return new CoordinateImpl(new byte[]{});
        }
        final byte[] copy = new byte[coordinate.level()];
        copyData(copy, coordinate);
        Arrays.fill(copy, 0, level, (byte) 0);
        return new CoordinateImpl(copy);
    }

    private static void copyData(final byte[] data, final Coordinate coordinate) {
        if (coordinate instanceof CoordinateImpl impl) {
            System.arraycopy(impl.data, 0, data, 0, Math.min(data.length, impl.data.length));
        } else {
            final int len = Math.min(data.length, coordinate.level());
            for (int i = 0; i < len; i++) {
                data[i] = coordinate.get(i);
            }
        }
    }

    public static final class LevelCacheImpl implements LevelCache {
        private final int level;
        private final Hash.Strategy<Coordinate> inner;
        private final Hash.Strategy<Coordinate> outer;
        private final Hash.Strategy<Coordinate> full;
        private final Coordinate up;
        private final Coordinate upRight;
        private final Coordinate downRight;
        private final Coordinate down;
        private final Coordinate downLeft;
        private final Coordinate upLeft;

        public LevelCacheImpl(final int level) {
            this.level = level;
            inner = SHMImpl.innerStrategy(level);
            outer = SHMImpl.outerStrategy(level);
            full = SHMImpl.innerStrategy(MAX_LEVEL + 1);
            up = SHM.offset(Hex.Direction.UP, level);
            upRight = SHM.offset(Hex.Direction.UP_RIGHT, level);
            downRight = SHM.offset(Hex.Direction.DOWN_RIGHT, level);
            down = SHM.offset(Hex.Direction.DOWN, level);
            downLeft = SHM.offset(Hex.Direction.DOWN_LEFT, level);
            upLeft = SHM.offset(Hex.Direction.UP_LEFT, level);
        }

        @Override
        public Coordinate offset(final Hex.Direction direction) {
            return switch (direction) {
                case UP -> up;
                case UP_RIGHT -> upRight;
                case DOWN_RIGHT -> downRight;
                case DOWN -> down;
                case DOWN_LEFT -> downLeft;
                case UP_LEFT -> upLeft;
            };
        }

        @Override
        public int level() {
            return level;
        }

        @Override
        public Hash.Strategy<Coordinate> inner() {
            return inner;
        }

        @Override
        public Hash.Strategy<Coordinate> outer() {
            return outer;
        }

        @Override
        public Hash.Strategy<Coordinate> full() {
            return full;
        }
    }

    public SHMImpl() {
        this(MAX_LEVEL);
    }

    public SHMImpl(final int level) {
        if (level < 0 || level > MAX_LEVEL) {
            throw new IllegalArgumentException();
        }
        defaultLevel = level;
    }
}
