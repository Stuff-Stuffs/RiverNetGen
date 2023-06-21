package io.github.stuff_stuffs.river_net_gen.util;

import it.unimi.dsi.fastutil.Hash;

import java.util.Arrays;

public final class SHM {
    private static final long LONG_PHI = 0x9E3779B97F4A7C15L;
    private static final byte[] DIRS = new byte[]{0, 5, 1, 6, 3, 4, 2};
    private static final Hex.Coordinate[] DECODE;
    private static final byte[][] ADDITION_TABLE = {{0, 1, 2, 3, 4, 5, 6}, {1, 63, 15, 2, 0, 6, 64}, {2, 15, 14, 26, 3, 0, 1}, {3, 2, 26, 25, 31, 4, 0}, {4, 0, 3, 31, 36, 42, 5}, {5, 6, 0, 4, 42, 41, 53}, {6, 64, 1, 0, 5, 53, 52}};


    static {
        DECODE = new Hex.Coordinate[7];
        final int[] code = new int[]{0, -1, 1};
        DECODE[0] = new Hex.Coordinate(0, 0);
        for (int i = 1; i < 7; i++) {
            DECODE[i] = new Hex.Coordinate(code[0], code[1]);
            rotate(code);
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
    private final byte[] buffer0 = new byte[512];
    private final byte[] buffer1 = new byte[512];

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
            final byte d = DIRS[proj < 0 ? proj + 7 : proj];
            buffer0[l++] = d;
            if (d != 0) {
                lastNonZero = l;
                final Hex.Coordinate dir = DECODE[d];
                q = q - dir.q();
                r = r - dir.r();
            }
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
        int l = -1;
        System.arraycopy(first.data, 0, buffer1, 0, firstLevel);
        if (len > firstLevel) {
            Arrays.fill(buffer1, firstLevel, len, (byte) 0);
        }
        for (int i = 0; i < len; i++) {
            final byte sum = ADDITION_TABLE[buffer1[i]][second.get(i)];
            byte b = (byte) (sum % 10);
            if (b != 0) {
                l = Math.max(i, l);
            }
            buffer0[i] = b;
            int carry = sum / 10;
            int j = i + 1;
            while (carry > 0) {
                final byte carryByte = ADDITION_TABLE[buffer1[j]][carry];
                b = (byte) (carryByte % 10);
                buffer1[j] = b;
                if (b != 0) {
                    len = Math.max(len, j + 1);
                }
                carry = carryByte / 10;
                j++;
            }
        }
        return new Coordinate(Arrays.copyOf(buffer0, l + 1));
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

    private static long mix(final long x) {
        long h = x * LONG_PHI;
        h ^= h >>> 32;
        return h ^ (h >>> 16);
    }

    public static int innerHash(final byte[] data, final int level) {
        long s = 0;
        final int min = Math.min(data.length, level);
        for (int i = 0; i < min; i++) {
            final byte datum = data[i];
            s = s ^ mix(s ^ datum);
        }
        return (int) (s ^ (s >>> 32));
    }

    public static int outerHash(final byte[] data, final int level) {
        long s = 0;
        for (int i = level; i < data.length; i++) {
            s = s ^ mix(s ^ data[i]);
        }
        return (int) (s ^ (s >>> 32));
    }

    public static boolean innerEquals(final Coordinate first, final Coordinate second, final int level) {
        final int longest = Math.min(Math.max(first.level(), second.level()), level);
        for (int i = 0; i < longest; i++) {
            if (first.get(i) != second.get(i)) {
                return false;
            }
        }
        return true;
    }

    public static boolean outerEquals(final Coordinate first, final Coordinate second, final int level) {
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

    public SHM(final int level) {
        defaultLevel = level;
    }
}
