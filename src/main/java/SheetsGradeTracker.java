import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

public class SheetsGradeTracker {
    // Set during startup (Task 0) — either from user input or from a newly created spreadsheet
    private static String spreadsheetId;
    private static String sheetName = "Sheet1"; // default tab name

    public static void main(String[] args) throws Exception {

        // Full read+write access (for updating, appending, or creating sheets)
        List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);
        final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Sheets service = new Sheets.Builder(httpTransport,
                GsonFactory.getDefaultInstance(),
                GoogleAuthHelper.getCredentials(httpTransport,
                        Collections.singletonList(SheetsScopes.SPREADSHEETS)))
                .setApplicationName("Grade Tracker").build();

        Scanner scanner = new Scanner(System.in);
        SheetsBot.initializeSpreadsheet(service, scanner);
        SheetsBot.runMenu(service, scanner);
    }
}