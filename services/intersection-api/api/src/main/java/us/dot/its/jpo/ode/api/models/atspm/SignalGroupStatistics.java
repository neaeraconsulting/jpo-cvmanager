package us.dot.its.jpo.ode.api.models.atspm;

public record SignalGroupStatistics(
        int signalGroup,
        double percentGreenPaired,
        double percentYellowPaired,
        double percentRedPaired,
        double percentAllPaired) {
}
