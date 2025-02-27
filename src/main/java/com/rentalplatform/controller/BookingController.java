package com.rentalplatform.controller;

import com.rentalplatform.dto.BookingDto;
import com.rentalplatform.dto.creationDto.CreationBookingDto;
import com.rentalplatform.dto.PageDto;
import com.rentalplatform.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RequiredArgsConstructor
@RequestMapping("/api/bookings")
@RestController
public class BookingController {

    private final BookingService bookingService;

    public static final String MY_BOOKINGS = "/my-bookings";
    public static final String BOOKINGS_FOR_LANDLORD = "/owner";
    public static final String CONFIRM_BY_ID = "/confirm/{bookingId}";
    public static final String CANCEL_BY_ID = "/cancel/{bookingId}";
    public static final String DECLINE_BY_ID = "/decline/{bookingId}";

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDto> getBooking(@PathVariable Long bookingId, Principal principal) {
        return ResponseEntity.ok(bookingService.getBookingById(bookingId, principal.getName()));
    }

    @GetMapping(MY_BOOKINGS)
    public ResponseEntity<PageDto<BookingDto>> getBookings(Principal principal,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getBookings(principal.getName(), page, size));
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @GetMapping(BOOKINGS_FOR_LANDLORD)
    public ResponseEntity<PageDto<BookingDto>> getBookingsForLandlord(Principal principal,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getBookingsForLandlord(principal.getName(), page, size));
    }

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreationBookingDto bookingDto,
                                                    Principal principal) {
        return ResponseEntity.ok(bookingService.createBooking(bookingDto, principal.getName()));
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @PostMapping(CONFIRM_BY_ID)
    public ResponseEntity<BookingDto> confirmBooking(@PathVariable Long bookingId, Principal principal) {
        return ResponseEntity.ok(bookingService.confirmBookingByLandlord(bookingId, principal.getName()));
    }

    @PreAuthorize("hasRole('ROLE_TENANT')")
    @PatchMapping(CANCEL_BY_ID)
    public ResponseEntity<BookingDto> cancelBooking(@PathVariable Long bookingId, Principal principal) {
        return ResponseEntity.ok(bookingService.cancelBookingByTenant(bookingId, principal.getName()));
    }

    @PreAuthorize("hasRole('ROLE_LANDLORD')")
    @PatchMapping(DECLINE_BY_ID)
    public ResponseEntity<BookingDto> declineBooking(@PathVariable Long bookingId, Principal principal) {
        return ResponseEntity.ok(bookingService.declineBookingByLandlord(bookingId, principal.getName()));
    }
}
