package org.matsim.velbert.analysis;

import org.apache.commons.csv.CSVFormat;
import org.matsim.core.events.EventsUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.stream.Collectors;

public class RunMainModeHandler {

    public static void main(String[] args) {

        var manager = EventsUtils.createEventsManager();
        var handler = new MainModeHandlerWithFilter();
        manager.addHandler(handler);
        EventsUtils.readEvents(manager, "C:\\Users\\tekuh\\OneDrive\\Master\\Matsim\\output-NEU\\005\\velbert-matsim-1pct-calibration-005.output_events.xml.gz");

        var personTrips = handler.getPersonTrips();
        var modes = personTrips.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(mode -> mode, mode -> 1, Integer::sum));

        var totalTrips = modes.values().stream()
                .mapToDouble(d -> d)
                .sum();

        try (var writer = Files.newBufferedWriter(Paths.get("C:\\Users\\tekuh\\OneDrive\\Desktop\\modes.csv")); var printer = CSVFormat.DEFAULT.withDelimiter(',').withHeader("Mode", "Count", "Share").print(writer)) {

            for (var entry : modes.entrySet()) {
                printer.printRecord(entry.getKey(), entry.getValue(), entry.getValue() / totalTrips);
            }

            printer.printRecord("total", totalTrips, 1.0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}