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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingDtoMapper bookingDtoMapper;

    @Mock
    private RedisCacheCleaner redisCacheCleaner;

    @Mock
    private NotificationService notificationService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private BookingService bookingService;

    @Test
    void testGetBookingById_Success() {
        Long bookingId = 1L;
        String username = "Test Username";
        UserEntity user = new UserEntity();
        user.setUsername(username);

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .tenant(user)
                .build();

        BookingDto expected = BookingDto.builder()
                .id(1L)
                .tenantUsername(username)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));
        when(bookingDtoMapper.makeBookingDto(booking)).thenReturn(expected);

        BookingDto result = bookingService.getBookingById(bookingId, username);

        assertNotNull(result);
        verify(bookingRepository, times(1)).findById(bookingId);
        verify(bookingDtoMapper, times(1)).makeBookingDto(booking);
    }

    @Test
    void testGetBookingById_WhenBookingNotFound_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "Test Username";

        when(bookingRepository.findById(bookingId)).thenThrow(
                new NotFoundException("Booking not found"));

        assertThrows(NotFoundException.class, () -> bookingService.getBookingById(bookingId, username));
    }

    @Test
    void testBookingById_WhenUnauthorized_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "Unauthorized User";
        UserEntity user = new UserEntity();
        user.setUsername("tenantUser");

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .tenant(user)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        assertThrows(BadRequestException.class, () -> bookingService.getBookingById(bookingId, username));
    }

    @Test
    void testGetBookings_Success() {
        String username = "Test Username";
        int page = 0;
        int size = 10;
        UserEntity user = new UserEntity();
        user.setUsername(username);
        PageRequest request = PageRequest.of(page, size);

        BookingEntity booking = BookingEntity.builder()
                .id(1L)
                .tenant(user)
                .build();

        BookingDto bookingDto = BookingDto.builder()
                .id(1L)
                .tenantUsername(username)
                .build();

        Page<BookingEntity> bookingPage = new PageImpl<>(List.of(booking), request, 1);

        when(bookingRepository.findAllByTenantUsernameOrLandlordUsername(username, request))
                .thenReturn(bookingPage);
        when(bookingDtoMapper.makeBookingDto(booking)).thenReturn(bookingDto);

        Page<BookingDto> result = bookingService.getBookings(username, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(bookingRepository, times(1))
                .findAllByTenantUsernameOrLandlordUsername(username, request);
    }

    @Test
    void testGetBookings_WhenNoBookings_ShouldReturnEmptyList() {
        String username = "Test Username";
        int page = 0;
        int size = 10;
        PageRequest request = PageRequest.of(page, size);

        Page<BookingEntity> emptyPage = new PageImpl<>(Collections.emptyList(), request, 0);

        when(bookingRepository.findAllByTenantUsernameOrLandlordUsername(username, request))
                .thenReturn(emptyPage);

        Page<BookingDto> result = bookingService.getBookings(username, page, size);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(bookingRepository, times(1))
                .findAllByTenantUsernameOrLandlordUsername(username, request);
    }

    @Test
    void testGetBookings_WhenSizeGreaterThan50_ShouldThrowException() {
        String username = "Test Username";
        int page = 0;
        int size = 51;

        assertThrows(BadRequestException.class, () -> bookingService.getBookings(username, page, size));
    }

    @Test
    void testGetBookingsForLandlord_Success() {
        String username = "Test Username";
        int page = 0;
        int size = 10;
        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);
        PageRequest request = PageRequest.of(page, size);

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(landlord)
                .build();

        BookingEntity booking = BookingEntity.builder()
                .id(1L)
                .listing(listing)
                .build();

        BookingDto bookingDto = BookingDto.builder()
                .id(1L)
                .build();

        Page<BookingEntity> bookingPage = new PageImpl<>(List.of(booking), request, 1);

        when(bookingRepository.findAllByListingLandlordUsername(username, request)).thenReturn(bookingPage);
        when(bookingDtoMapper.makeBookingDto(booking)).thenReturn(bookingDto);

        Page<BookingDto> result = bookingService.getBookingsForLandlord(username, page, size);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        verify(bookingRepository, times(1)).findAllByListingLandlordUsername(username, request);
    }

    @Test
    void testGetBookingsForLandlord_WhenNoBookings_ShouldReturnEmptyList() {
        String username = "Test Username";
        int page = 0;
        int size = 10;
        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);
        PageRequest request = PageRequest.of(page, size);
        Page<BookingEntity> emptyPage = new PageImpl<>(Collections.emptyList(), request, 0);

        when(bookingRepository.findAllByListingLandlordUsername(username, request)).thenReturn(emptyPage);

        Page<BookingDto> result = bookingService.getBookingsForLandlord(username, page, size);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        verify(bookingRepository, times(1)).findAllByListingLandlordUsername(username, request);
    }

    @Test
    void testGetBookingsForLandlord_WhenSizeGreaterThan50_ShouldThrowException() {
        String username = "Test Username";
        int page = 0;
        int size = 51;

        assertThrows(BadRequestException.class, () -> bookingService.getBookingsForLandlord(username, page, size));
    }

    @Test
    void testCreateBooking_Success() {
        String username = "Test Username";
        UserEntity currentUser = new UserEntity();
        currentUser.setUsername(username);

        UserEntity landlord = UserEntity.builder()
                .id(1L)
                .username("Landlord")
                .email("testEmail@gmail.com")
                .build();

        CreationBookingDto creationBookingDto = CreationBookingDto.builder()
                .listingId(1L)
                .startDate(LocalDate.of(2025, 3, 10))
                .endDate(LocalDate.of(2025, 5, 10))
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(landlord)
                .build();

        BookingEntity booking = BookingEntity.builder()
                .id(1L)
                .listing(listing)
                .build();

        BookingDto bookingDto = BookingDto.builder()
                .id(1L)
                .status(BookingStatus.PENDING)
                .build();

        when(listingRepository.findById(creationBookingDto.getListingId())).thenReturn(Optional.of(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(booking);
        when(bookingDtoMapper.makeBookingDto(booking)).thenReturn(bookingDto);
        when(bookingRepository.findMaxEndDateByListingIdAndStatus(listing.getId(), BookingStatus.CONFIRMED))
                .thenReturn(Optional.empty());

        BookingDto result = bookingService.createBooking(creationBookingDto, username);

        assertNotNull(result);
        assertEquals(bookingDto.getId(), result.getId());
        assertEquals(bookingDto.getStatus(), result.getStatus());

        verify(bookingRepository, times(1)).save(any(BookingEntity.class));
        verify(redisCacheCleaner, times(1)).evictBookingCacheForUser(username);
        verify(redisCacheCleaner, times(1)).evictBookingCacheForLandlord(listing.getLandlord().getUsername());
        verify(emailService, times(1)).sendEmail(
                eq(landlord.getEmail()),
                eq("New Booking Request"),
                contains("A new booking request has been made for your listing")
        );
        verify(notificationService, times(1)).createNotification(
                contains("A new booking request has been made for your listing"),
                eq(landlord)
        );
    }

    @Test
    void testCreateBooking_WhenListingNotFound_ShouldThrowException() {
        String username = "Test Username";
        CreationBookingDto creationBookingDto = CreationBookingDto.builder()
                .listingId(1L)
                .startDate(LocalDate.of(2025, 3, 10))
                .endDate(LocalDate.of(2025, 5, 10))
                .build();

        when(listingRepository.findById(creationBookingDto.getListingId())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.createBooking(creationBookingDto, username));
    }

    @Test
    void testCreateBooking_WhenUserNotFound_ShouldThrowException() {
        String username = "Test Username";
        CreationBookingDto creationBookingDto = CreationBookingDto.builder()
                .listingId(1L)
                .startDate(LocalDate.of(2025, 3, 10))
                .endDate(LocalDate.of(2025, 5, 10))
                .build();

        when(listingRepository.findById(creationBookingDto.getListingId())).thenReturn(Optional.of(new ListingEntity()));
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.createBooking(creationBookingDto, username));
    }

    @Test
    void testCreateBooking_WhenBookingOwnListing_ShouldThrowException() {
        String username = "Test Username";
        UserEntity user = new UserEntity();
        user.setUsername(username);

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(user)
                .build();

        CreationBookingDto creationBookingDto = CreationBookingDto.builder()
                .listingId(1L)
                .startDate(LocalDate.of(2025, 3, 10))
                .endDate(LocalDate.of(2025, 5, 10))
                .build();

        when(listingRepository.findById(creationBookingDto.getListingId())).thenReturn(Optional.ofNullable(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                bookingService.createBooking(creationBookingDto, username));
        assertEquals("You cannot book your own listing", exception.getMessage());
    }

    @Test
    void testCreateBooking_WhenBookingStartDateInPast_ShouldThrowException() {
        String username = "Tenant Username";
        UserEntity currentUser = new UserEntity();
        currentUser.setUsername(username);

        UserEntity landlord = new UserEntity();
        landlord.setUsername("Landlord Username");

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(landlord)
                .build();

        CreationBookingDto creationBookingDto = CreationBookingDto.builder()
                .listingId(1L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .build();

        when(listingRepository.findById(creationBookingDto.getListingId())).thenReturn(Optional.ofNullable(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                bookingService.createBooking(creationBookingDto, username));

        assertEquals("The booking start date cannot be in the past", exception.getMessage());
    }

    @Test
    void testCreateBooking_WhenBookingStartDateAfterEndDate_ShouldThrowException() {
        String username = "Tenant Username";
        UserEntity currentUser = new UserEntity();
        currentUser.setUsername(username);

        UserEntity landlord = new UserEntity();
        landlord.setUsername("Landlord Username");

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(landlord)
                .build();

        CreationBookingDto creationBookingDto = CreationBookingDto.builder()
                .listingId(1L)
                .startDate(LocalDate.now().plusDays(20))
                .endDate(LocalDate.now().plusDays(10))
                .build();

        when(listingRepository.findById(creationBookingDto.getListingId())).thenReturn(Optional.ofNullable(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                bookingService.createBooking(creationBookingDto, username));

        assertEquals("The booking start date must be before the end date", exception.getMessage());
    }

    @Test
    void testCreateBooking_WhenBookingStartOverlapsExistingBooking_ShouldThrowException() {
        String username = "Tenant Username";
        UserEntity currentUser = new UserEntity();
        currentUser.setUsername(username);

        UserEntity landlord = new UserEntity();
        landlord.setUsername("Landlord Username");

        CreationBookingDto creationBookingDto = CreationBookingDto.builder()
                .listingId(1L)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        BookingEntity existingBooking = BookingEntity.builder()
                .id(1L)
                .startDate(LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .endDate(LocalDate.now().plusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant())
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(landlord)
                .bookings(List.of(existingBooking))
                .build();

        when(listingRepository.findById(creationBookingDto.getListingId())).thenReturn(Optional.ofNullable(listing));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                bookingService.createBooking(creationBookingDto, username));

        assertEquals("The booking start date is only available after: " + existingBooking.getEndDate(),
                exception.getMessage());
    }

    @Test
    void testConfirmBookingByLandlord_Success() {
        Long bookingId = 1L;
        String username = "Landlord Username";
        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title("Test Listing")
                .landlord(landlord)
                .build();

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .listing(listing)
                .tenant(UserEntity.builder().id(1L).username("Tenant Username").build())
                .status(BookingStatus.PENDING)
                .build();

        BookingDto expectedDto = BookingDto.builder()
                .id(1L)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.ofNullable(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(booking);
        when(bookingDtoMapper.makeBookingDto(booking)).thenReturn(expectedDto);

        BookingDto result = bookingService.confirmBookingByLandlord(bookingId, username);

        assertNotNull(result);
        assertEquals(expectedDto.getStatus(), result.getStatus());

        verify(bookingRepository, times(1)).save(any(BookingEntity.class));
        verify(redisCacheCleaner, times(1)).evictCacheByPattern("bookings::" + bookingId);
        verify(redisCacheCleaner, times(1)).evictBookingCacheForUser(booking.getTenant().getUsername());
        verify(redisCacheCleaner, times(1)).evictBookingCacheForLandlord(username);
        verify(emailService, times(1)).sendEmail(
                eq(booking.getTenant().getEmail()),
                eq("Booking Confirmed"),
                contains("Your booking for listing 'Test Listing' has been confirmed")
        );
        verify(notificationService, times(1)).createNotification(
                contains("Your booking for listing 'Test Listing' has been confirmed"),
                eq(booking.getTenant())
        );
    }

    @Test
    void testConfirmBookingByLandlord_WhenBookingNotFound_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "Landlord Username";

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.confirmBookingByLandlord(bookingId, username));
    }

    @Test
    void testConfirmBookingByLandlord_WhenUserNotLandlord_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "Some Username";
        UserEntity landlord = new UserEntity();
        landlord.setUsername("LandLord Username");

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(landlord)
                .build();

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .listing(listing)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.ofNullable(booking));

        Exception exception = assertThrows(BadRequestException.class, () ->
                bookingService.confirmBookingByLandlord(bookingId, username));

        assertEquals("You are not authorized to confirm this booking", exception.getMessage());
    }

    @Test
    void testConfirmBookingByLandlord_WhenBookingStatusNotPending_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "Landlord Username";
        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .landlord(landlord)
                .build();

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .listing(listing)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.ofNullable(booking));

        Exception exception = assertThrows(BadRequestException.class, () ->
                bookingService.confirmBookingByLandlord(bookingId, username));

        assertEquals("Only bookings with status 'PENDING' can be confirmed", exception.getMessage());
    }

    @Test
    void testCancelBookingByTenant_Access() {
        Long bookingId = 1L;
        String username = "Tenant Username";

        UserEntity tenant = new UserEntity();
        tenant.setUsername(username);

        UserEntity landlord = new UserEntity();
        landlord.setUsername("Landlord Username");

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title("Test Listing")
                .landlord(landlord)
                .build();

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .listing(listing)
                .tenant(tenant)
                .startDate(Instant.now().plusSeconds(86400)) // 1 day
                .build();

        BookingDto expectedDto = BookingDto.builder()
                .id(1L)
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.ofNullable(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(booking);
        when(bookingDtoMapper.makeBookingDto(booking)).thenReturn(expectedDto);

        BookingDto result = bookingService.cancelBookingByTenant(bookingId, username);

        assertNotNull(result);
        assertEquals(expectedDto.getStatus(), result.getStatus());

        verify(bookingRepository, times(1)).save(any(BookingEntity.class));
        verify(redisCacheCleaner, times(1)).evictCacheByPattern("bookings::" + bookingId);
        verify(redisCacheCleaner, times(1)).evictBookingCacheForUser(username);
        verify(redisCacheCleaner, times(1)).evictBookingCacheForLandlord(landlord.getUsername());
        verify(emailService, times(1)).sendEmail(
                eq(booking.getListing().getLandlord().getEmail()),
                eq("Booking Canceled"),
                contains("User has been canceled the booking for listing 'Test Listing'")
        );
        verify(notificationService, times(1)).createNotification(
                contains("User has been canceled the booking for listing 'Test Listing'"),
                eq(booking.getListing().getLandlord())
        );
    }

    @Test
    void testCancelBookingByTenant_WhenBookingNotFound_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "Tenant Username";

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.cancelBookingByTenant(bookingId, username));
    }

    @Test
    void testCancelBookingByTenant_WhenBookingDateAlreadyStarted_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "Tenant Username";

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .startDate(Instant.now().minusSeconds(86400)) // 1 day
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.ofNullable(booking));

        Exception exception = assertThrows(BadRequestException.class, () ->
                bookingService.cancelBookingByTenant(bookingId, username));

        assertEquals("You cannot cancel a booking already that has already started", exception.getMessage());
    }

    @Test
    void testDeclineBookingByLandlord_Success() {
        Long bookingId = 1L;
        String landlordUsername = "landlordUser";
        String tenantUsername = "tenantUser";

        UserEntity landlord = UserEntity.builder()
                .username(landlordUsername)
                .build();

        UserEntity tenant = UserEntity.builder()
                .username(tenantUsername)
                .email("tenant@gmail.com")
                .build();

        ListingEntity listing = ListingEntity.builder()
                .id(1L)
                .title("Test Listing")
                .landlord(landlord)
                .build();

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .listing(listing)
                .tenant(tenant)
                .status(BookingStatus.PENDING)
                .build();

        BookingDto expectedDto = BookingDto.builder()
                .id(1L)
                .status(BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.ofNullable(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenReturn(booking);
        when(bookingDtoMapper.makeBookingDto(booking)).thenReturn(expectedDto);

        BookingDto result = bookingService.declineBookingByLandlord(bookingId, landlordUsername);

        assertNotNull(result);
        assertEquals(expectedDto.getStatus(), result.getStatus());

        verify(bookingRepository, times(1)).save(any(BookingEntity.class));
        verify(redisCacheCleaner, times(1)).evictCacheByPattern("bookings::" + bookingId);
        verify(redisCacheCleaner, times(1)).evictBookingCacheForUser(booking.getTenant().getUsername());
        verify(redisCacheCleaner, times(1)).evictBookingCacheForLandlord(booking.getListing().getLandlord().getUsername());
        verify(emailService, times(1)).sendEmail(
                eq(booking.getTenant().getEmail()),
                eq("Booking Declined"),
                contains("The landlord has declined booking for the listing 'Test Listing'")
        );
        verify(notificationService, times(1)).createNotification(
                contains("The landlord has declined booking for the listing 'Test Listing'"),
                eq(booking.getTenant())
        );
    }

    @Test
    void testDeclineBookingByLandlord_BookingNotFound_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "landlordUser";

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> bookingService.declineBookingByLandlord(bookingId, username));
    }

    @Test
    void testDeclineBookingByLandlord_WhenBookingInvalidStatus_ShouldThrowException() {
        Long bookingId = 1L;
        String username = "landlordUser";

        UserEntity landlord = new UserEntity();
        landlord.setUsername(username);

        ListingEntity listing = ListingEntity.builder()
                .landlord(landlord)
                .build();

        BookingEntity booking = BookingEntity.builder()
                .id(bookingId)
                .listing(listing)
                .status(BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BadRequestException exception = assertThrows(BadRequestException.class, () ->
                bookingService.declineBookingByLandlord(bookingId, username));

        assertEquals("Only bookings with status 'PENDING' can be declined", exception.getMessage());
    }
}