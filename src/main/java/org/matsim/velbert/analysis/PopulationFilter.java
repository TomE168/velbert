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
import java.util.stream.Collectors;

public class PopulationFilter {
    private static final CoordinateTransformation transformation = TransformationFactory.getCoordinateTransformation("EPSG:25832", "EPSG:3857");
    private static final String postleitzahlenGebiete = "C:\\Users\\tekuh\\Downloads\\OSM_PLZ_072019.shp";
    private static final String populationFile = "C:\\Users\\tekuh\\OneDrive\\Master\\Matsim\\output-MUT\\output-velbert-2705\\velbert.output_plans.xml.gz";
    private static final String networkFile = "C:\\Users\\tekuh\\OneDrive\\Master\\Matsim\\output-MUT\\output-velbert-2705\\velbert.output_network.xml.gz";
    private static final List<String> dilutionAreaPlz = new ArrayList<>(Arrays.asList("42551", "42549", "42555", "42553"));
    //statt list set damit nicht mehrere IDs enthalten, contains geht schneller
    public Set<Id> getPersonIdWhichHomeIsInDilutionArea(){
        var population = PopulationUtils.readPopulation(populationFile);
        return getPersonIdWhichHomeIsInDilutionArea(population);
    }
    // plz als input von Methode
    public Set<Id> getPersonIdWhichHomeIsInDilutionArea(Population population) {
            var features = ShapeFileReader.getAllFeatures(postleitzahlenGebiete);
            var dilutionAreaGeometry = getDilutionAreaGeometry(features);
            Network network = NetworkUtils.readNetwork(networkFile);
            Set<Id> personIdWichHomeIsInDilutionArea = new HashSet<>();
            for (var person : population.getPersons().values()) {
                var plan = person.getSelectedPlan();
                var activities = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);
                for (var activity : activities) {
                    if (activity.getType().contains("home")) {
                        var activityCoord = getCoord(activity, network);
                        if (isInDilutionAreaGeometry(activityCoord, dilutionAreaGeometry)) {
                            personIdWichHomeIsInDilutionArea.add(person.getId());
                            break;
                        }
                    }
                }
            }
        System.out.println(personIdWichHomeIsInDilutionArea.size());
            return personIdWichHomeIsInDilutionArea;
        }

    private static Coord getCoord(Activity activity, Network network) {
        if (activity.getCoord() != null) {
            return activity.getCoord();
        }

        return network.getLinks().get(activity.getLinkId()).getCoord();
    }

    private static boolean isInDilutionAreaGeometry(Coord coord, Collection<Geometry> geometry) {
        var transformed = transformation.transform(coord);
        return geometry.stream().anyMatch(g -> g.covers(MGC.coord2Point(transformed)));
    }
    private static Collection<Geometry> getDilutionAreaGeometry(Collection<SimpleFeature> features) {
        return features.stream()
                .filter(feature -> dilutionAreaPlz.contains(feature.getAttribute("plz")))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .collect(Collectors.toSet());
    }
    }
