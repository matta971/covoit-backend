package com.nc.sinpase.poc.modulith.covoit.rides.adapters.out.persistence;

import com.nc.sinpase.poc.modulith.covoit.rides.domain.Ride;
import com.nc.sinpase.poc.modulith.covoit.rides.domain.RideRepository;
import com.nc.sinpase.poc.modulith.covoit.rides.domain.RideStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class RidePersistenceAdapter implements RideRepository {

    private final SpringDataRideRepository repo;

    RidePersistenceAdapter(SpringDataRideRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(Ride ride) {
        repo.save(toJpa(ride));
    }

    @Override
    public Optional<Ride> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Ride> search(String from, String to, Instant dateFrom, Instant dateTo) {
        Specification<RideJpaEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "SCHEDULED"));
            if (from != null && !from.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("fromLocation")), "%" + from.toLowerCase() + "%"));
            }
            if (to != null && !to.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("toLocation")), "%" + to.toLowerCase() + "%"));
            }
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("departureTime"), dateFrom));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThan(root.get("departureTime"), dateTo));
            }
            if (query != null) query.orderBy(cb.asc(root.get("departureTime")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return repo.findAll(spec).stream().map(this::toDomain).toList();
    }

    @Override
    public List<Ride> findByDriverId(UUID driverId) {
        return repo.findByDriverId(driverId).stream().map(this::toDomain).toList();
    }

    private RideJpaEntity toJpa(Ride ride) {
        RideJpaEntity e = new RideJpaEntity();
        e.setId(ride.getId());
        e.setDriverId(ride.getDriverId());
        e.setFromLocation(ride.getFromLocation());
        e.setToLocation(ride.getToLocation());
        e.setDepartureTime(ride.getDepartureTime());
        e.setTotalSeats(ride.getTotalSeats());
        e.setAvailableSeats(ride.getAvailableSeats());
        e.setStatus(ride.getStatus().name());
        e.setVersion(ride.getVersion());
        e.setCreatedAt(ride.getCreatedAt());
        e.setUpdatedAt(ride.getUpdatedAt());
        return e;
    }

    private Ride toDomain(RideJpaEntity e) {
        return Ride.reconstitute(
                e.getId(), e.getDriverId(), e.getFromLocation(), e.getToLocation(),
                e.getDepartureTime(), e.getTotalSeats(), e.getAvailableSeats(),
                RideStatus.valueOf(e.getStatus()), e.getVersion(),
                e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
