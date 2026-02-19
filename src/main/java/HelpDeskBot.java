import com.google.api.services.gmail.Gmail;

public class HelpDeskBot {

    public static void listUnreadTickets() {
        Gmail service = GoogleAuthHelper.getService();
    }

    public static void searchTickets(Gmail service, String query)
    {

    }

    public static void readTicket(Gmail service, String messageId)
    {

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
