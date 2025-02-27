package com.rentalplatform.cahingTesting;

import com.rentalplatform.dto.BookingDto;
import com.rentalplatform.dto.PageDto;
import com.rentalplatform.dto.creationDto.CreationBookingDto;
import com.rentalplatform.entity.BookingEntity;
import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.repository.BookingRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.service.BookingService;
import com.rentalplatform.utils.RedisCacheCleaner;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingServiceIT extends AbstractRedisTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisCacheCleaner redisCacheCleaner;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> redisOps;

    @BeforeEach
    void setUp() {
        redisOps = redisTemplate.opsForValue();
    }

    @Test
    @Transactional
    void testBookingById_ShouldCacheResult() {
        Long bookingId = createTestBookingForUser("testUser");
        String cacheKey = "bookings::" + bookingId;

        bookingService.getBookingById(bookingId, "testUser");

        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testGetBookings_ShouldCacheResult() {
        String username = "testUser";
        int page = 0;
        int size = 10;
        createTestBookingForUser(username);
        String cacheKey = "bookings::" + username + "_" + page + "_" + size;

        PageDto<BookingDto> firstCall = bookingService.getBookings(username, page, size);
        PageDto<BookingDto> secondCall = bookingService.getBookings(username, page, size);

        assertThat(redisOps.get(cacheKey)).isNotNull();
        assertThat(firstCall.getContent()).isEqualTo(secondCall.getContent());
    }

    @Test
    @Transactional
    void testGetBookings_ShouldNotCacheEmptyResult() {
        String username = "testUser";
        int page = 0;
        int size = 10;
        String cacheKey = "bookings::" + username + "_" + page + "_" + size;

        PageDto<BookingDto> result = bookingService.getBookings(username, page, size);

        assertThat(redisOps.get(cacheKey)).isNull();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @Transactional
    void testGetBookingsForLandlord_ShouldCacheResult() {
        String username = "testLandlord";
        int page = 0;
        int size = 10;
        createTestBookingForLandlord(username);
        String cacheKey = "bookingsForLandlord::" + username + "_" + page + "_" + size;

        PageDto<BookingDto> firstCall = bookingService.getBookingsForLandlord(username, page, size);
        PageDto<BookingDto> secondCall = bookingService.getBookingsForLandlord(username, page, size);

        assertThat(redisOps.get(cacheKey)).isNotNull();
        assertThat(firstCall.getContent()).isEqualTo(secondCall.getContent());
    }

    @Test
    @Transactional
    void testGetBookingsForLandlord_ShouldNotCacheEmptyResult() {
        String username = "testLandlord";
        int page = 0;
        int size = 10;
        String cacheKey = "bookingsForLandlord::" + username + "_" + page + "_" + size;

        bookingService.getBookingsForLandlord(username, page, size);

        assertThat(redisOps.get(cacheKey)).isNull();
    }

    @Test
    @Transactional
    void testCreateBooking_ShouldEvictCache() {
        int page = 0;
        int size = 10;

        String landlordUsername = "testLandlord";
        BookingEntity testBooking = createTestBookingForLandlord(landlordUsername);
        Long listingId = testBooking.getListing().getId();
        String username = testBooking.getTenant().getUsername();

        String userCacheKey = "bookings::" + username + "_" + page + "_" + size;
        String landlordCacheKey = "bookingsForLandlord::" + landlordUsername + "_" + page + "_" + size;

        bookingService.getBookings(username, page, size);
        bookingService.getBookingsForLandlord(landlordUsername, page, size);

        assertThat(redisOps.get(userCacheKey)).isNotNull();
        assertThat(redisOps.get(landlordCacheKey)).isNotNull();

        CreationBookingDto dto = CreationBookingDto.builder()
                .listingId(listingId)
                .startDate(LocalDate.now().plusDays(2))
                .endDate(LocalDate.now().plusDays(5))
                .build();

        bookingService.createBooking(dto, username);

        assertThat(redisOps.get(userCacheKey)).isNull();
        assertThat(redisOps.get(landlordCacheKey)).isNull();
    }

    @Test
    @Transactional
    void testConfirmBookingByLandlord_ShouldEvictCache() {
        BookingEntity testBooking = createTestBookingForLandlord("landlordUsername");
        prepareTestCacheEviction(testBooking.getId(),
                testBooking.getTenant().getUsername(),
                testBooking.getListing().getLandlord().getUsername(),
                (bookingId) -> bookingService.confirmBookingByLandlord(bookingId,
                        testBooking.getListing().getLandlord().getUsername()));
    }

    @Test
    @Transactional
    void testCancelBookingByTenant_ShouldEvictCache() {
        BookingEntity testBooking = createTestBookingForLandlord("landlordUsername");
        prepareTestCacheEviction(testBooking.getId(),
                testBooking.getTenant().getUsername(),
                testBooking.getListing().getLandlord().getUsername(),
                (bookingId) -> bookingService.cancelBookingByTenant(bookingId, testBooking.getTenant().getUsername()));
    }

    @Test
    @Transactional
    void testDeclineBookingByLandlord_ShouldEvictCache() {
        BookingEntity testBooking = createTestBookingForLandlord("landlordUsername");
        prepareTestCacheEviction(testBooking.getId(),
                testBooking.getTenant().getUsername(),
                testBooking.getListing().getLandlord().getUsername(),
                (bookingId) -> bookingService.declineBookingByLandlord(bookingId,
                        testBooking.getListing().getLandlord().getUsername()));
    }

    public void prepareTestCacheEviction(Long bookingId, String tenantUsername, String landlordUsername,
                                         Consumer<Long> bookingAction) {
        int page = 0;
        int size = 10;

        String userCacheKey = "bookings::" + tenantUsername + "_" + page + "_" + size;
        String landlordCacheKey = "bookingsForLandlord::" + landlordUsername + "_" + page + "_" + size;
        String bookingsCacheById = "bookings::" + bookingId;

        bookingService.getBookingById(bookingId, landlordUsername);
        bookingService.getBookings(tenantUsername, page, size);
        bookingService.getBookingsForLandlord(landlordUsername, page, size);
        assertThat(redisOps.get(userCacheKey)).isNotNull();
        assertThat(redisOps.get(landlordCacheKey)).isNotNull();
        assertThat(redisOps.get(bookingsCacheById)).isNotNull();

        bookingAction.accept(bookingId);

        assertThat(redisOps.get(userCacheKey)).isNull();
        assertThat(redisOps.get(landlordCacheKey)).isNull();
        assertThat(redisOps.get(bookingsCacheById)).isNull();
    }

    private BookingEntity createTestBookingForLandlord(String username) {
        UserEntity landlord = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username(username)
                        .email("testEmail123@gmail.com")
                        .password("123456")
                        .build())
                );

        ListingEntity listing = listingRepository.findById(1L)
                .orElseGet(() -> listingRepository.save(ListingEntity.builder()
                        .title("Test Listing")
                        .landlord(landlord)
                        .build()));

        UserEntity tenant = userRepository.findByUsername("tenant")
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username("tenant")
                        .email("tenant@gmail.com")
                        .password("123456")
                        .build()));

        BookingEntity bookingForLandlord = BookingEntity.builder()
                .tenant(tenant)
                .listing(listing)
                .startDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .status(BookingStatus.PENDING)
                .build();

        return bookingRepository.save(bookingForLandlord);
    }

    private Long createTestBookingForUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username(username)
                        .email("testEmail123@gmail.com")
                        .password("123456")
                        .build())
                );

        ListingEntity listing = listingRepository.findById(1L)
                .orElseGet(() -> listingRepository.save(ListingEntity.builder()
                        .title("Test Listing")
                        .build()));

        BookingEntity booking = BookingEntity.builder()
                .tenant(user)
                .listing(listing)
                .startDate(Instant.now().plus(1, ChronoUnit.DAYS))
                .endDate(Instant.now().plus(5, ChronoUnit.DAYS))
                .status(BookingStatus.PENDING)
                .build();

        return bookingRepository.save(booking).getId();
    }
}
