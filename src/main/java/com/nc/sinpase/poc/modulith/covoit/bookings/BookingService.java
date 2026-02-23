package com.nc.sinpase.poc.modulith.covoit.bookings;

import com.nc.sinpase.poc.modulith.covoit.ConcurrentUpdateException;
import com.nc.sinpase.poc.modulith.covoit.ForbiddenException;
import com.nc.sinpase.poc.modulith.covoit.bookings.domain.BookingRequest;
import com.nc.sinpase.poc.modulith.covoit.bookings.domain.BookingRequestRepository;
import com.nc.sinpase.poc.modulith.covoit.bookings.domain.BookingStatus;
import com.nc.sinpase.poc.modulith.covoit.rides.RideCapacityPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    private final BookingRequestRepository bookingRepository;
    private final RideCapacityPort rideCapacityPort;
    private final ApplicationEventPublisher eventPublisher;

    public BookingService(BookingRequestRepository bookingRepository,
                          RideCapacityPort rideCapacityPort,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.rideCapacityPort = rideCapacityPort;
        this.eventPublisher = eventPublisher;
    }

    public BookingRequestView requestBooking(UUID rideId, UUID passengerId) {
        UUID driverId = rideCapacityPort.getDriverId(rideId);
        if (driverId.equals(passengerId)) {
            throw new ForbiddenException("A driver cannot book their own ride");
        }
        rideCapacityPort.assertRideBookable(rideId);

        BookingRequest booking = BookingRequest.request(rideId, passengerId);
        save(booking);
        eventPublisher.publishEvent(new BookingRequestedEvent(booking.getId(), rideId, passengerId));
        return toView(booking);
    }

    public void accept(UUID bookingId, UUID byDriverId) {
        BookingRequest booking = load(bookingId);

        UUID rideDriverId = rideCapacityPort.getDriverId(booking.getRideId());
        if (!rideDriverId.equals(byDriverId)) {
            throw new ForbiddenException("Only the driver can accept a booking request");
        }

        booking.accept();
        rideCapacityPort.reserveSeat(booking.getRideId(), booking.getPassengerId());
        save(booking);
        eventPublisher.publishEvent(new BookingAcceptedEvent(booking.getId(), booking.getRideId(), booking.getPassengerId()));
    }

    public void reject(UUID bookingId, UUID byDriverId) {
        BookingRequest booking = load(bookingId);

        UUID rideDriverId = rideCapacityPort.getDriverId(booking.getRideId());
        if (!rideDriverId.equals(byDriverId)) {
            throw new ForbiddenException("Only the driver can reject a booking request");
        }

        booking.reject();
        save(booking);
        eventPublisher.publishEvent(new BookingRejectedEvent(booking.getId(), booking.getRideId(), booking.getPassengerId()));
    }

    public void cancel(UUID bookingId, UUID byPassengerId) {
        BookingRequest booking = load(bookingId);
        boolean wasAccepted = booking.getStatus() == BookingStatus.ACCEPTED;

        booking.cancel(byPassengerId);

        if (wasAccepted) {
            rideCapacityPort.releaseSeat(booking.getRideId());
        }
        save(booking);
        eventPublisher.publishEvent(new BookingCanceledEvent(booking.getId(), booking.getRideId(), booking.getPassengerId(), wasAccepted));
    }

    @Transactional(readOnly = true)
    public List<BookingRequestView> findByPassenger(UUID passengerId) {
        return bookingRepository.findByPassengerId(passengerId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public List<BookingRequestView> findByDriver(UUID driverId) {
        List<UUID> rideIds = rideCapacityPort.findRideIdsByDriver(driverId);
        return rideIds.stream()
                .flatMap(rideId -> bookingRepository.findByRideId(rideId).stream())
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BookingRequestView> findByRide(UUID rideId) {
        return bookingRepository.findByRideId(rideId).stream().map(this::toView).toList();
    }

    private BookingRequest load(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(bookingId));
    }

    private void save(BookingRequest booking) {
        try {
            bookingRepository.save(booking);
        } catch (OptimisticLockingFailureException e) {
            throw new ConcurrentUpdateException();
        }
    }

    private BookingRequestView toView(BookingRequest b) {
        return new BookingRequestView(
                b.getId(), b.getRideId(), b.getPassengerId(),
                b.getStatus().name(), b.getRequestedAt(), b.getDecidedAt(), b.getCanceledAt()
        );
    }
}
