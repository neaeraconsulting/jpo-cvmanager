package us.dot.its.jpo.rsustatusmonitor.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import us.dot.its.jpo.conflictmonitor.monitor.models.notifications.Notification;

/**
 * Notification representing a NearestNeighborUnresponsiveEvent
 */
public class NearestNeighborUnresponsiveNotification extends Notification {

    /**
     * Constructs a SignalStateConflictNotification with the notification type set.
     */
    public NearestNeighborUnresponsiveNotification() {
        super("NearestNeighborUnresponsive");
    }

    /** The associated SignalStateConflictEvent for this notification. */
    @Getter
    private NearestNeighborUnresponsiveEvent event;

    /**
     * Sets the event for this notification and updates intersection and regulator
     * IDs.
     *
     * @param event the SignalStateConflictEvent to associate with this notification
     */
    public void setEvent(NearestNeighborUnresponsiveEvent event) {
        if (event != null) {
            this.event = event;
            this.setIntersectionID(event.getIntersectionID());
            this.setRoadRegulatorID(event.getRoadRegulatorID());
            this.key = getUniqueId();
        }
    }

    /**
     * Returns a unique identifier for this notification, based on type, conflicting
     * signal groups, intersection ID, and regulator ID.
     *
     * @return unique identifier string for this notification
     */
    @Override
    @JsonIgnore
    public String getUniqueId() {
        return String.format("%s_%s",
                this.getNotificationType(),
                event.getIntersectionID());
    }
}