import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Properties;


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

            System.out.println("Message ID: " + msgRef.getId());

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

            System.out.println("Message ID: " + msgRef.getId());

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

        if(message.isEmpty()) {
            System.out.println("No message found");
            return;
        }


        String output = "";


        // Case 1: Simple single-part message — body data is directly in payload
//        String data = message.getPayload().getBody().getData();
//        if (data != null) {
//            byte[] bytes = Base64.getUrlDecoder().decode(data);  // decode Base64URL ? raw bytes
//            output = new String(bytes, StandardCharsets.UTF_8);    // convert bytes ? readable text
//        }

//         Case 2: Multipart message — body is split across parts (plain text, HTML, etc.)
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

        if(output.isEmpty())
            System.out.println("(no plain text body)");
        else
            System.out.println(output);
    }

    public static void replyToTicket(Gmail service, String messageId, String replyBody) throws IOException, MessagingException {

        Message message = service.users().messages()
                .get("me", messageId)
                .setFormat("metadata")
                .setMetadataHeaders(Arrays.asList("Subject", "From", "Message-ID", "threadId"))
                .execute();

        if(message.isEmpty()) {
            System.out.println("No message found");
            return;
        }

        String subject = message.getPayload().getHeaders()
                .stream()
                .filter(h -> "Subject".equals(h.getName()))
                .map(h -> h.getValue())
                .findFirst()
                .orElse("(no subject)");

        String from = message.getPayload().getHeaders()
                .stream()
                .filter(h -> "From".equals(h.getName()))
                .map(h -> h.getValue())
                .findFirst()
                .orElse("(no sender)");

        String messageID = message.getPayload().getHeaders()
                .stream()
                .filter(h -> "Message-ID".equals(h.getName()))
                .map(h -> h.getValue())
                .findFirst()
                .orElse("(no subject)");

        String threadID = message.getPayload().getHeaders()
                .stream()
                .filter(h -> "threadID".equals(h.getName()))
                .map(h -> h.getValue())
                .findFirst()
                .orElse("(no subject)");


        // Step 1: Create a MIME message using Jakarta Mail
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);  // minimal mail session (no server needed)
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress("me"));           // "me" = authenticated user
        email.addRecipient(jakarta.mail.Message.RecipientType.TO,new InternetAddress(from));
        email.setSubject("Re: " + subject);
        email.setText(replyBody);                            // plain-text body

        // Step 2: Serialize the MimeMessage to bytes
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();

        // Step 3: Encode as Base64URL
        String encodedEmail = Base64.getUrlEncoder()
                .encodeToString(rawMessageBytes);

        // Step 4: Wrap in a Gmail API Message and send
        Message newMessage = new Message();
        newMessage.setRaw(encodedEmail);
        newMessage = service.users().messages()
                .send("me", newMessage)
                .execute();

        System.out.println("Message sent. ID: " + newMessage.getId());
    }

    public static void applyLabel(Gmail service, String messageId, String labelName) throws IOException {
        // List all labels — both system and custom
        var labels = service.users().labels().list("me").execute();
        for (Label label : labels.getLabels()) {
            System.out.println(label.getName() + " - " + label.getId());
            // getName() ? "IN_PROGRESS" or "INBOX" etc.
            // getId()   ? "Label_12345" or "INBOX" etc.
        }

        Label newLabel = new Label()
                .setName(labelName)
                .setLabelListVisibility("labelShow")      // show in the Gmail sidebar
                .setMessageListVisibility("show");        // show on messages in the list

        Label created = service.users().labels()
                .create("me", newLabel)
                .execute();

        String labelID = created.getId();

        System.out.println("Created label ID: " + labelID);  // save this ID for later use

        // labelId comes from listing or creating labels above
        ModifyMessageRequest request = new ModifyMessageRequest()
                .setAddLabelIds(Collections.singletonList(labelID));      // labels to add
                //.setRemoveLabelIds(Collections.singletonList("UNREAD"));  // labels to remove (marks as read)

        service.users().messages()
                .modify("me", messageId, request)
                .execute();

        System.out.println("Label " + labelName + " with LabelID of " + labelID + " applied to messageID: " + messageId);
    }

    public static void trashTicket(Gmail service, String messageId) throws IOException {

    }

    public static void runMenu(Gmail service) throws IOException {

    }
}
