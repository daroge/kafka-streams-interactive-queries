package de.daroge.streams.kafkaqueries.processing.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;
import org.apache.kafka.streams.state.HostInfo;

@Getter
@AllArgsConstructor
public class HostInformation {

    private String host;
    private int port;
    private Set<String> storeNames;

    public boolean isEquivalentTo(HostInfo hostInfo) {
        return hostInfo.host().equals(host) && hostInfo.port() == port;
    }
}