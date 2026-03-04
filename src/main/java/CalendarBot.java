import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;


public class CalendarBot {

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

    public static void searchByDateRange(Calendar service, String startDateStr, String endDateStr)
    {

    }

    public static void createEvent(Calendar service)
    {

    }

    public static void checkAvailability(Calendar service)
    {

    }

    public static void deleteEvent(Calendar service)
    {

    }

    public static void listCalendars(Calendar service)
    {

    }


}