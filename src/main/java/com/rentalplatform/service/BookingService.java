package com.rentalplatform.service;

import com.rentalplatform.dto.BookingDto;
import com.rentalplatform.dto.CreationBookingDto;
import com.rentalplatform.entity.BookingEntity;
import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.factory.BookingDtoFactory;
import com.rentalplatform.repository.BookingRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RequiredArgsConstructor
@Service
public class BookingService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;
    private final BookingDtoFactory bookingDtoFactory;

    public Page<BookingDto> getBookings(String username, int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        Page<BookingEntity> bookings = bookingRepository.findAllByTenantUsernameOrLandlordUsername(username, request);
        return bookings.map(bookingDtoFactory::makeBookingDto);
    }

    public Page<BookingDto> getBookingsForLandlord(String username, int page, int size) {
        PageRequest request = PageRequest.of(page, size);
        Page<BookingEntity> bookings = bookingRepository.findAllByListingLandlordUsername(username, request);
        return bookings.map(bookingDtoFactory::makeBookingDto);
    }

    @Transactional
    public BookingDto createBooking(CreationBookingDto bookingDto, String username) {
        Instant startDateTime = bookingDto.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endDateTime = bookingDto.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant();

        ListingEntity listingToBook = findListingByIdFromBookingDto(bookingDto);
        UserEntity user = findUserByUsername(username);

        validateBookingCreation(listingToBook, user, startDateTime, endDateTime);

        BookingEntity booking = BookingEntity.builder()
                .listing(listingToBook)
                .tenant(user)
                .startDate(startDateTime)
                .endDate(endDateTime)
                .status(BookingStatus.PENDING)
                .build();

        BookingEntity savedBooking = bookingRepository.save(booking);

        return bookingDtoFactory.makeBookingDto(savedBooking);
    }

    @Transactional
    public boolean confirmBookingByLandlord(Long bookingId, String username) {
        BookingEntity booking = findBookingById(bookingId);

        if (!booking.getListing().getLandlord().getUsername().equals(username)) {
            throw new BadRequestException("You are not authorized to confirm this booking");
        }

        validateBookingStatus(booking, BookingStatus.PENDING,
                "Only bookings with status 'PENDING' can be declined");

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        return true;
    }

    @Transactional
    public boolean cancelBookingByTenant(Long bookingId, String username) {
        BookingEntity booking = findBookingById(bookingId);

        validateLandlord(username, booking.getListing());

        if (booking.getStartDate().isBefore(Instant.now())) {
            throw new BadRequestException("You cannot cancel a booking already that has already started");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        return true;
    }

    @Transactional
    public boolean declineBookingByLandlord(Long bookingId, String username) {
        BookingEntity booking = findBookingById(bookingId);

        validateLandlord(username, booking.getListing());
        validateBookingStatus(booking, BookingStatus.PENDING,
                "Only bookings with status 'PENDING' can be declined");

        booking.setStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);

        return true;
    }

    private void validateBookingStatus(BookingEntity booking, BookingStatus requiredStatus, String errorMessage) {
        if (booking.getStatus() != requiredStatus) {
            throw new BadRequestException(errorMessage);
        }
    }

    private static void validateLandlord(String username, ListingEntity listing) {
        if (!listing.getLandlord().getUsername().equals(username)) {
            throw new BadRequestException("You are not authorized to cancel this booking");
        }
    }

    private BookingEntity findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking with id '%d' not found".formatted(bookingId)));
    }

    private static void validateBookingCreation(ListingEntity listingToBook, UserEntity user, Instant startDateTime,
                                                Instant endDateTime) {
        if (listingToBook.getLandlord().getUsername().equals(user.getUsername())) {
            throw new BadRequestException("You cannot book your own listing");
        }

        if (startDateTime.isBefore(Instant.now().truncatedTo(ChronoUnit.DAYS))) {
            throw new BadRequestException("The booking start date cannot be in the past");
        }

        if (!startDateTime.isBefore(endDateTime)) {
            throw new BadRequestException("The booking start date must be before the end date");
        }

        if (!listingToBook.getBookings().isEmpty()) {
            BookingEntity lastBooking = listingToBook.getBookings().get(listingToBook.getBookings().size() - 1);
            if (startDateTime.isBefore(lastBooking.getEndDate())) {
                throw new BadRequestException(
                        "The booking start date is only available after: " + lastBooking.getEndDate());
            }
        }
    }

    private UserEntity findUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User '%s' not found".formatted(username)));
    }

    private ListingEntity findListingByIdFromBookingDto(CreationBookingDto bookingDto) {
        return listingRepository.findById(bookingDto.getListingId())
                .orElseThrow(() -> new NotFoundException("Listing with id '%d' not found".
                        formatted(bookingDto.getListingId())));
    }
}
