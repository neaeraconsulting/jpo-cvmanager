package us.dot.its.jpo.rsustatusmonitor.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.EqualsAndHashCode;
import lombok.Generated;
import lombok.Getter;
import lombok.Setter;
import us.dot.its.jpo.conflictmonitor.monitor.models.events.Event;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Generated
public class NearestNeighborUnresponsiveEvent extends Event {

    public NearestNeighborUnresponsiveEvent() {
        super("NearestNeighborUnresponsive");
    }

    @JsonIgnore
    public String getKey() {
        return "" + this.getIntersectionID();
    }
}
