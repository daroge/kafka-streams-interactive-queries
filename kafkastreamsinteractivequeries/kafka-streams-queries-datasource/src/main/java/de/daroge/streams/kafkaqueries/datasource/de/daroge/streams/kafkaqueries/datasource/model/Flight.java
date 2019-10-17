package de.daroge.streams.kafkaqueries.datasource.de.daroge.streams.kafkaqueries.datasource.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import de.daroge.streams.kafkaqueries.common.model.FlightState;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Flight {

    @JsonProperty("states")
    private List<FlightState> flightStates;
}
