package com.rentalplatform.cahingTesting;

import com.rentalplatform.entity.FavoriteEntity;
import com.rentalplatform.entity.ListingEntity;
import com.rentalplatform.entity.UserEntity;
import com.rentalplatform.repository.FavoriteRepository;
import com.rentalplatform.repository.ListingRepository;
import com.rentalplatform.repository.UserRepository;
import com.rentalplatform.service.FavoriteService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;

public class FavoriteServiceIT extends AbstractRedisTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private ValueOperations<String, String> redisOps;

    @BeforeEach
    void setUp() {
        redisOps = redisTemplate.opsForValue();
    }

    @Test
    @Transactional
    void testGetFavoriteById_ShouldCacheResult() {
        FavoriteEntity favorite = createTestFavoriteForUser("testUsername");
        String cacheKey = "favoriteListings::" + favorite.getId();

        favoriteService.getFavoriteById(favorite.getId());
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testGetFavoriteListings_ShouldCacheResult() {
        FavoriteEntity favorite = createTestFavoriteForUser("testUsername");
        String cacheKey = "favoriteListings::" + favorite.getUser().getUsername();

        favoriteService.getFavoriteListings(favorite.getUser().getUsername());
        assertThat(redisOps.get(cacheKey)).isNotNull();
    }

    @Test
    @Transactional
    void testAddToFavorites_ShouldEvictCache() {
        FavoriteEntity favorite = createTestFavoriteForUser("testUsername");
        String cacheKey = "favoriteListings::" + favorite.getUser().getUsername();

        favoriteService.getFavoriteListings(favorite.getUser().getUsername());
        assertThat(redisOps.get(cacheKey)).isNotNull();

        ListingEntity listing = createTestListing();

        favoriteService.addToFavorites(listing.getId(), favorite.getUser().getUsername());
        assertThat(redisOps.get(cacheKey)).isNull();
    }

    @Test
    @Transactional
    void testRemoveFromFavorites_ShouldEvictCache() {
        FavoriteEntity favorite = createTestFavoriteForUser("testUsername");

        String cacheKeyById = "favoriteListings::" + favorite.getId();
        String cacheKeyByUsername = "favoriteListings::" + favorite.getUser().getUsername();

        favoriteService.getFavoriteById(favorite.getId());
        favoriteService.getFavoriteListings(favorite.getUser().getUsername());
        assertThat(redisOps.get(cacheKeyById)).isNotNull();
        assertThat(redisOps.get(cacheKeyByUsername)).isNotNull();

        favoriteService.removeFromFavorites(favorite.getId(), favorite.getUser().getUsername());

        assertThat(redisOps.get(cacheKeyById)).isNull();
        assertThat(redisOps.get(cacheKeyByUsername)).isNull();
    }

    private ListingEntity createTestListing() {
        UserEntity user = userRepository.findByUsername("anotherTestUser")
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username("anotherTestUser")
                        .email("testuser@gmail.com")
                        .password("123456")
                        .build())
                );

        ListingEntity listingToAddToFavorites = listingRepository.findById(500L)
                .orElseGet(() -> listingRepository.save(ListingEntity.builder()
                        .title("New Test Title")
                        .landlord(user)
                        .build())
                );

        return listingToAddToFavorites;
    }

    private FavoriteEntity createTestFavoriteForUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .username(username)
                        .email("testEmail123@gmail.com")
                        .password("123456")
                        .build())
                );

        ListingEntity listing = listingRepository.findById(1L)
                .orElseGet(() -> listingRepository.save(ListingEntity.builder()
                        .title("Test Title")
                        .landlord(user)
                        .build())
                );

        FavoriteEntity favorite = favoriteRepository.findById(1L)
                .orElseGet(() -> favoriteRepository.save(FavoriteEntity.builder()
                        .user(user)
                        .listing(listing)
                        .build())
                );

        return favorite;
    }
}
