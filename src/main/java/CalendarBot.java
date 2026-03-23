import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;

import java.io.IOException;
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
            EventDateTime start = event.getStart();
            String when = (start.getDateTime() != null)
                    ? start.getDateTime().toString()
                    : start.getDate().toString();
            System.out.println(event.getSummary() + " @ " + when);
            System.out.println("  ID: " + event.getId());  // print ID for delete/patch operations
        }
    }

    //Task 2
    public static void searchByDateRange(Calendar service, String startDateStr, String endDateStr)
    {
        DateTime start = javaToGoogleTime(LocalDate.parse(startDateStr).atStartOfDay());
        DateTime end = javaToGoogleTime(LocalDate.parse(endDateStr).atStartOfDay());


    }

    //Task 3
    public static void createEvent(Calendar service)
    {

    }

    //Task 4
    public static void checkAvailability(Calendar service)
    {

    }

    //Task 5
    public static void deleteEvent(Calendar service)
    {

    }

    //Task 6
    public static void listCalendars(Calendar service)
    {

    }


}