
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TimePickerDialog extends Dialog<String> {
    private final Spinner<Integer> hourSpinner;
    private final Spinner<Integer> minuteSpinner;
    private final Spinner<Integer> toHourSpinner;
    private final Spinner<Integer> toMinuteSpinner;

    public TimePickerDialog() {
        setTitle("Select Time Range");
        
        // Create spinners
        hourSpinner = createHourSpinner();
        minuteSpinner = createMinuteSpinner();
        toHourSpinner = createHourSpinner();
        toMinuteSpinner = createMinuteSpinner();

        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("From:"), 0, 0);
        grid.add(hourSpinner, 1, 0);
        grid.add(new Label(":"), 2, 0);
        grid.add(minuteSpinner, 3, 0);
        grid.add(new Label("To:"), 0, 1);
        grid.add(toHourSpinner, 1, 1);
        grid.add(new Label(":"), 2, 1);
        grid.add(toMinuteSpinner, 3, 1);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                String from = String.format("%02d:%02d", hourSpinner.getValue(), minuteSpinner.getValue());
                String to = String.format("%02d:%02d", toHourSpinner.getValue(), toMinuteSpinner.getValue());
                return from + "-" + to;
            }
            return null;
        });
    }

    private Spinner<Integer> createHourSpinner() {
        Spinner<Integer> spinner = new Spinner<>(0, 23, 9);
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        return spinner;
    }

    private Spinner<Integer> createMinuteSpinner() {
        Spinner<Integer> spinner = new Spinner<>(0, 59, 0);
        spinner.setEditable(true);
        spinner.setPrefWidth(70);
        return spinner;
    }
}