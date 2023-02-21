package org.echoiot.rule.engine.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.util.GeometryFixer;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContextFactory;
import org.locationtech.spatial4j.distance.DistanceUtils;
import org.locationtech.spatial4j.shape.Point;
import org.locationtech.spatial4j.shape.SpatialRelation;
import org.locationtech.spatial4j.shape.jts.JtsGeometry;

import java.util.*;
import java.util.stream.Collectors;

public class GeoUtil {

    private static final SpatialContext distCtx = SpatialContext.GEO;
    private static final JtsSpatialContext jtsCtx;

    private static final JsonParser JSON_PARSER = new JsonParser();

    static {
        @NotNull JtsSpatialContextFactory factory = new JtsSpatialContextFactory();
        factory.normWrapLongitude = true;
        jtsCtx = factory.newSpatialContext();
    }

    public static synchronized double distance(@NotNull Coordinates x, @NotNull Coordinates y, @NotNull RangeUnit unit) {
        Point xLL = distCtx.getShapeFactory().pointXY(x.getLongitude(), x.getLatitude());
        Point yLL = distCtx.getShapeFactory().pointXY(y.getLongitude(), y.getLatitude());
        return unit.fromKm(distCtx.getDistCalc().distance(xLL, yLL) * DistanceUtils.DEG_TO_KM);
    }

    public static synchronized boolean contains(@NonNull String polygonInString, @NonNull Coordinates coordinates) {
        if (polygonInString.isEmpty() || polygonInString.isBlank()) {
            throw new RuntimeException("Polygon string can't be empty or null!");
        }

        @NotNull JsonArray polygonsJson = normalizePolygonsJson(JSON_PARSER.parse(polygonInString).getAsJsonArray());
        @NotNull List<Geometry> polygons = buildPolygonsFromJson(polygonsJson);
        @NotNull Set<Geometry> holes = extractHolesFrom(polygons);
        polygons.removeIf(holes::contains);

        Geometry globalGeometry = unionToGlobalGeometry(polygons, holes);
        var point = jtsCtx.getShapeFactory().getGeometryFactory()
                .createPoint(new Coordinate(coordinates.getLatitude(), coordinates.getLongitude()));

        return globalGeometry.contains(point);
    }

    private static Geometry unionToGlobalGeometry(@NotNull List<Geometry> polygons, @NotNull Set<Geometry> holes) {
        Geometry globalPolygon = polygons.stream().reduce(Geometry::union).orElseThrow(() ->
                new RuntimeException("Error while calculating globalPolygon - the result of all polygons union is null"));
        @NotNull Optional<Geometry> globalHole = holes.stream().reduce(Geometry::union);
        if (globalHole.isEmpty()) {
            return globalPolygon;
        } else {
            return globalPolygon.difference(globalHole.get());
        }
    }

    @NotNull
    private static JsonArray normalizePolygonsJson(@NotNull JsonArray polygonsJsonArray) {
        @NotNull JsonArray result = new JsonArray();
        normalizePolygonsJson(polygonsJsonArray, result);
        return result;
    }

    private static void normalizePolygonsJson(@NotNull JsonArray polygonsJsonArray, @NotNull JsonArray result) {
        if (containsArrayWithPrimitives(polygonsJsonArray)) {
            result.add(polygonsJsonArray);
        } else {
            for (@NotNull JsonElement element : polygonsJsonArray) {
                if (containsArrayWithPrimitives(element.getAsJsonArray())) {
                    result.add(element);
                } else {
                    normalizePolygonsJson(element.getAsJsonArray(), result);
                }
            }
        }
    }

    @NotNull
    private static Set<Geometry> extractHolesFrom(@NotNull List<Geometry> polygons) {
        @NotNull Map<Geometry, List<Geometry>> polygonsHoles = new HashMap<>();

        for (Geometry polygon : polygons) {
            @NotNull List<Geometry> holes = polygons.stream()
                                                    .filter(another -> !another.equalsExact(polygon))
                                                    .filter(another -> {
                        JtsGeometry currentGeo = jtsCtx.getShapeFactory().makeShape(polygon);
                        JtsGeometry anotherGeo = jtsCtx.getShapeFactory().makeShape(another);

                        boolean currentContainsAnother = currentGeo
                                .relate(anotherGeo)
                                .equals(SpatialRelation.CONTAINS);

                        boolean anotherWithinCurrent = anotherGeo
                                .relate(currentGeo)
                                .equals(SpatialRelation.WITHIN);

                        return currentContainsAnother && anotherWithinCurrent;
                    })
                                                    .collect(Collectors.toList());

            if (!holes.isEmpty()) {
                polygonsHoles.put(polygon, holes);
            }
        }

        return polygonsHoles.values().stream().flatMap(Collection::stream).collect(Collectors.toSet());
    }

    @NotNull
    private static List<Geometry> buildPolygonsFromJson(@NotNull JsonArray polygonsJsonArray) {
        @NotNull List<Geometry> polygons = new LinkedList<>();

        for (@NotNull JsonElement polygonJsonArray : polygonsJsonArray) {
            polygons.add(
                    buildPolygonFromCoordinates(parseCoordinates(polygonJsonArray.getAsJsonArray()))
            );
        }

        return polygons;
    }

    private static Geometry buildPolygonFromCoordinates(@NotNull List<Coordinate> coordinates) {
        if (coordinates.size() == 2) {
            Coordinate a = coordinates.get(0);
            Coordinate c = coordinates.get(1);
            coordinates.clear();

            @NotNull Coordinate b = new Coordinate(a.x, c.y);
            @NotNull Coordinate d = new Coordinate(c.x, a.y);
            coordinates.addAll(List.of(a, b, c, d, a));
        }

        CoordinateSequence coordinateSequence = jtsCtx
                .getShapeFactory()
                .getGeometryFactory()
                .getCoordinateSequenceFactory()
                .create(coordinates.toArray(new Coordinate[0]));

        return GeometryFixer.fix(jtsCtx.getShapeFactory().getGeometryFactory().createPolygon(coordinateSequence));
    }

    @NotNull
    private static List<Coordinate> parseCoordinates(@NotNull JsonArray coordinatesJson) {
        @NotNull List<Coordinate> result = new LinkedList<>();

        for (@NotNull JsonElement coords : coordinatesJson) {
            double x = coords.getAsJsonArray().get(0).getAsDouble();
            double y = coords.getAsJsonArray().get(1).getAsDouble();
            result.add(new Coordinate(x, y));
        }

        if (result.size() >= 3) {
            result.add(result.get(0));
        }

        return result;
    }

    private static boolean containsPrimitives(@NotNull JsonArray array) {
        for (@NotNull JsonElement element : array) {
            return element.isJsonPrimitive();
        }

        return false;
    }

    private static boolean containsArrayWithPrimitives(@NotNull JsonArray array) {
        for (@NotNull JsonElement element : array) {
            if (!containsPrimitives(element.getAsJsonArray())) {
                return false;
            }
        }

        return true;
    }

}
