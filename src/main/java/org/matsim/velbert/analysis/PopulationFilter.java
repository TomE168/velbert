package org.matsim.velbert.analysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;
public class PopulationFilter {
    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:3857");
    private static final String postleitzahlenGebiete = "C:\\Users\\tekuh\\Downloads\\OSM_PLZ_072019.shp";
    private static final String populationFile = "C:\\Users\\tekuh\\OneDrive\\Master\\Matsim\\output-velbert-0206\\velbert.output_plans.xml.gz";
    private static final String networkFile = "C:\\Users\\tekuh\\OneDrive\\Master\\Matsim\\output-velbert-0206\\velbert.output_network.xml.gz";
    private static final List<String> dilutionAreaPlz = new ArrayList<>(Arrays.asList("42551", "42549", "42555", "42553"));
    public List<Id> getPersonIdWhichHomeIsInDilutionArea(){
        var population = PopulationUtils.readPopulation(populationFile);
        return getPersonIdWichHomeIsInDilutionArea(population);
    }

    private static List<Id> getPersonIdWichHomeIsInDilutionArea(Population population) {
            var features = ShapeFileReader.getAllFeatures(postleitzahlenGebiete);
            Network network = NetworkUtils.readNetwork(networkFile);
            List<Id> personIdWichHomeIsInDilutionArea = new ArrayList<>();
            for (var person : population.getPersons().values()) {
                var plan = person.getSelectedPlan();
                var activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                for (var activity : activities) {
                    var activityCoord = getCoord(activity, network);
                    var dilutionAreaGeometry = getDilutionAreaGeometry(features);
                    if (activity.getType().contains("home") && isInDilutionAreaGeometry(activityCoord, dilutionAreaGeometry)) {
                        personIdWichHomeIsInDilutionArea.add(person.getId());
                        break;
                    }
                }
            }
            System.out.println(personIdWichHomeIsInDilutionArea);
            return personIdWichHomeIsInDilutionArea;
        }

    private static Coord getCoord(Activity activity, Network network) {
        if (activity.getCoord() != null) {
            return activity.getCoord();
        }

        return network.getLinks().get(activity.getLinkId()).getCoord();
    }

    private static boolean isInDilutionAreaGeometry(Coord coord, Geometry geometry) {
        var transformed = transformation.transform(coord);
        return geometry.covers(MGC.coord2Point(transformed));
    }
    private static Geometry getDilutionAreaGeometry(Collection<SimpleFeature> features) {
        return features.stream()
                .filter(feature -> dilutionAreaPlz.contains(feature.getAttribute("plz")))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .findAny()
                .orElseThrow();
    }


//    private static boolean isCoordInPlz(Coord coord, List Plz){
//        return plz.contains(getGeometriesFromPlz(coord));
//    }
//
//    private static List getGeometriesFromPlz(List plz) {
//        var features = ShapeFileReader.getAllFeatures(postleitzahlenGebiete);
//        List<Geometry> dilutionAreaGeometries = new ArrayList<>();
//        for (SimpleFeature feature : features) {
//            if (plz.contains(feature.getAttribute("plz"))) {
//                dilutionAreaGeometries.add((Geometry) feature.getDefaultGeometry());
//            }
//        }
//        return dilutionAreaGeometries;
//    }
    }
