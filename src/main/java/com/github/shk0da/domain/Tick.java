package com.github.shk0da.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "ticks")
@IdClass(Tick.PK.class)
public class Tick implements Serializable {

    @Data
    @Embeddable
    @EqualsAndHashCode
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {
        private Timestamp datetime;
        private String symbol;
        private Integer timeFrame;
    }

    @Id
    @NotNull
    @Column(name = "datetime", columnDefinition = "timestamp default now()")
    private Timestamp datetime;

    @Id
    @NotNull
    @Column(name = "symbol", columnDefinition = "varchar(10)")
    private String symbol;

    @Id
    @NotNull
    @Column(name = "time_frame")
    private Integer timeFrame;

    @NotNull
    @Column(name = "open")
    private Double open;

    @NotNull
    @Column(name = "max")
    private Double max;

    @NotNull
    @Column(name = "min")
    private Double min;

    @NotNull
    @Column(name = "close")
    private Double close;

    @NotNull
    @Column(name = "value")
    private Double value;

    @JsonIgnore
    @Column(name = "normalization")
    private Double normalization;
}
