package com.nc.sinpase.poc.modulith.covoit.events.adapters.out;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "events")
@Getter
@Setter
public class EventJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(updatable = false, nullable = false)
    private Instant created_at;

    protected EventJpaEntity() {}

    public EventJpaEntity(String type, String payload, Instant created_at) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.payload = payload;
        this.created_at = created_at;
    }

}