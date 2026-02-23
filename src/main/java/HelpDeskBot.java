import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class HelpDeskBot {

    public static void listUnreadTickets(Gmail service) throws IOException {
        int count = 0;

        ListMessagesResponse unreadResponses = service.users().messages()
                .list("me")
                .setQ("is:unread label:inbox")
                .setMaxResults(10L)
                .execute();

        if (unreadResponses.getMessages() == null) {
            System.out.println("No messages found.");
            return;
        }

        System.out.println("--- Unread Tickets (latest 10) -----------------------");
        for (Message msgRef : unreadResponses.getMessages()) {
            count++;

            Message fullMsg = service.users().messages()
                    .get("me", msgRef.getId())
                    .setFormat("metadata")
                    .setMetadataHeaders(Arrays.asList("Subject", "From"))
                    .execute();


            String subject = fullMsg.getPayload().getHeaders()
                    .stream()
                    .filter(h -> "Subject".equals(h.getName()))
                    .map(h -> h.getValue())
                    .findFirst()
                    .orElse("(no subject)");

            String from = fullMsg.getPayload().getHeaders()
                    .stream()
                    .filter(h -> "From".equals(h.getName()))
                    .map(h -> h.getValue())
                    .findFirst()
                    .orElse("(no sender)");

            System.out.println(count + ". Subject: " + subject + " || From: " + from);
        }
        System.out.println("------------------------------------------------------");
    }

    public static void searchTickets(Gmail service, String query) throws IOException {
        ListMessagesResponse searchResponse = service.users().messages()
                .list("me")
                .setQ(query)
                .setMaxResults(20L)
                .execute();

        int count = 0;

        System.out.println("--- Custom Query: " + query + " (latest 20) -----------------------");
        for (Message msgRef : searchResponse.getMessages()) {
            count++;

            Message fullMsg = service.users().messages()
                    .get("me", msgRef.getId())
                    .setFormat("metadata")
                    .setMetadataHeaders(Arrays.asList("Subject", "From", "Snippet"))
                    .execute();


            String subject = fullMsg.getPayload().getHeaders()
                    .stream()
                    .filter(h -> "Subject".equals(h.getName()))
                    .map(h -> h.getValue())
                    .findFirst()
                    .orElse("(no subject)");

            String from = fullMsg.getPayload().getHeaders()
                    .stream()
                    .filter(h -> "From".equals(h.getName()))
                    .map(h -> h.getValue())
                    .findFirst()
                    .orElse("(no sender)");

            String snippet = fullMsg.getPayload().getHeaders()
                    .stream()
                    .filter(h -> "Snippet".equals(h.getName()))
                    .map(h -> h.getValue())
                    .findFirst()
                    .orElse("(no snippet)");

            System.out.println(count + ". Subject: " + subject + " || From: " + from + "Snippet: " + snippet);
        }
        System.out.println("------------------------------------------------------");
    }

    public static void readTicket(Gmail service, String messageId) throws IOException {
        // Fetch full message (not just metadata)
        Message message = service.users().messages()
                .get("me", messageId)
                .setFormat("full")       // "full" gives us the entire MIME tree including body
                .execute();

        if(!message.isEmpty()) {
            System.out.println("No message found");
            return;
        }


        String output = "";


        // Case 1: Simple single-part message — body data is directly in payload
        String data = message.getPayload().getBody().getData();
        if (data != null) {
            byte[] bytes = Base64.getUrlDecoder().decode(data);  // decode Base64URL ? raw bytes
            output = new String(bytes, StandardCharsets.UTF_8);    // convert bytes ? readable text
        }

        // Case 2: Multipart message — body is split across parts (plain text, HTML, etc.)
        var parts = message.getPayload().getParts();
        if (parts != null) {
            for (var part : parts) {
                if ("text/plain".equals(part.getMimeType())) {   // find the plain-text part
                    String partData = part.getBody().getData();
                    byte[] bytes = Base64.getUrlDecoder().decode(partData);
                    output = new String(bytes, StandardCharsets.UTF_8);
                }
            }
        }

        if(!output.isEmpty())
            System.out.println(output);
        else
            System.out.println("(no plain text body)");
    }

    public static void replyToTicket(Gmail service, String messageId, String replyBody)
    {

    }

    public static void applyLabel(Gmail service, String messageId, String labelName)
    {

    }

    public static void trashTicket(Gmail service, String messageId)
    {

    }

    public static void runMenu(Gmail service)
    {

    }
}
