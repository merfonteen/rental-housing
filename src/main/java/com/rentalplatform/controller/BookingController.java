package com.rentalplatform.controller;

import com.rentalplatform.dto.BookingDto;
import com.rentalplatform.dto.CreationBookingDto;
import com.rentalplatform.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

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

    @GetMapping(MY_BOOKINGS)
    public ResponseEntity<Page<BookingDto>> getBookings(Principal principal,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getBookings(principal.getName(), page, size));
    }

    @GetMapping(BOOKINGS_FOR_LANDLORD)
    public ResponseEntity<Page<BookingDto>> getBookingsForLandlord(Principal principal,
                                                                   @RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(bookingService.getBookingsForLandlord(principal.getName(), page, size));
    }

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody CreationBookingDto bookingDto,
                                                    Principal principal) {
        return ResponseEntity.ok(bookingService.createBooking(bookingDto, principal.getName()));
    }

    @PostMapping(CONFIRM_BY_ID)
    public ResponseEntity<Boolean> confirmBooking(@PathVariable Long bookingId, Principal principal) {
        return ResponseEntity.ok(bookingService.confirmBookingByLandlord(bookingId, principal.getName()));
    }

    @PatchMapping(CANCEL_BY_ID)
    public ResponseEntity<Boolean> cancelBooking(@PathVariable Long bookingId, Principal principal) {
        return ResponseEntity.ok(bookingService.cancelBookingByTenant(bookingId, principal.getName()));
    }

    @PatchMapping(DECLINE_BY_ID)
    public ResponseEntity<Boolean> declineBooking(@PathVariable Long bookingId, Principal principal) {
        return ResponseEntity.ok(bookingService.declineBookingByLandlord(bookingId, principal.getName()));
    }
}
