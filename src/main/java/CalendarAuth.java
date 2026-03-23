import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;
import com.google.api.client.util.DateTime;
import com.google.api.services.gmail.Gmail;
import jakarta.mail.MessagingException;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class CalendarAuth {
    public static void main(String[] args) throws Exception {
        List<String> scopes = Collections.singletonList(CalendarScopes.CALENDAR);
        final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Calendar service = new Calendar.Builder(httpTransport,
                GsonFactory.getDefaultInstance(),
                GoogleAuthHelper.getCredentials(httpTransport,
                        scopes)).setApplicationName("My Calendar App").build();


        CalendarBot.runMenu(service);

    }
}