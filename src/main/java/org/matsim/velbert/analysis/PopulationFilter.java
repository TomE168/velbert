package org.matsim.velbert.analysis;

import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
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
    private static final String populationFile = "C:\\Users\\tekuh\\OneDrive\\Master\\Matsim\\output-NEU\\005\\velbert-matsim-1pct-calibration-005.output_plans.xml.gz";
    private static final String networkFile = "C:\\Users\\tekuh\\OneDrive\\Master\\Matsim\\output-NEU\\005\\velbert-matsim-1pct-calibration-005.output_network.xml.gz";
    private static final List<String> dilutionAreaPlz = new ArrayList<>(Arrays.asList("42551", "42549", "42555", "42553"));
    public Set<?> getPersonIdWhichHomeIsInDilutionArea(){
        var population = PopulationUtils.readPopulation(populationFile);
        return getPersonIdWhichHomeIsInDilutionArea(population);
    }
    public Set<?> getPersonIdWhichHomeIsInDilutionArea(Population population) {
            var dilutionAreaGeometry = getDilutionAreaGeometry(ShapeFileReader.getAllFeatures(postleitzahlenGebiete));
            Network network = NetworkUtils.readNetwork(networkFile);
            Set<Id<Person>> personIdWhichHomeIsInDilutionArea = new HashSet<>();
            for (var person : population.getPersons().values()) {
                for (var activity : TripStructureUtils.getActivities(person.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities)) {
                    if (activity.getType().contains("home")) {
                        if (isInDilutionAreaGeometry(getCoord(activity, network), dilutionAreaGeometry)) {
                            personIdWhichHomeIsInDilutionArea.add(person.getId());
                            break;
                        }
                    }
                }
            }
            return personIdWhichHomeIsInDilutionArea;
        }

    private static Coord getCoord(Activity activity, Network network) {
        if (activity.getCoord() != null) return activity.getCoord();
        return network.getLinks().get(activity.getLinkId()).getCoord();
    }

    private static boolean isInDilutionAreaGeometry(Coord coord, Collection<Geometry> geometry) {
        var transformed = transformation.transform(coord);
        return geometry.stream().anyMatch(g -> g.covers(MGC.coord2Point(transformed)));
    }
    private static Collection<Geometry> getDilutionAreaGeometry(Collection<SimpleFeature> features) {
        return features.stream()
                .filter(feature -> dilutionAreaPlz.contains((String) feature.getAttribute("plz")))
                .map(feature -> (Geometry) feature.getDefaultGeometry())
                .collect(Collectors.toSet());
    }
    }
