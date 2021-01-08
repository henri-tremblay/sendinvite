import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SendInvite {

    private final Calendar calendar;

    public SendInvite(Calendar calendar) {
        this.calendar = calendar;
    }

    public static void main(String[] args) throws Exception {
        CalendarFactory calendarFactory = new CalendarFactory();
        Calendar calendar = calendarFactory.newCalendar();
        SendInvite service = new SendInvite(calendar);

        Files.lines(Paths.get("slots.csv"), StandardCharsets.UTF_8)
                .skip(1)
                .map(line -> line.split(";"))
                .map(tokens -> new Object() {
                    final String summary = tokens[0];
                    final String location = tokens[1];
                    final String description = tokens[2];
                    final String startTime = tokens[3];
                    final String endTime = tokens[4];
                    final String speaker = tokens[5];

                })
                .forEach(tuple -> service.createInvitation(
                        tuple.summary,
                        tuple.location,
                        tuple.description,
                        tuple.startTime,
                        tuple.endTime,
                        tuple.speaker
                ));
    }

    private void createInvitation(String summary, String location, String description, String startTime, String endTime, String speaker) {
        Event event = new Event()
                .setSummary(summary)
                .setLocation(location)
                .setDescription(description);

        DateTime startDateTime = new DateTime(startTime);
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("America/Toronto");
        event.setStart(start);

        DateTime endDateTime = new DateTime(endTime);
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("America/Toronto");
        event.setEnd(end);

        List<EventAttendee> attendees = List.of(
                new EventAttendee().setEmail(speaker)
        );
        event.setAttendees(attendees);

        String calendarId = "primary";
        try {
            event = calendar.events().insert(calendarId, event)
                    .setSendNotifications(true)
                    .execute();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        System.out.printf("Event created: %s\n", event.getHtmlLink());
    }
}
