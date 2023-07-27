package io.github.stuff_stuffs.river_net_gen.api.geo.column;

import io.github.stuff_stuffs.river_net_gen.api.util.ProceduralRandom;
import it.unimi.dsi.fastutil.HashCommon;

public class GeoColumnInterpolator3d {
    private final Result fResult;
    private final Result sResult;
    private final Result tResult;

    public GeoColumnInterpolator3d(final double x0, final double z0, final GeoColumn first, final double x1, final double z1, final GeoColumn second, final double x2, final double z2, final GeoColumn third) {
        this(x0, z0, first, x1, z1, second, x2, z2, third, ProceduralRandom.Factory.XOROSHIRO.create(tripleSeed(first, second, third)));
    }

    public GeoColumnInterpolator3d(final double x0, final double z0, final GeoColumn first, final double x1, final double z1, final GeoColumn second, final double x2, final double z2, final GeoColumn third, final ProceduralRandom random) {
        fResult = build(x0, z0, first, random);
        sResult = build(x1, z1, second, random);
        tResult = build(x2, z2, third, random);
    }

    public String interpolate(final double x, final double y, final double z) {
        final double[] bestScore = new double[]{Double.NEGATIVE_INFINITY};
        int best = search(x, fResult.yScale * y, z, fResult.sections, bestScore);
        int chosen = 0;
        int r = search(x, sResult.yScale * y, z, sResult.sections, bestScore);
        if (r != -1) {
            best = r;
            chosen = 1;
        }
        r = search(x, tResult.yScale * y, z, tResult.sections, bestScore);
        if (r != -1) {
            best = r;
            chosen = 2;
        }
        return switch (chosen) {
            case 0 -> fResult.data[best];
            case 1 -> sResult.data[best];
            case 2 -> tResult.data[best];
            default -> throw new IndexOutOfBoundsException();
        };
    }

    private static int search(final double x, final double y, final double z, final Section[] sections, final double[] bestScoreArr) {
        double bestScore = bestScoreArr[0];
        int best = -1;
        for (int i = 0; i < sections.length; i++) {
            final Section section = sections[i];
            final double dx = x - section.x;
            final double dy = 2 * (y - section.y);
            final double dz = z - section.z;
            final double manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
            final double score = 1 / (manhattan + 4 * Math.abs(dx * section.nx + dy * section.ny + dz * section.nz));
            if (bestScore < score) {
                bestScore = score;
                best = i;
            }
        }
        bestScoreArr[0] = bestScore;
        return best;
    }

    private static long tripleSeed(final GeoColumn first, final GeoColumn second, final GeoColumn third) {
        return HashCommon.mix(computeSeed(first) ^ HashCommon.mix(computeSeed(second) ^ HashCommon.mix(computeSeed(third)) + 1) + 1);
    }

    private static long computeSeed(final GeoColumn column) {
        long seed = 0;
        final int length = column.length();
        for (int i = 0; i < length; i++) {
            seed = HashCommon.murmurHash3(seed + 1) ^ column.data(i).hashCode();
        }
        return seed;
    }

    private static Result build(final double x, final double z, final GeoColumn column, final ProceduralRandom random) {
        final int length = column.length();
        final Section[] sections = new Section[length];
        final String[] data = new String[length];
        final double yScale = 1 / (double) (column.height(length - 1) + column.thickness(length - 1));
        for (int i = 0; i < length; i++) {
            final double nx = (random.nextDouble() - 0.5) * 0.1;
            final double ny = 1 - Math.abs(random.nextGaussian());
            final double nz = (random.nextDouble() - 0.5) * 0.1;
            final double lenInv = 1 / Math.sqrt(nx * nx + ny * ny + nz * nz);
            sections[i] = new Section(x, column.height(i) * yScale, z, nx * lenInv, ny * lenInv, nz * lenInv);
            data[i] = column.data(i);
        }
        return new Result(yScale, sections, data);
    }

    private record Result(double yScale, Section[] sections, String[] data) {
    }

    private record Section(double x, double y, double z, double nx, double ny, double nz) {
    }
}
