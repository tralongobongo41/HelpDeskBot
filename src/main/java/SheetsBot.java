import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;


public class SheetsBot {

    //Instance variables
    private static String spreadsheetId;
    private static final String NEW_SHEET_NAME = "Sheet1";

    //Helper
    public static String getCellValue(List<Object> row, int index)
    {
        if(row == null || index >= row.size())
            return "";

        Object value = row.get(index);
        return (value == null) ? "" : value.toString();
    }

    //Task 0
    public static void initializeSpreadsheet(Sheets service, Scanner scanner) throws IOException {
        System.out.println("========================================");
        System.out.println("Grade Tracker — Startup");
        System.out.println("========================================");
        System.out.println("How would you like to proceed?");
        System.out.println("1. Load an existing spreadsheet by ID");
        System.out.println("2. Create a new spreadsheet");
        System.out.println("\nChoice: ");


        String title = "";
        String choice = scanner.nextLine().trim();

        if (choice.equals("1"))
        {
            System.out.println("Enter spreadsheet ID: ");
            String inputId = scanner.nextLine().trim();

            try
            {
                Spreadsheet sheet = service.spreadsheets().get(inputId).execute();
                title = sheet.getProperties().getTitle();
                spreadsheetId = inputId;
                System.out.println("? Found: " + title);
            }
            catch (GoogleJsonResponseException e)
            {
                if (e.getStatusCode() == 404) {
                    System.err.println("? Spreadsheet not found. Check the ID and try again.");
                } else if (e.getStatusCode() == 403) {
                    System.err.println("? Access denied. Make sure you share the sheet with your Google account.");
                } else {
                    System.err.println("? Error: " + e.getMessage());
                }
                System.exit(1);
            }

            System.out.println("\n? Ready. Working with spreadsheet: " + title);
            System.out.println("(ID: " + spreadsheetId + ")");
            System.out.println("========================================");
        }
        else if(choice.equals("2"))
        {
            System.out.println("Enter title for new spreadsheet: ");
            title = scanner.nextLine().trim();


            Spreadsheet newSpreadsheet = new Spreadsheet()
                    .setProperties(new SpreadsheetProperties()
                            .setTitle(title))           // the file name in Google Drive
                    .setSheets(List.of(new Sheet()
                            .setProperties(new SheetProperties()
                                    .setTitle(NEW_SHEET_NAME))));         // the tab name inside the file

            Spreadsheet created = service.spreadsheets().create(newSpreadsheet).execute();
            spreadsheetId = created.getSpreadsheetId();  // store for all subsequent tasks
            String url    = created.getSpreadsheetUrl();

            // Now write the header row so the file isn't completely empty
            List<List<Object>> header = List.of(
                    List.of("Name", "Student ID", "Grade", "Score", "Notes")
            );
            service.spreadsheets().values()
                    .update(spreadsheetId, NEW_SHEET_NAME + "!A1:E1", new ValueRange().setValues(header))
                    .setValueInputOption("RAW")
                    .execute();

            System.out.println("Header row written to " + NEW_SHEET_NAME + "!A1:E1"); //check sheetName!!! *note

            System.out.println("\n? Ready. Working with spreadsheet: " + title);
            System.out.println("(ID: " + spreadsheetId + ")");
            System.out.println("========================================");
        }
        else
        {
            System.out.println("Invalid choice. Please restart.");
            System.exit(1);
        }
    }

    public static void displayAllStudents(Sheets service) throws IOException {
        // Read just cell B2
        ValueRange singleCell = service.spreadsheets().values()
                .get(spreadsheetId, "Sheet1!A:E")
                .execute();

        List<List<Object>> vals = singleCell.getValues();

        if (vals == null || vals.size() <= 1) {
            System.out.println("No student records found.");
            return;
        }

        System.out.println("");




    }

    public static void searchByName(Sheets service, String query)
    {

    }

    public static void addStudent(Sheets service)
    {

    }

    public static void updateGrade(Sheets service)
    {

    }

    public static void generateReport(Sheets service)
    {

    }

    public static void exportFiltered(Sheets service)
    {

    }

    public static void runMenu(Sheets service, Scanner scanner)
    {

    }

}
