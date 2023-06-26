package io.github.stuff_stuffs.river_net_gen.util;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.HashCommon;

import java.util.Arrays;

public final class SHM {
    public static final int MAX_LEVEL = 16;
    private static final byte[] ENCODE = new byte[]{0, 5, 1, 6, 3, 4, 2};
    private static final Hex.Coordinate[] DECODE;
    private static final byte[] ADDITION_TABLE;

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

    public static Coordinate offset(final Hex.Direction direction, final int level) {
        final byte[] data = new byte[level + 1];
        data[data.length - 1] = switch (direction) {
            case UP -> 5;
            case UP_RIGHT -> 6;
            case DOWN_RIGHT -> 1;
            case DOWN -> 2;
            case DOWN_LEFT -> 3;
            case UP_LEFT -> 4;
        };
        return new Coordinate(data);
    }

    public Coordinate fromHex(final Hex.Coordinate coordinate) {
        return fromHex(coordinate, defaultLevel);
    }

    public Coordinate fromHex(final Hex.Coordinate coordinate, final int level) {
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
        return new Coordinate(Arrays.copyOf(buffer0, lastNonZero));
    }

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

    public Coordinate add(final Coordinate first, final Coordinate second) {
        final int firstLevel = first.level();
        int len = Math.max(firstLevel, second.level());
        Arrays.fill(buffer1, (byte) 0);
        System.arraycopy(first.data, 0, buffer1, 0, firstLevel);
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
        return new Coordinate(Arrays.copyOf(buffer0, lastNonZero + 1));
    }

    public static final class Coordinate {
        private final byte[] data;

        public Coordinate(final byte[] data) {
            this.data = data;
        }

        public int level() {
            return data.length;
        }

        public byte get(final int level) {
            if (level >= data.length) {
                return 0;
            }
            return data[level];
        }
    }

    public static int innerHash(Coordinate coordinate, int level) {
        return innerHash(coordinate.data, level);
    }

    public static int innerHash(final byte[] data, final int level) {
        long s = 0;
        final int min = Math.min(data.length, level);
        for (int i = 0; i < min; i++) {
            s = s ^ HashCommon.mix(s ^ data[i]);
        }
        return (int) (s ^ (s >>> 32));
    }

    public static int outerHash(Coordinate coordinate, int level) {
        return outerHash(coordinate.data, level);
    }

    public static int outerHash(final byte[] data, final int level) {
        long s = 0;
        for (int i = level; i < data.length; i++) {
            s = s ^ HashCommon.mix(s ^ data[i]);
        }
        return (int) (s ^ (s >>> 32));
    }

    public static boolean innerEquals(final Coordinate first, final Coordinate second, final int level) {
        if(first==second) {
            return true;
        }
        if(first==null || second == null) {
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
        if(first==second) {
            return true;
        }
        if(first==null || second == null) {
            return false;
        }
        final int longest = Math.max(first.level(), second.level());
        for (int i = level; i < longest; i++) {
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
                return SHM.innerHash(o.data, level);
            }

            @Override
            public boolean equals(final Coordinate a, final Coordinate b) {
                return SHM.innerEquals(a, b, level);
            }
        };
    }

    public static Hash.Strategy<Coordinate> outerStrategy(final int level) {
        return new Hash.Strategy<>() {
            @Override
            public int hashCode(final Coordinate o) {
                return SHM.outerHash(o.data, level);
            }

            @Override
            public boolean equals(final Coordinate a, final Coordinate b) {
                return SHM.outerEquals(a, b, level);
            }
        };
    }

    public static Coordinate outerTruncate(final Coordinate coordinate, final int level) {
        if (coordinate.level() < level) {
            return new Coordinate(new byte[]{});
        }
        final byte[] copy = Arrays.copyOf(coordinate.data, coordinate.data.length);
        Arrays.fill(copy, 0, level, (byte) 0);
        return new Coordinate(copy);
    }

    public static final class LevelCache {
        private final Hash.Strategy<SHM.Coordinate> inner;
        private final Hash.Strategy<SHM.Coordinate> outer;
        private final SHM.Coordinate up;
        private final SHM.Coordinate upRight;
        private final SHM.Coordinate downRight;
        private final SHM.Coordinate down;
        private final SHM.Coordinate downLeft;
        private final SHM.Coordinate upLeft;

        public LevelCache(final int level) {
            inner = SHM.innerStrategy(level);
            outer = SHM.outerStrategy(level);
            up = SHM.offset(Hex.Direction.UP, level);
            upRight = SHM.offset(Hex.Direction.UP_RIGHT, level);
            downRight = SHM.offset(Hex.Direction.DOWN_RIGHT, level);
            down = SHM.offset(Hex.Direction.DOWN, level);
            downLeft = SHM.offset(Hex.Direction.DOWN_LEFT, level);
            upLeft = SHM.offset(Hex.Direction.UP_LEFT, level);
        }

        public SHM.Coordinate offset(final Hex.Direction direction) {
            return switch (direction) {
                case UP -> up;
                case UP_RIGHT -> upRight;
                case DOWN_RIGHT -> downRight;
                case DOWN -> down;
                case DOWN_LEFT -> downLeft;
                case UP_LEFT -> upLeft;
            };
        }

        public Hash.Strategy<SHM.Coordinate> inner() {
            return inner;
        }

        public Hash.Strategy<SHM.Coordinate> outer() {
            return outer;
        }
    }

    public SHM() {
        this(MAX_LEVEL);
    }

    public SHM(final int level) {
        if (level < 0 || level > MAX_LEVEL) {
            throw new IllegalArgumentException();
        }
        defaultLevel = level;
    }
}
