package com.rentalplatform.service;

import com.rentalplatform.dto.BookingDto;
import com.rentalplatform.dto.CreationBookingDto;
import com.rentalplatform.entity.BookingEntity;
import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.exception.BadRequestException;
import com.rentalplatform.exception.NotFoundException;
import com.rentalplatform.mapper.BookingDtoMapper;
import com.rentalplatform.repository.BookingRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.utils.RedisCacheCleaner;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.awt.print.Book;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class BookingService {

    private final UserRepository userRepository;
    private final ListingRepository listingRepository;
    private final BookingRepository bookingRepository;
    private final BookingDtoMapper bookingDtoMapper;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final RedisCacheCleaner redisCacheCleaner;

    @Cacheable(cacheNames = "bookings", key = "#bookingId")
    public BookingDto getBookingById(Long bookingId, String username) {
        BookingEntity booking = findBookingById(bookingId);
        
        if(!username.equals(booking.getTenant().getUsername())) {
            throw new BadRequestException("You are not authorized to view this booking");
        }
        
        return bookingDtoMapper.makeBookingDto(booking);
    }

    @Cacheable(cacheNames = "bookings", key = "#username + '_' + #page + '_' + #size")
    public Page<BookingDto> getBookings(String username, int page, int size) {
        if(size > 50) {
            throw new BadRequestException("Maximum page size is 50");
        }
        PageRequest request = PageRequest.of(page, size);
        Page<BookingEntity> bookings = bookingRepository.findAllByTenantUsernameOrLandlordUsername(username, request);
        return bookings.map(bookingDtoMapper::makeBookingDto);
    }

    @Cacheable(cacheNames = "bookingsForLandlord", key = "#username + '_' + #page + '_' + #size")
    public Page<BookingDto> getBookingsForLandlord(String username, int page, int size) {
        if(size > 50) {
            throw new BadRequestException("Maximum page size is 50");
        }
        PageRequest request = PageRequest.of(page, size);
        Page<BookingEntity> bookings = bookingRepository.findAllByListingLandlordUsername(username, request);
        return bookings.map(bookingDtoMapper::makeBookingDto);
    }

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

        updateNextAvailableDate(listingToBook);

        BookingEntity savedBooking = bookingRepository.save(booking);

        redisCacheCleaner.evictBookingCacheForUser(username);
        redisCacheCleaner.evictBookingCacheForLandlord(listingToBook.getLandlord().getUsername());

        emailService.sendEmail(listingToBook.getLandlord().getEmail(),
                "New Booking Request",
                "A new booking request has been made for your listing '%s'".formatted(listingToBook.getTitle()));

        notificationService.createNotification("A new booking request has been made for your listing '%s'"
                        .formatted(listingToBook.getTitle()),
                listingToBook.getLandlord());

        return bookingDtoMapper.makeBookingDto(savedBooking);
    }

    @Transactional
    public BookingDto confirmBookingByLandlord(Long bookingId, String username) {
        BookingEntity booking = findBookingById(bookingId);

        if (!booking.getListing().getLandlord().getUsername().equals(username)) {
            throw new BadRequestException("You are not authorized to confirm this booking");
        }

        validateBookingStatus(booking, "Only bookings with status 'PENDING' can be confirmed");

        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        redisCacheCleaner.evictCacheByPattern("bookings::" + bookingId);
        redisCacheCleaner.evictBookingCacheForUser(booking.getTenant().getUsername());
        redisCacheCleaner.evictBookingCacheForLandlord(booking.getListing().getLandlord().getUsername());

        emailService.sendEmail(booking.getTenant().getEmail(),
                "Booking Confirmed",
                "Your booking for listing '%s' has been confirmed".formatted(booking.getListing().getTitle()));

        notificationService.createNotification("Your booking for listing '%s' has been confirmed"
                        .formatted(booking.getListing().getTitle()),
                booking.getTenant());

        return bookingDtoMapper.makeBookingDto(booking);
    }

    @Transactional
    public BookingDto cancelBookingByTenant(Long bookingId, String username) {
        BookingEntity booking = findBookingById(bookingId);

        if (booking.getStartDate().isBefore(Instant.now())) {
            throw new BadRequestException("You cannot cancel a booking already that has already started");
        }

        booking.setStatus(BookingStatus.CANCELLED);

        updateNextAvailableDate(booking.getListing());

        bookingRepository.save(booking);

        redisCacheCleaner.evictCacheByPattern("bookings::" + bookingId);
        redisCacheCleaner.evictBookingCacheForUser(username);
        redisCacheCleaner.evictBookingCacheForLandlord(booking.getListing().getLandlord().getUsername());

        emailService.sendEmail(booking.getListing().getLandlord().getEmail(),
                "Booking Canceled",
                "User has been canceled the booking for listing '%s'".formatted(booking.getListing().getTitle()));

        notificationService.createNotification("User has been canceled the booking for listing '%s'"
                        .formatted(booking.getListing().getTitle()),
                booking.getListing().getLandlord());

        return bookingDtoMapper.makeBookingDto(booking);
    }

    @Transactional
    public BookingDto declineBookingByLandlord(Long bookingId, String username) {
        BookingEntity booking = findBookingById(bookingId);

        validateBookingStatus(booking, "Only bookings with status 'PENDING' can be declined");

        booking.setStatus(BookingStatus.CANCELLED);

        bookingRepository.save(booking);

        redisCacheCleaner.evictCacheByPattern("bookings::" + bookingId);
        redisCacheCleaner.evictBookingCacheForUser(booking.getTenant().getUsername());
        redisCacheCleaner.evictBookingCacheForLandlord(booking.getListing().getLandlord().getUsername());

        emailService.sendEmail(booking.getTenant().getEmail(),
                "Booking Declined",
                "The landlord has declined booking for the listing '%s'".formatted(booking.getListing().getTitle()));

        notificationService.createNotification("The landlord has declined booking for the listing '%s'"
                        .formatted(booking.getListing().getTitle()),
                booking.getTenant());

        return bookingDtoMapper.makeBookingDto(booking);
    }

    private void updateNextAvailableDate(ListingEntity listing) {
        Optional<Instant> maxEndDate = bookingRepository.findMaxEndDateByListingIdAndStatus(
                listing.getId(),
                BookingStatus.CONFIRMED);
        if(maxEndDate.isPresent()) {
            listing.setNextAvailableDate(maxEndDate.get().plusSeconds(3600));
        } else {
            listing.setNextAvailableDate(Instant.now());
        }
    }

    private void validateBookingStatus(BookingEntity booking, String errorMessage) {
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException(errorMessage);
        }
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

    private BookingEntity findBookingById(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking with id '%d' not found".formatted(bookingId)));
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
