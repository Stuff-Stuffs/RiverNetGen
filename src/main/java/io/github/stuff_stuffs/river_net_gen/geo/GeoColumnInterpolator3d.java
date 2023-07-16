package io.github.stuff_stuffs.river_net_gen.geo;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GeoColumnInterpolator3d<T> {
    private static final int MAX_SLOPE = 64;
    private final MatchResult result;

    public GeoColumnInterpolator3d(final ColumnCoordinate firstCoord, final GeoColumn<T> first, final ColumnCoordinate secondCoord, final GeoColumn<T> second, final ColumnCoordinate thirdCoord, final GeoColumn<T> third) {
        result = match(firstCoord, first, secondCoord, second, thirdCoord, third);
    }

    private static MatchResult match(final ColumnCoordinate firstCoord, final GeoColumn<?> first, final ColumnCoordinate secondCoord, final GeoColumn<?> second, final ColumnCoordinate thirdCoord, final GeoColumn<?> third) {
        int f = 0;
        int s = 0;
        int t = 0;
        final int fLength = first.length();
        final int sLength = second.length();
        final int tLength = third.length();
        final int count = Math.max(fLength, Math.max(sLength, tLength));
        final List<Vec3d> normals = new ArrayList<>(count);
        final List<Vec3d> vertices = new ArrayList<>(count);
        final List<Object> data = new ArrayList<>(count);
        Vec3d lastNorm = new Vec3d(0, 1, 0);
        int prevSuccess = 0;
        while (f < fLength && s < sLength && t < tLength) {
            final Match3 match3 = match3(firstCoord, first, secondCoord, second, thirdCoord, third, f, s, t);
            if (match3 != null) {
                final int fDiff = f - (prevSuccess + 1);
                final int sDiff = match3.s - s;
                final int tDiff = match3.t - t;
                final int fCount = 0;
                int sCount = 0;
                int tCount = 0;
                while (sCount < sDiff || tCount < tDiff) {
                    final boolean chosen;
                    if (tCount == tDiff) {
                        chosen = true;
                    } else if (sCount == sDiff) {
                        chosen = false;
                    } else {
                        chosen = second.height(s + sCount) <= third.height(t + tCount);
                    }
                    if (chosen) {
                        final double alpha = (sCount + 1) / (double) (sDiff + 1);
                        normals.add(slerp(lastNorm, match3.normal, alpha));
                        vertices.add(new Vec3d(secondCoord.x, second.height(s + sCount), secondCoord.z));
                        data.add(second.data(s + sCount));
                        sCount++;
                    } else {
                        final double alpha = (tCount + 1) / (double) (tDiff + 1);
                        normals.add(slerp(lastNorm, match3.normal, alpha));
                        vertices.add(new Vec3d(thirdCoord.x, second.height(t + tCount), thirdCoord.z));
                        data.add(second.data(t + tCount));
                        tCount++;
                    }
                }
                normals.add(match3.normal);
                vertices.add(match3.point);
                data.add(first.data(f));
                lastNorm = match3.normal;
                prevSuccess = f;
                f = f + 1;
                s = match3.s + 1;
                t = match3.t + 1;
            }
        }
    }

    private static void fill(final ColumnCoordinate firstCoord, final GeoColumn<?> first, final ColumnCoordinate secondCoord, final GeoColumn<?> second, final ColumnCoordinate thirdCoord, final GeoColumn<?> third, final int fStart, final int sStart, final int tStart, final int fEnd, final int sEnd, final int tEnd, final Vec3d lastNormal, @Nullable final Vec3d nextNormal, final List<Vec3d> normals, final List<Vec3d> vertices, final List<Object> data) {
        final int fDiff = fEnd - fStart;
        final int sDiff = sEnd - sStart;
        final int tDiff = tEnd - tStart;
        int fCount = 0;
        int sCount = 0;
        int tCount = 0;
        while (fCount < fDiff || sCount < sDiff || tCount < tDiff) {
            final int chosen;
            if (fCount == fDiff) {
                if (sCount == sDiff) {
                    chosen = 2;
                } else if (tCount == tDiff) {
                    chosen = 1;
                } else {
                    chosen = second.height(sCount + sStart) <= third.height(tCount + tStart) ? 1 : 2;
                }
            } else if (sCount == sDiff) {
                if (tCount == tDiff) {
                    chosen = 0;
                } else {
                    chosen = first.height(fCount + fStart) <= third.height(tCount + tStart) ? 0 : 2;
                }
            } else if (tCount == tDiff) {
                chosen = first.height(fCount + fStart) <= second.height(sCount + sStart) ? 0 : 1;
            } else {
                final int fHeight = first.height(fStart + fCount);
                final int sHeight = first.height(sStart + sCount);
                final int tHeight = first.height(tStart + tCount);
                if (fHeight <= sHeight) {
                    if (fHeight <= tHeight) {
                        chosen = 0;
                    } else {
                        chosen = 2;
                    }
                } else if (sHeight <= tHeight) {
                    chosen = 1;
                } else {
                    chosen = 2;
                }
            }
            if (chosen == 0) {
                final Vec3d vertex = new Vec3d(firstCoord.x, first.height(fStart + fCount), firstCoord.z);
                Vec3d normal;
                if (nextNormal == null) {
                    normal = lastNormal;
                } else {
                    double alpha = (fCount + 1) / (double) (fDiff + 1);
                    normal = slerp(lastNormal, nextNormal, alpha);
                }
                vertices.add(vertex);
                normals.add(normal);
                data.add(first.data(fStart+fCount));
                fCount++;
            } else if(chosen==1) {
                final Vec3d vertex = new Vec3d(secondCoord.x, second.height(sStart + sCount), secondCoord.z);
                Vec3d normal;
                if (nextNormal == null) {
                    normal = lastNormal;
                } else {
                    double alpha = (sCount + 1) / (double) (sDiff + 1);
                    normal = slerp(lastNormal, nextNormal, alpha);
                }
                vertices.add(vertex);
                normals.add(normal);
                data.add(second.data(sStart+sCount));
                sCount++;
            } else {
                final Vec3d vertex = new Vec3d(thirdCoord.x, third.height(tStart + tCount), thirdCoord.z);
                Vec3d normal;
                if (nextNormal == null) {
                    normal = lastNormal;
                } else {
                    double alpha = (tCount + 1) / (double) (tDiff + 1);
                    normal = slerp(lastNormal, nextNormal, alpha);
                }
                vertices.add(vertex);
                normals.add(normal);
                data.add(third.data(tStart+tCount));
                sCount++;
            }
        }
    }

    private static @Nullable Match3 match3(final ColumnCoordinate firstCoord, final GeoColumn<?> first, final ColumnCoordinate secondCoord, final GeoColumn<?> second, final ColumnCoordinate thirdCoord, final GeoColumn<?> third, final int fStart, final int sStart, final int tStart) {
        final int sLength = second.length();
        final int tLength = third.length();
        final int fHeight = first.height(fStart);
        final Object fData = first.data(fStart);
        final double ax = secondCoord.x - firstCoord.x;
        final double bx = thirdCoord.x - firstCoord.x;
        final double az = secondCoord.z - firstCoord.z;
        final double bz = thirdCoord.z - firstCoord.z;
        for (int i = sStart; i < sLength; i++) {
            final int sHeight = second.height(i);
            if (sHeight - fHeight > MAX_SLOPE) {
                return null;
            }
            if (!fData.equals(second.data(i))) {
                continue;
            }
            for (int j = tStart; j < tLength; j++) {
                final int tHeight = third.height(j);
                if (tHeight - sHeight > MAX_SLOPE | tHeight - fHeight > MAX_SLOPE) {
                    return null;
                }
                if (!fData.equals(third.data(j))) {
                    continue;
                }
                final double ay = sHeight - fHeight;
                final double by = tHeight - fHeight;
                final double x = ay * bz - az * by;
                final double y = ax * bz - az * bx;
                final double z = ax * by - ay * bx;
                final double invLength = 1 / Math.sqrt(x * x + y * y + z * z);
                final Vec3d normal = new Vec3d(x * invLength, y * invLength, z * invLength);
                return new Match3(new Vec3d(firstCoord.x, fHeight, firstCoord.z), normal, fStart, i, j);
            }
        }
        return null;
    }

    private static Vec3d slerp(final Vec3d start, final Vec3d end, final double alpha) {
        final double omega = start.x * end.x + start.y * end.y + start.z * end.z;
        final double sinOmega = Math.sin(omega);
        final double startFactor = Math.sin((1 - alpha) * omega) / sinOmega;
        final double endFactor = Math.sin(alpha * omega) / sinOmega;
        return new Vec3d(startFactor * start.x + endFactor * end.x, startFactor * start.y + endFactor * end.y, startFactor * start.z + endFactor * end.z);
    }

    private record Match3(Vec3d point, Vec3d normal, int f, int s, int t) {
    }

    private record MatchResult(Vec3d[] vertices, Vec3d[] normals, Object[] data) {
    }

    private record Vec3d(double x, double y, double z) {
    }

    public record ColumnCoordinate(double x, double z) {
    }
}
