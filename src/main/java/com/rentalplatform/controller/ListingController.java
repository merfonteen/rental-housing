package com.rentalplatform.controller;

import com.rentalplatform.dto.creationDto.CreationListingDto;
import com.rentalplatform.dto.updateDto.EditListingDto;
import com.rentalplatform.dto.FilterListingsDto;
import com.rentalplatform.dto.ListingDto;
import com.rentalplatform.service.ListingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RequiredArgsConstructor
@RequestMapping("/api/listings")
@RestController
public class ListingController {

    private final ListingService listingService;

    @GetMapping("/{listingId}")
    public ResponseEntity<ListingDto> getListing(@PathVariable Long listingId) {
        return ResponseEntity.ok(listingService.getListingById(listingId));
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @GetMapping("/my-listings")
    public ResponseEntity<Page<ListingDto>> getMyListings(Principal principal,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(listingService.getMyListings(principal.getName(), page, size));
    }

    @GetMapping("/all-listings")
    public ResponseEntity<Page<ListingDto>> getAllListings(@Valid @ModelAttribute FilterListingsDto filterDto,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(listingService.getAllListings(filterDto, page, size));
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @PostMapping
    public ResponseEntity<ListingDto> createListing(@Valid @RequestBody CreationListingDto creationListingDto,
                                                    Principal principal) {
        return ResponseEntity.ok(listingService.createListing(creationListingDto, principal.getName()));
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @PatchMapping("/{id}")
    public ResponseEntity<ListingDto> editListing(@PathVariable Long id,
                                                  @Valid @RequestBody EditListingDto editListingDto,
                                                  Principal principal) {
        return ResponseEntity.ok(listingService.editListing(id, editListingDto, principal.getName()));
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteListing(@PathVariable Long id, Principal principal) {
        listingService.deleteListing(id, principal.getName());
        return ResponseEntity.ok("The listing has been successfully deleted");
    }
}
