package com.rentalplatform.utils;

import com.rentalplatform.entity.BookingEntity;
import com.rentalplatform.entity.BookingStatus;
import com.rentalplatform.repository.BookingRepository;
import com.rentalplatform.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@RequiredArgsConstructor
@Component
public class BookingStatusUpdater {

    private final EmailService emailService;
    private final BookingRepository bookingRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    public void updateFinishedBookings() {
        Instant now = Instant.now();
        List<BookingEntity> bookings = bookingRepository.findAllByStatusAndEndDateBefore(BookingStatus.CONFIRMED, now);

        for(BookingEntity booking : bookings) {
            booking.setStatus(BookingStatus.FINISHED);
            emailService.sendEmail(booking.getTenant().getEmail(),
                    "Booking Finished",
                    "Your booking '%s' has been finished".formatted(booking.getListing().getTitle()));
        }
    }
}
