package us.dot.its.jpo.rsustatusmonitor.models.postgres.tables;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@EqualsAndHashCode
@Getter
@Entity
@Table(name = "rsu_nn_monitoring")
public class RsuNnMonitoring {
    @Id
    private int rsu_nn_monitoring_id;
    private int rsu_id;
    private boolean active;
}
