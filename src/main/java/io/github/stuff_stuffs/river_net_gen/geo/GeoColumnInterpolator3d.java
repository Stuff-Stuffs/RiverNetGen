package io.github.stuff_stuffs.river_net_gen.geo;

import io.github.stuff_stuffs.river_net_gen.util.ProceduralRandom;
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

    public int interpolate(final double x, final double y, final double z) {
        final Section[] fSections = fResult.sections;
        double bestScore = Double.NEGATIVE_INFINITY;
        int best = -1;
        for (int i = 0; i < fSections.length; i++) {
            final Section section = fSections[i];
            final double dx = x - section.x;
            final double dy = 2 * (y * fResult.yScale - section.y);
            final double dz = z - section.z;
            final double manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
            final double score = 1 / (manhattan + 4 * Math.abs(dx * section.nx + dy * section.ny + dz * section.nz));
            if (bestScore < score) {
                bestScore = score;
                best = fResult.data[i];
            }
        }
        final Section[] sSections = sResult.sections;
        for (int i = 0; i < sSections.length; i++) {
            final Section section = sSections[i];
            final double dx = x - section.x;
            final double dy = 2 * (y * sResult.yScale - section.y);
            final double dz = z - section.z;
            final double manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
            final double score = 1 / (manhattan + 4 * Math.abs(dx * section.nx + dy * section.ny + dz * section.nz));
            if (bestScore < score) {
                bestScore = score;
                best = sResult.data[i];
            }
        }

        final Section[] tSections = tResult.sections;
        for (int i = 0; i < tSections.length; i++) {
            final Section section = tSections[i];
            final double dx = x - section.x;
            final double dy = 2 * (y * tResult.yScale - section.y);
            final double dz = z - section.z;
            final double manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
            final double score = 1 / (manhattan + 4 * Math.abs(dx * section.nx + dy * section.ny + dz * section.nz));
            if (bestScore < score) {
                bestScore = score;
                best = tResult.data[i];
            }
        }
        return best;
    }

    private static long tripleSeed(final GeoColumn first, final GeoColumn second, final GeoColumn third) {
        return HashCommon.mix(computeSeed(first) ^ HashCommon.mix(computeSeed(second) ^ HashCommon.mix(computeSeed(third)) + 1) + 1);
    }

    private static long computeSeed(final GeoColumn column) {
        long seed = 0;
        final int length = column.length();
        for (int i = 0; i < length; i++) {
            seed = HashCommon.murmurHash3(seed + 1) ^ column.data(i);
        }
        return seed;
    }

    private static Result build(final double x, final double z, final GeoColumn column, final ProceduralRandom random) {
        final int length = column.length();
        final Section[] sections = new Section[length];
        final int[] data = new int[length];
        final double yScale = 1 / (double) (column.height(length - 1) + column.thickness(length - 1));
        for (int i = 0; i < length; i++) {
            final double nx = (random.nextDouble() - 0.5) * 0.1;
            final double ny = random.nextDouble() * 2 - 1;
            final double nz = (random.nextDouble() - 0.5) * 0.1;
            final double lenInv = 1 / Math.sqrt(nx * nx + ny * ny + nz * nz);
            sections[i] = new Section(x, column.height(i) * yScale, z, nx * lenInv, ny * lenInv, nz * lenInv);
            data[i] = column.data(i);
        }
        return new Result(yScale, sections, data);
    }

    private record Result(double yScale, Section[] sections, int[] data) {
    }

    private record Section(double x, double y, double z, double nx, double ny, double nz) {
    }
}
