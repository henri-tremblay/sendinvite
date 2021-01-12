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
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SendInvite {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final Calendar calendar;
    private final Map<String, String> people;

    public SendInvite(Calendar calendar, Map<String, String> people) {
        this.calendar = calendar;
        this.people = people;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> people = readPeople();

        CalendarFactory calendarFactory = new CalendarFactory();
        Calendar calendar = calendarFactory.newCalendar();
        SendInvite service = new SendInvite(calendar, people);

        Files.lines(Paths.get("slots.csv"), StandardCharsets.UTF_8)
                .skip(1)
                .map(line -> line.split(","))
                .map(tokens -> new Object() {
                    // Day,Room,Time (EST),Title,Speaker 1,Speaker 2,Moderator,Guest URL,YouTube URL
                    final String day = tokens[0];
                    final String room = tokens[1];
                    final String time = tokens[2];
                    final String title = tokens[3];
                    final String speaker1 = tokens[4];
                    final String speaker2 = tokens[5];
                    final String moderator = tokens[6];
                    final String guestUrl = tokens[7];
                    final String youtubeUrl = tokens[8];
                })
                .map(tuple -> new Object() {
                    final String summary = "jChampion Conference slot: " + tuple.title;
                    final String location = tuple.guestUrl;
                    final String description = "Hi,\nDon't forget, you are presenting or moderating a session at jChampions Conference.\nHere is the information you need:\n"
                            + "Room (gmail account to use to connect to StreamYard): " + tuple.room + "\n"
                            + "Title: " + tuple.title + "\n"
                            + "Speaker 1: " + tuple.speaker1 + "\n"
                            + "Speaker 2: " + tuple.speaker2 + "\n"
                            + "Moderator: " + tuple.moderator + "\n"
                            + "Speaker URL: " + tuple.guestUrl + "\n"
                            + "Youtube URL: " + tuple.youtubeUrl + "\n"
                            + "\n"
                            + "Thanks a lot, have fun!\n"
                            + "jChampions Conference organizers";
                    final ZonedDateTime startTime = getZonedDateTime(tuple.day + " " + tuple.time + " EST");

                    private ZonedDateTime getZonedDateTime(String time) {
                        return ZonedDateTime.parse(time, FORMATTER);
                    }

                    final ZonedDateTime endTime = startTime.plusHours(1);
                    final String[] attendees = Stream.of(tuple.speaker1, tuple.speaker2, tuple.moderator)
                            .filter(p -> p != null && !p.isBlank())
                            .map(people::get)
                            .toArray(String[]::new);

                })
                .forEach(tuple -> service.createInvitation(
                        tuple.summary,
                        tuple.location,
                        tuple.description,
                        tuple.startTime,
                        tuple.endTime,
                        tuple.attendees
                ));
    }

    private static Map<String, String> readPeople() {
        try {
            return Files.lines(Paths.get("people.csv"), StandardCharsets.UTF_8)
                    .map(line -> line.split(","))
                    .map(tokens -> new Object() {
                        final String name = tokens[0];
                        final String email = tokens[1];
                    })
                    .collect(Collectors.toMap(tuple -> tuple.name, tuple -> tuple.email));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void createInvitation(String summary, String location, String description, ZonedDateTime startTime, ZonedDateTime endTime, String... recipients) {
        Event event = new Event()
                .setSummary(summary)
                .setLocation(location)
                .setDescription(description);

        DateTime startDateTime = new DateTime(startTime.toInstant().toEpochMilli());
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("America/Toronto");
        event.setStart(start);

        DateTime endDateTime = new DateTime(endTime.toInstant().toEpochMilli());
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("America/Toronto");
        event.setEnd(end);

        List<EventAttendee> attendees = Stream.of(recipients)
            .map(recipient -> new EventAttendee().setEmail(recipient))
            .collect(Collectors.toList());
        event.setAttendees(attendees);

        System.out.println(event);

//        String calendarId = "primary";
//        try {
//            event = calendar.events().insert(calendarId, event)
//                    .setSendNotifications(true)
//                    .execute();
//        } catch (IOException e) {
//            throw new UncheckedIOException(e);
//        }
//        System.out.printf("Event created: %s\n", event.getHtmlLink());
    }
}
