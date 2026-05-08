package us.dot.its.jpo.ode.api.models.atspm;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Document("CmAtspmSpatPairLog")
public class AtspmSpatPairLog {
    /**
     * ATSPM Route ID
     */
    private int routeId;

    /**
     * ATSPM Signal ID
     */
    private String signalId;

    /**
     * Intersection ID from SPAT
     */
    private int intersectionId;

    /**
     * Start time of batch period
     */
    private Instant startTime;

    /**
     * End time of batch period
     */
    private Instant endTime;

    /**
     * ATSMP-SPAT event pairs
     */
    private List<AtspmSpatPair> atspmSpatPairs;

    /**
     * Error message, or null of no errors
     */
    private String error;

    /**
     * Percentage of ATSPM events that were paired with a SPAT signal indication
     * event
     */
    private double percentPaired;

    /**
     * Percentage of ATSPM events that were paired with a SPAT GREEN signal
     * indication event
     */
    private double percentGreenPaired;

    /**
     * Percentage of ATSPM events that were paired with a SPAT RED signal indication
     * event
     */
    private double percentRedPaired;
    /**
     * Percentage of ATSPM events that were paired with a SPAT YELLOW signal
     * indication event
     */
    private double percentYellowPaired;

    /**
     * Overall Statistics of the Signal Group
     */
    private AtspmSpatStatistics signalGroupStatistics;

    // @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    // @AccessType(AccessType.Type.PROPERTY)
    // public double getPercentPaired() {
    // if (atspmSpatPairs == null)
    // return 0;
    // double numPaired =
    // atspmSpatPairs.stream().filter(AtspmSpatPair::isPaired).count();
    // double total = atspmSpatPairs.size();
    // if (total == 0)
    // return 0;
    // return (numPaired / total) * 100.0;
    // }
    // // public void setPercentPaired(double percentPaired) {}

    // @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    // @AccessType(AccessType.Type.PROPERTY)
    // public double getPercentGreenPaired() {
    // return percentPaired(SpatSignalIndication.GREEN);
    // }
    // // public void setPercentGreenPaired(double percentGreenPaired) {}

    // @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    // @AccessType(AccessType.Type.PROPERTY)
    // public double getPercentRedPaired() {
    // return percentPaired(SpatSignalIndication.RED);
    // }
    // // public void setPercentRedPaired(double percentRedPaired) {}

    // @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    // @AccessType(AccessType.Type.PROPERTY)
    // public double getPercentYellowPaired() {
    // return percentPaired(SpatSignalIndication.YELLOW);
    // }
    // // public void setPercentYellowPaired(double percentYellowPaired) {}

    // private double percentPaired(final SpatSignalIndication indication) {
    // if (atspmSpatPairs == null)
    // return 0;
    // double numPaired = atspmSpatPairs.stream().filter(AtspmSpatPair::isPaired)
    // .filter(pair -> pair.getSpatIndication() == indication).count();
    // double total = atspmSpatPairs.stream().filter(pair ->
    // pair.getSpatIndication() == indication).count();
    // if (total == 0)
    // return 0;
    // return (numPaired / total) * 100.0;
    // }

    // private double percentPaired(final SpatSignalIndication indication, final int
    // signalGroup) {
    // if (atspmSpatPairs == null)
    // return 0;
    // double numPaired = atspmSpatPairs.stream()
    // .filter(pair -> pair.getSpatSignalGroupId() == signalGroup)
    // .filter(AtspmSpatPair::isPaired)
    // .filter(pair -> pair.getSpatIndication() == indication)
    // .count();
    // double total = atspmSpatPairs.stream()
    // .filter(pair -> pair.getSpatSignalGroupId() == signalGroup)
    // .filter(pair -> pair.getSpatIndication() == indication)
    // .count();
    // if (total == 0)
    // return 0;
    // return (numPaired / total) * 100.0;
    // }

    // private double percentPaired(final int signalGroup) {
    // if (atspmSpatPairs == null)
    // return 0;
    // double numPaired = atspmSpatPairs.stream()
    // .filter(pair -> pair.getSpatSignalGroupId() == signalGroup)
    // .filter(AtspmSpatPair::isPaired)
    // .count();
    // double total = atspmSpatPairs.stream()
    // .filter(pair -> pair.getSpatSignalGroupId() == signalGroup)
    // .count();
    // if (total == 0)
    // return 0;
    // return (numPaired / total) * 100.0;
    // }

    // private Set<Integer> uniqueSignalGroups() {
    // if (atspmSpatPairs == null)
    // return Collections.emptySet();
    // return
    // atspmSpatPairs.stream().map(AtspmSpatPair::getSpatSignalGroupId).collect(Collectors.toUnmodifiableSet());
    // }

    // @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    // @AccessType(AccessType.Type.PROPERTY)
    // public AtspmSpatStatistics getSignalGroupStatistics() {
    // Set<Integer> uniqueSignalGroups = uniqueSignalGroups();
    // AtspmSpatStatistics stats = new AtspmSpatStatistics();
    // for (Integer signalGroupId : uniqueSignalGroups) {
    // double green = percentPaired(SpatSignalIndication.GREEN, signalGroupId);
    // double yellow = percentPaired(SpatSignalIndication.YELLOW, signalGroupId);
    // double red = percentPaired(SpatSignalIndication.RED, signalGroupId);
    // double all = percentPaired(signalGroupId);
    // SignalGroupStatistics sgStats = new SignalGroupStatistics(signalGroupId,
    // green, yellow, red, all);
    // stats.put(signalGroupId, sgStats);
    // }
    // return stats;
    // }
    // public void setSignalGroupStatistics(AtspmSpatStatistics stats) {}

}
