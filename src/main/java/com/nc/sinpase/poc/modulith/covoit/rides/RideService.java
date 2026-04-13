package com.nc.sinpase.poc.modulith.covoit.rides;

import com.nc.sinpase.poc.modulith.covoit.ConcurrentUpdateException;
import com.nc.sinpase.poc.modulith.covoit.rides.adapters.in.rest.UpdateRideRequest;
import com.nc.sinpase.poc.modulith.covoit.rides.domain.Ride;
import com.nc.sinpase.poc.modulith.covoit.rides.domain.RideRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RideService implements RideCapacityPort {

    private final RideRepository rideRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RideService(RideRepository rideRepository, ApplicationEventPublisher eventPublisher) {
        this.rideRepository = rideRepository;
        this.eventPublisher = eventPublisher;
    }

    public RideView publish(PublishRideCommand command) {
        Ride ride = Ride.publish(command.driverId(), command.from(), command.to(),
                command.departureTime(), command.totalSeats());
        rideRepository.save(ride);
        eventPublisher.publishEvent(new RidePublishedEvent(
                ride.getId(), ride.getDriverId(), ride.getFromLocation(),
                ride.getToLocation(), ride.getDepartureTime()));

        //throw new RuntimeException("💥 Simulated crash after save");

       return toView(ride);
    }

    @Transactional(readOnly = true)
    public List<RideView> search(String from, String to, LocalDate date) {
        Instant dateFrom = date != null ? date.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant dateTo = date != null ? date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        return rideRepository.search(from, to, dateFrom, dateTo).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public RideView findById(UUID rideId) {
        return rideRepository.findById(rideId)
                .map(this::toView)
                .orElseThrow(() -> new RideNotFoundException(rideId));
    }

    @Transactional(readOnly = true)
    public List<RideView> findByDriver(UUID driverId) {
        return rideRepository.findByDriverId(driverId).stream().map(this::toView).toList();
    }

    public void cancel(UUID rideId, UUID byDriverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
        ride.cancel(byDriverId);
        save(ride);
        eventPublisher.publishEvent(new RideCanceledEvent(ride.getId(), ride.getDriverId()));
    }

    public RideView update(UUID rideId, UUID driverId, UpdateRideRequest request) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
        
        // Vérifier que le driver actuel est le propriétaire du ride (Horizontal Escalation Protection)
        if (!ride.getDriverId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own rides");
        }
        
        ride.update(request.from(), request.to(), request.totalSeats());
        save(ride);
        return toView(ride);
    }

    public void delete(UUID rideId, UUID driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
        
        // Vérifier que le driver actuel est le propriétaire du ride (Horizontal Escalation Protection)
        if (!ride.getDriverId().equals(driverId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own rides");
        }
        
        // Soft delete - marquer comme CANCELED au lieu de vraiment supprimer
        ride.cancel(driverId);
        save(ride);
        eventPublisher.publishEvent(new RideDeletedEvent(ride.getId(), ride.getDriverId()));
    }

    // --- RideCapacityPort ---

    @Override
    public void reserveSeat(UUID rideId, UUID passengerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
        ride.reserveSeat();
        save(ride);
    }

    @Override
    public void releaseSeat(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
        ride.releaseSeat();
        save(ride);
    }

    @Override
    public void assertRideBookable(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
        ride.assertBookable();
    }

    @Override
    @Transactional(readOnly = true)
    public UUID getDriverId(UUID rideId) {
        return rideRepository.findById(rideId)
                .map(Ride::getDriverId)
                .orElseThrow(() -> new RideNotFoundException(rideId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findRideIdsByDriver(UUID driverId) {
        return rideRepository.findByDriverId(driverId).stream().map(Ride::getId).toList();
    }

    private void save(Ride ride) {
        try {
            rideRepository.save(ride);
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentUpdateException();
        }
    }

    private RideView toView(Ride ride) {
        return new RideView(
                ride.getId(), ride.getDriverId(),
                ride.getFromLocation(), ride.getToLocation(),
                ride.getDepartureTime(), ride.getTotalSeats(),
                ride.getAvailableSeats(), ride.getStatus().name()
        );
    }
}
