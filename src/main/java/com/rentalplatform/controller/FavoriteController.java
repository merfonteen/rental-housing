package com.rentalplatform.controller;

import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/favorites")
@RestController
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/{favoriteId}")
    public ResponseEntity<ListingDto> getFavorite(@PathVariable Long favoriteId) {
        return ResponseEntity.ok(favoriteService.getFavoriteById(favoriteId));
    }

    @GetMapping
    public ResponseEntity<List<ListingDto>> getFavorites(Principal principal) {
        return ResponseEntity.ok(favoriteService.getFavoriteListings(principal.getName()));
    }

    @PostMapping("/{listingId}")
    public ResponseEntity<ListingDto> addToFavorites(@PathVariable Long listingId, Principal principal) {
        return ResponseEntity.ok(favoriteService.addToFavorites(listingId, principal.getName()));
    }

    @DeleteMapping("/{listingId}")
    public ResponseEntity<String> removeFromFavorites(@PathVariable Long listingId, Principal principal) {
        favoriteService.removeFromFavorites(listingId, principal.getName());
        return ResponseEntity.ok("Listing has been deleted successfully");
    }
}
