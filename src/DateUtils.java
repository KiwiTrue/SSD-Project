import java.time.LocalDate;

public final class DateUtils {

    // Method to return today's date
    public static LocalDate getTodayDate() {
        return LocalDate.now();
    }

    // Method to calculate the date after adding a number of months to a given date
    public static LocalDate calculateDateAfterMonths(LocalDate startDate, int monthsToAdd) {
        return startDate.plusMonths(monthsToAdd);
    }
}
