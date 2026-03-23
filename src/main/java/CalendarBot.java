import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;

import java.io.IOException;
import java.sql.SQLOutput;
import java.time.*;
import java.util.*;

import static java.text.DateFormat.Field.TIME_ZONE;


public class CalendarBot {

    private static final String TIME_ZONE = "America/New_York";

    //Helper 1
    private static DateTime javaToGoogleTime(LocalDateTime ldt)
    {
        ZonedDateTime zdt = ldt.atZone(ZoneId.of("America/New_York"));
        return new DateTime(zdt.toInstant().toEpochMilli());
    }

    //Helper 2
    private static String getWhen(EventDateTime edt)
    {
        if(edt.getDateTime() != null)
            return edt.getDateTime().toString();
        return "All day " + edt.getDate().toString();
    }

    //Task 1
    public static void listUpcomingEvents(Calendar service) throws IOException {

        DateTime now = new DateTime(System.currentTimeMillis());

        Events events = service.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(now)          // only events starting from now
                .setOrderBy("startTime")
                .setSingleEvents(true)    // ← IMPORTANT: see note below
                .execute();

        for (Event event : events.getItems()) {
            System.out.println(event.getSummary() + " @ " + getWhen(event.getStart()) + ";  ID: " + event.getId());  // print ID for delete/patch operations
        }
    }

    //Task 2
    public static void searchByDateRange(Calendar service, String startDateStr, String endDateStr) throws IOException {
        DateTime start = javaToGoogleTime(LocalDate.parse(startDateStr).atStartOfDay());
        DateTime end = javaToGoogleTime(LocalDate.parse(endDateStr).atStartOfDay());

        Events events = service.events().list("primary")
                .setMaxResults(10)
                .setTimeMin(start)
                .setTimeMax(end)
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();

        for (Event event : events.getItems()) {
            System.out.println(event.getSummary() + " @ " + getWhen(event.getStart()) + ";  ID: " + event.getId());
        }
    }

    //Task 3
    public static void createEvent(Calendar service, Scanner scanner) throws IOException
    {
        System.out.println("Summary: ");
        String summary = scanner.nextLine();

        System.out.println("Date (yyyy-MM-dd): ");
        String date = scanner.nextLine();

        System.out.println("Time (HH:mm): ");
        String time = scanner.nextLine();

        System.out.println("Duration (minutes): ");
        int duration = Integer.parseInt(scanner.nextLine());

        System.out.println("Attendee Email: ");
        String email = scanner.nextLine();



        LocalDateTime startLDT = LocalDateTime.parse(date + "T" + time);
        Event event = new Event().setSummary(summary);
        event.setStart(new EventDateTime().setDateTime(javaToGoogleTime(startLDT)).setTimeZone(TIME_ZONE));
        event.setEnd(new EventDateTime().setDateTime(javaToGoogleTime(startLDT.plusMinutes(duration))).setTimeZone(TIME_ZONE));

        if(!email.isEmpty())
        {
            event.setAttendees(List.of(new EventAttendee().setEmail(email)));
        }

        Event eventCreated = service.events().insert("primary", event).setSendUpdates("all").execute();
        System.out.println("Event created. ID: " + eventCreated.getId());
    }

    //Task 4
    public static void checkAvailability(Calendar service, Scanner scanner) throws IOException
    {
        System.out.println("Date (yyyy-MM-dd): ");
        String date = scanner.nextLine();

        System.out.println("Start Time (HH:mm): ");
        String startTime = scanner.nextLine();

        System.out.println("End Time (HH:mm): ");
        String endTime = scanner.nextLine();


        DateTime start = javaToGoogleTime(LocalDateTime.parse(date + "T" + startTime));
        DateTime end = javaToGoogleTime(LocalDateTime.parse(date + "T" + endTime));


        FreeBusyRequest request = new FreeBusyRequest()
                .setTimeMin(start)
                .setTimeMax(end)
                .setItems(List.of(new FreeBusyRequestItem().setId("primary")));

        FreeBusyResponse response = service.freebusy().query(request).execute();

        // Parse the response: it's a map from calendar ID ? list of busy windows
        var busySlots = response.getCalendars().get("primary").getBusy();
        if (busySlots == null || busySlots.isEmpty()) {
            System.out.println("? Calendar is FREE during this window.");
        } else {
            System.out.println("? Busy during:");
            for (var slot : busySlots) {
                // Each slot has a start and end — these are RFC3339 timestamp strings
                // Example: "2026-03-05T10:00:00-05:00" to "2026-03-05T11:00:00-05:00"
                System.out.println("  " + slot.getStart() + "  ?  " + slot.getEnd());
            }
        }
    }

    //Task 5
    public static void deleteEvent(Calendar service, Scanner scanner) throws IOException
    {
        System.out.println("Event ID: ");
        String eventID = scanner.nextLine();

        System.out.println("Are you sure? (yes/no)");
        if(scanner.nextLine().equalsIgnoreCase("yes"))
        {
            service.events().delete("primary", eventID).execute();
            System.out.println("Event deleted");
        }
    }

    //Task 6
    public static void listCalendars(Calendar service) throws IOException
    {
        for(CalendarListEntry entry : service.calendarList().list().execute().getItems())
            System.out.println(entry.getSummary() + " (ID: " + entry.getId() + ")");
    }


}