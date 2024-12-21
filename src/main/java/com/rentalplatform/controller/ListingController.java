package com.rentalplatform.controller;

import com.rentalplatform.dto.CreationListingDto;
import com.rentalplatform.dto.EditListingDto;
import com.rentalplatform.dto.FilterListingsDto;
import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RequiredArgsConstructor
@RequestMapping("/api/listings")
@RestController
public class ListingController {

    private final ListingService listingService;

    public static final String MY_LISTINGS = "/my-listings";
    public static final String ALL_LISTINGS = "/all-listings";
    public static final String LISTING_BY_ID = "/{id}";

    @GetMapping(MY_LISTINGS)
    public ResponseEntity<List<ListingDto>> getListings(Principal principal) {
        return ResponseEntity.ok(listingService.getMyListings(principal.getName()));
    }

    @GetMapping(ALL_LISTINGS)
    public ResponseEntity<List<ListingDto>> getAllListings(@Valid @ModelAttribute FilterListingsDto filterDto) {
        return ResponseEntity.ok(listingService.getAllListings(filterDto));
    }

    @PostMapping
    public ResponseEntity<ListingDto> createListing(@Valid @RequestBody CreationListingDto creationListingDto,
                                                    Principal principal) {
        return ResponseEntity.ok(listingService.createListing(creationListingDto, principal.getName()));
    }

    @PatchMapping(LISTING_BY_ID)
    public ResponseEntity<ListingDto> editListing(@PathVariable Long id,
                                                  @Valid @RequestBody EditListingDto editListingDto,
                                                  Principal principal) {
        return ResponseEntity.ok(listingService.editListing(id, editListingDto, principal.getName()));
    }

    @DeleteMapping(LISTING_BY_ID)
    public ResponseEntity<String> deleteListing(@PathVariable Long id, Principal principal) {
        listingService.deleteListing(id, principal.getName());
        return ResponseEntity.ok("The listing has been successfully deleted");
    }
}
