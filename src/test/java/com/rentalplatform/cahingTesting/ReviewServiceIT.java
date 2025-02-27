package com.rentalplatform.cahingTesting;

import com.rentalplatform.dto.creationDto.CreationReviewDto;
import com.rentalplatform.dto.updateDto.UpdateReviewDto;
import com.rentalplatform.entity.*;
import com.rentalplatform.repository.BookingRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.ReviewRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.service.ReviewService;
import com.rentalplatform.utils.RedisCacheCleaner;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewServiceIT extends AbstractRedisTest {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private RedisCacheCleaner redisCacheCleaner;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> redisOps;

    @BeforeEach
    void setUp() {
        redisOps = redisTemplate.opsForValue();
    }

    @Test
    @Transactional
    void testGetReviewById_ShouldCacheResult() {
        Long reviewId = createTestReviewForUser("testUsername").getId();
        String cacheKey = "reviews::" + reviewId;

        reviewService.getReviewById(reviewId);

        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testGetReviewsForListing_ShouldCacheResult() {
        Long listingId = createTestReviewForUser("testUsername").getListing().getId();
        int page = 0;
        int size = 10;
        String cacheKey = "reviews::" + listingId + "_" + false + "_" + false + "_" + page + "_" + size;

        reviewService.getReviewsForListing(listingId, false, false, page, size);

        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testCreateReview_ShouldEvictCache() {
        ReviewEntity review = createTestReviewForUser("tenantUsername");

        CreationReviewDto dto = CreationReviewDto.builder()
                .listingId(review.getListing().getId())
                .comment("Test Comment")
                .rating(9.0)
                .build();

        prepareTestCacheEviction(review, (reviewId) -> reviewService.createReview(dto, review.getTenant().getUsername()));
    }

    @Test
    @Transactional
    void testEditReview_ShouldEvictCache() {
        ReviewEntity review = createTestReviewForUser("tenantUsername");

        UpdateReviewDto dto = UpdateReviewDto.builder()
                .rating(5.0)
                .build();

        prepareTestCacheEviction(review, (reviewId) ->
                reviewService.editReview(reviewId, dto, review.getTenant().getUsername()));
    }

    @Test
    @Transactional
    void testDeleteReview_ShouldEvictCache() {
        ReviewEntity review = createTestReviewForUser("tenantUsername");
        prepareTestCacheEviction(review, (reviewId) -> reviewService.deleteReview(reviewId, review.getTenant().getUsername()));
    }

    private void prepareTestCacheEviction(ReviewEntity review, Consumer<Long> reviewAction) {
        int page = 0;
        int size = 10;
        String cacheKey = "reviews::" + review.getListing().getId() + "_" + false + "_" + false + "_" + page + "_" + size;

        reviewService.getReviewsForListing(review.getListing().getId(), false, false, page, size);
        assertThat(redisOps.get(cacheKey)).isNotNull();

        reviewAction.accept(review.getId());
        assertThat(redisOps.get(cacheKey)).isNull();
    }

    private ReviewEntity createTestReviewForUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username(username)
                        .email("testEmail123@gmail.com")
                        .password("123456")
                        .build())
                );

        UserEntity landlord = userRepository.findByUsername("landlordUsername")
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username("landlordUsername")
                        .email("testlandlord@gmail.com")
                        .password("123456")
                        .build())
                );

        ListingEntity listing = listingRepository.findById(1L)
                .orElseGet(() -> listingRepository.save(ListingEntity.builder()
                        .title("Test Title")
                        .landlord(landlord)
                        .build()));

        BookingEntity booking = bookingRepository.findById(1L)
                .orElseGet(() -> bookingRepository.save(BookingEntity.builder()
                        .listing(listing)
                        .tenant(user)
                        .status(BookingStatus.FINISHED)
                        .build()));

        ReviewEntity review = reviewRepository.findById(1L)
                .orElseGet(() -> reviewRepository.save(ReviewEntity.builder()
                        .tenant(user)
                        .listing(listing)
                        .build()));

        return review;
    }
}
