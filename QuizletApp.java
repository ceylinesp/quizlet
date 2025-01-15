import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class QuizletApp {

    private JFrame frame;
    private JTextArea displayArea;
    private List<String[]> wordPairs;

    public QuizletApp() {
        // Initialize word pairs list
        wordPairs = new ArrayList<>();

        // Set up the frame
        frame = new JFrame("Quizlet-like App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Set up the display area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(displayArea);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);

        // Load the CSV file automatically
        loadCSVFile("./words.csv");

        // Show the frame
        frame.setVisible(true);
    }

    private void loadCSVFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            JOptionPane.showMessageDialog(frame, "File not found!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            wordPairs.clear();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    wordPairs.add(new String[]{parts[0].trim(), parts[1].trim()});
                }
            }

            displayArea.setText("Loaded " + wordPairs.size() + " word pairs successfully!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error reading the file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizletApp());
    }
}
