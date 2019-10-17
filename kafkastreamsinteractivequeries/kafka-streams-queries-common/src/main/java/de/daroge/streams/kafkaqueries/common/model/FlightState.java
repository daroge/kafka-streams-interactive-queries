package de.daroge.streams.kafkaqueries.common.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonFormat(shape = JsonFormat.Shape.ARRAY)
public class FlightState {

    private String icao24;
    private String callsign;
    private String originCountry;
    private Double lastPositionUpdate;
    private Double lastContact;
    private Double longitude;
    private Double latitude;
    private Double geoAltitude;
    private boolean onGround;
    private Double velocity;
    private Double heading;
    private Double verticalRate;
    private Integer serials;
    private Double baroAltitude;
    private String squawk;
    private boolean spi;
    private int positionSource;
}