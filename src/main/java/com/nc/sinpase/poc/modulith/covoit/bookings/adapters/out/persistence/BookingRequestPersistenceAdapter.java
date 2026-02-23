package com.nc.sinpase.poc.modulith.covoit.bookings.adapters.out.persistence;

import com.nc.sinpase.poc.modulith.covoit.bookings.domain.BookingRequest;
import com.nc.sinpase.poc.modulith.covoit.bookings.domain.BookingRequestRepository;
import com.nc.sinpase.poc.modulith.covoit.bookings.domain.BookingStatus;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
class BookingRequestPersistenceAdapter implements BookingRequestRepository {

    private final SpringDataBookingRequestRepository repo;

    BookingRequestPersistenceAdapter(SpringDataBookingRequestRepository repo) {
        this.repo = repo;
    }

    @Override
    public void save(BookingRequest request) {
        repo.save(toJpa(request));
    }

    @Override
    public Optional<BookingRequest> findById(UUID id) {
        return repo.findById(id).map(this::toDomain);
    }

    @Override
    public List<BookingRequest> findByPassengerId(UUID passengerId) {
        return repo.findByPassengerId(passengerId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<BookingRequest> findByRideId(UUID rideId) {
        return repo.findByRideId(rideId).stream().map(this::toDomain).toList();
    }

    private BookingRequestJpaEntity toJpa(BookingRequest b) {
        BookingRequestJpaEntity e = new BookingRequestJpaEntity();
        e.setId(b.getId());
        e.setRideId(b.getRideId());
        e.setPassengerId(b.getPassengerId());
        e.setStatus(b.getStatus().name());
        e.setRequestedAt(b.getRequestedAt());
        e.setDecidedAt(b.getDecidedAt());
        e.setCanceledAt(b.getCanceledAt());
        e.setVersion(b.getVersion());
        e.setCreatedAt(b.getCreatedAt());
        e.setUpdatedAt(b.getUpdatedAt());
        return e;
    }

    private BookingRequest toDomain(BookingRequestJpaEntity e) {
        return BookingRequest.reconstitute(
                e.getId(), e.getRideId(), e.getPassengerId(),
                BookingStatus.valueOf(e.getStatus()),
                e.getRequestedAt(), e.getDecidedAt(), e.getCanceledAt(),
                e.getVersion(), e.getCreatedAt(), e.getUpdatedAt()
        );
    }
}
