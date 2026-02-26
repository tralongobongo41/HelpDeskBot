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
import java.util.*;


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

            System.out.println(count + ". Subject: " + subject + " || From: " + from + " || Message ID: " + msgRef.getId());
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

            String snippet = fullMsg.getSnippet();
            if(snippet == null)
                snippet = "(no snippet)";

            System.out.println(count + ". Subject: " + subject + " || From: " + from + "Snippet: " + snippet + " || Message ID: " + msgRef.getId());
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
                .setMetadataHeaders(Arrays.asList("Subject", "From", "Message-ID"))
                .execute();

        if(message.isEmpty()) {
            System.out.println("No message found");
            return;
        }

        String threadId = message.getThreadId();

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

        String originalMessageID = message.getPayload().getHeaders()
                .stream()
                .filter(h -> "Message-ID".equals(h.getName()))
                .map(h -> h.getValue())
                .findFirst()
                .orElse("(no message ID found)");


        // Step 1: Create a MIME message using Jakarta Mail
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);  // minimal mail session (no server needed)
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress("me"));           // "me" = authenticated user
        email.addRecipient(jakarta.mail.Message.RecipientType.TO,new InternetAddress(from));

        if(!subject.toLowerCase().startsWith("re:"))
        {
            subject = "Re: " + subject;
        }

        email.setSubject(subject);
        email.setText(replyBody);                            // plain-text body

        email.setHeader("In-Reply-To", originalMessageID);
        email.setHeader("References", originalMessageID);

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
        newMessage.setThreadId(threadId);

        newMessage = service.users().messages()
                .send("me", newMessage)
                .execute();

        System.out.println("Reply sent. ID: " + newMessage.getId());
    }

    public static void applyLabel(Gmail service, String messageId, String labelName) throws IOException {
        String labelID = "";

        // List all labels — both system and custom
        var labels = service.users().labels().list("me").execute();
        for (Label label : labels.getLabels()) {
            System.out.println(label.getName() + " - " + label.getId());
            if(label.getName().equals(labelName)) {
                labelID = label.getId();
                System.out.println("Label exists. LabelID: " + labelID);
            }
            // getName() ? "IN_PROGRESS" or "INBOX" etc.
            // getId()   ? "Label_12345" or "INBOX" etc.
        }

        if(labelID.isEmpty())
        {
            Label newLabel = new Label()
                    .setName(labelName)
                    .setLabelListVisibility("labelShow")      // show in the Gmail sidebar
                    .setMessageListVisibility("show");        // show on messages in the list

            Label created = service.users().labels()
                    .create("me", newLabel)
                    .execute();

            labelID = created.getId();

            System.out.println("Created label ID: " + labelID);  // save this ID for later use

        }

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
        // Move message to trash (recoverable for 30 days)
        service.users().messages()
                .trash("me", messageId)
                .execute();

        System.out.println("Message (ID: " + messageId + ") moved to Trash (30-day retention). This action is reversible.");
    }

    public static void runMenu(Gmail service) throws IOException, MessagingException {
        Scanner scanner = new Scanner(System.in);
        boolean isRunning = true;

        try {
            while (isRunning) {
                System.out.println("\n----------------------------------");
                System.out.println("           Help-Desk Bot           ");
                System.out.println("----------------------------------");
                System.out.println("1. List unread tickets");
                System.out.println("2. Search tickets");
                System.out.println("3. Read full ticket");
                System.out.println("4. Reply to ticket");
                System.out.println("5. Label ticket IN_PROGRESS");
                System.out.println("6. Trash a ticket");
                System.out.println("0. Exit");
                System.out.println("----------------------------------");
                System.out.println("Choice: ");

                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1":
                        listUnreadTickets(service);
                        break;

                    case "2":
                        System.out.println("Enter search query: ");
                        String query = scanner.nextLine();
                        searchTickets(service, query);
                        break;

                    case "3":
                        System.out.println("Enter message ID to read: ");
                        String readMessageID = scanner.nextLine();
                        HelpDeskBot.readTicket(service, readMessageID);

                    case "4":
                        System.out.println("Enter message ID to reply to: ");
                        String replyMessageID = scanner.nextLine();

                        System.out.println("Enter reply text");
                        String replyBody = scanner.nextLine();

                        replyToTicket(service, replyMessageID, replyBody);

                        break;

                    case "5":
                        System.out.println("Enter message ID to label IN_PROGRESS: ");
                        String labelMessageID = scanner.nextLine();

                        applyLabel(service, labelMessageID, "IN_PROGRESS");

                        break;

                    case "6":
                        System.out.println("Enter message ID to trash: ");
                        String trashMessageID = scanner.nextLine();

                        System.out.println("Are you sure? (y/n): ");
                        String confirmation = scanner.nextLine();

                        if (confirmation.equalsIgnoreCase("y"))
                            trashTicket(service, trashMessageID);
                        else
                            System.out.println("Trash cancelled.");
                        break;

                    case "0":
                        isRunning = false;
                        System.out.println("Exiting program");
                        break;

                    default:
                        System.out.println("Invalid input. Please choose 0-6.");

                }
            }
            scanner.close();
        } catch (Exception e) {
            System.out.println("Error occured: " + e.getMessage());
        }
    }
}
