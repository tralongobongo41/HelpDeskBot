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

            System.out.println("Header row written to " + NEW_SHEET_NAME + "!A1:E1");

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

        System.out.println("\n--- Class Roster (" + + (vals.size() - 1) + "students) --------------------------------");
        System.out.printf("%-3s | %-16s | %-8s | %-5s | %-5s | %s%n", "#", "Name", "ID", "Grade", "Score", "Notes");
        System.out.println("----|----------------|--------|-------|-------|---------------------");

        for(int i = 1; i < vals.size(); i++)
        {
            List<Object> row = vals.get(i);
            String name = getCellValue(row, 0);
            String id = getCellValue(row, 1);
            String grade = getCellValue(row, 2);
            String score = getCellValue(row, 3);
            String notes = getCellValue(row, 4);

            System.out.printf("%-3s | %-16s | %-8s | %-5s | %-5s | %s%n", i, name, id, grade, score, notes);
        }
        System.out.println("--------------------------------------------------------------------");
        System.out.println("Total: " + (vals.size() - 1) + " student(s)");

    }

    public static void searchByName(Sheets service, Scanner scanner) throws IOException
    {
        System.out.print("Enter name to find: ");
        String query = scanner.nextLine().trim();


        ValueRange response = service.spreadsheets().values().get(spreadsheetId, NEW_SHEET_NAME + "!A:E").execute();
        List<List<Object>> allRows = response.getValues();
        List<List<Object>> matches = new ArrayList<>();

        matches.add(allRows.get(0));

        for(int i = 0; i < allRows.size(); i++)
        {
            if(getCellValue(allRows.get(i), 0).toLowerCase().contains(query.toLowerCase()))
            {
                List<Object> row = new ArrayList<>(allRows.get(i));
                row.add(0, i + 1);
                matches.add(row);
            }
        }

        if(matches.size() <= 1)
        {
            System.out.println("? Zero matches found for: " + query);
        }
        else
        {
            System.out.println( (matches.size() - 1) + " match(es) found.");
        }
    }

    public static void addStudent(Sheets service, Scanner scanner) throws IOException
    {
        System.out.println("\n--- Add New Student");

        System.out.println("Enter Name: ");
        String name = scanner.nextLine().trim();

        System.out.println("Enter Student ID: ");
        String id = scanner.nextLine().trim();

        if(name.isEmpty() || id.isEmpty())
        {
            System.out.println("Error: Invalid Name or ID Input");
            return;
        }

        System.out.println("Enter grade letter: ");
        String grade = scanner.nextLine().trim().toUpperCase();

        System.out.println("Enter score: ");
        String score = scanner.nextLine().trim();

        System.out.println("Enter notes: ");
        String notes = scanner.nextLine().trim();


        List<List<Object>> newRows = List.of(List.of(name, id, grade, score, notes));

        ValueRange appendBody = new ValueRange().setValues(newRows);

        AppendValuesResponse appendResult = service.spreadsheets().values()
                .append(spreadsheetId, NEW_SHEET_NAME+ "!A1", appendBody)
                .setValueInputOption("USER_ENTERED")
                .setInsertDataOption("INSERT_ROWS")   // INSERT_ROWS shifts existing data down; OVERWRITE replaces
                .execute();

        System.out.println("Student added at: " + appendResult.getUpdates().getUpdatedRange());
    }

    public static void updateGrade(Sheets service, Scanner scanner) throws IOException
    {
        System.out.println("\n--- Update Student Grade ---");

        System.out.println("Enter the row number to update: ");
        int rowNumber = Integer.parseInt(scanner.nextLine().trim());


        String range = NEW_SHEET_NAME + "!A" + rowNumber + ":B" + rowNumber;
        ValueRange checkRow = service.spreadsheets().values()
                .get(spreadsheetId, range).execute();

        List<List<Object>> rowData = checkRow.getValues();
        if(rowData == null || rowData.isEmpty())
        {
            System.out.println("Error: No student found at row " + rowNumber);
            return;
        }

        String studentName = rowData.get(0).get(0).toString();
        System.out.println("Updating record for: " + studentName);


        System.out.println("Enter new grade letter: ");
        String newGrade = scanner.nextLine().trim().toUpperCase();

        System.out.println("Enter new score: ");
        int newScore = Integer.parseInt(scanner.nextLine().trim());


        List<List<Object>> updatedValues = List.of(List.of(newGrade, newScore));

        ValueRange body = new ValueRange().setValues(updatedValues);


        range = NEW_SHEET_NAME + "!C" + rowNumber + ":D" + rowNumber;

        UpdateValuesResponse result = service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("USER_ENTERED")
                .execute();

        System.out.println("Updated " + result.getUpdatedCells() + " cells at " + range);
    }

    public static void generateReport(Sheets service, Scanner scanner) throws IOException
    {
        System.out.println("\n--- Generating Class Report ---");

        String range = NEW_SHEET_NAME + "!D2:D";

        ValueRange response = service.spreadsheets().values()
                .get(spreadsheetId, range).execute();

        List<List<Object>> values = response.getValues();

        if(values == null || values.isEmpty())
        {
            System.out.println("Error: No student scores found.");
            return;
        }


        double total = 0;
        int high = Integer.MIN_VALUE;
        int low = Integer.MAX_VALUE;
        int count = 0;

        for(List<Object> row: values)
        {
            if(!row.isEmpty())
            {
                try{
                    int score = Integer.parseInt(row.get(0).toString());
                    total += score;
                    if(score > high) high = score;
                    if(score < low) low = score;
                    count++;
                } catch (NumberFormatException e){
                    continue;
                }
            }
        }

        if(count == 0)
        {
            System.out.println("Error: No valid numeric scores found");
            return;
        }

        double average = total / count;

        Spreadsheet spreadsheet = service.spreadsheets().get(spreadsheetId).execute();
        List<Sheet> sheets = spreadsheet.getSheets();
        boolean reportExists = false;


        for(Sheet s: sheets)
        {
            if(s.getProperties().getTitle().equals("Report")){
                reportExists = true;
                break;
            }
        }

        if(!reportExists)
        {
            Request addSheetRequest = new Request().setAddSheet(new AddSheetRequest().setProperties(new SheetProperties().setTitle("Report")));

            BatchUpdateSpreadsheetRequest body = new BatchUpdateSpreadsheetRequest()
                    .setRequests(List.of(addSheetRequest));

            service.spreadsheets().batchUpdate(spreadsheetId, body).execute();
            System.out.println("Created new report tab.");
        }

        List<List<Object>> reportContent = List.of(
                List.of("CLASS SUMMARY REPORT", ""),
                List.of("-----------------", "-------"),
                List.of("Average Score:", average),
                List.of("Highest Score:", high),
                List.of("Lowest Score:", low),
                List.of("Total Students:", count)
        );

        ValueRange body = new ValueRange().setValues(reportContent);

        service.spreadsheets().values()
                .update(spreadsheetId, "Report!A1", body)
                .setValueInputOption("USER_ENTERED")
                .execute();

        System.out.println("Report written to 'Report' tab.");
        System.out.printf("(Average: %.2f | High: %d | Low: %d)%n", average, high, low);
    }

    public static void exportFiltered(Sheets service, Scanner scanner) throws IOException
    {
        System.out.println("\n--- Generating Class Report ---");

    }

    public static void runMenu(Sheets service, Scanner scanner) throws IOException
    {

        boolean running = true;

        while(running) {
            System.out.println("\n=========================================");
            System.out.println("            GRADE TRACKER MENU           ");
            System.out.println("=========================================");
            System.out.println("1. Display All");
            System.out.println("2. Search");
            System.out.println("3. Add");
            System.out.println("4. Update");
            System.out.println("5. Report");
            System.out.println("6. Export");
            System.out.println("0. Exit");
            System.out.println("Selection: ");
            String choice = scanner.nextLine().trim();


            if (choice.equals("1")) displayAllStudents(service);
            else if (choice.equals("2")) searchByName(service, scanner);
            else if (choice.equals("3")) addStudent(service, scanner);
            else if (choice.equals("4")) updateGrade(service, scanner);
            else if (choice.equals("5")) generateReport(service, scanner);
            else if (choice.equals("6")) exportFiltered(service, scanner);
            else if (choice.equals("0")) {
                running = false;
                System.out.println("Exiting Program.");
            }
            else System.out.println("Invalid choice");

        }
    }

}
