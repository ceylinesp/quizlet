import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.awt.Font;
import java.awt.FlowLayout;

public class QuizletApp {

    private JFrame frame;
    private JTextArea displayArea;
    private JButton selectWordsButton;
    private JButton startQuizButton;
    private List<String[]> wordPairs;
    private List<String[]> selectedWords;
    private Map<String, Integer[]> wordStats; // Tracks correct and attempted counts for each word
    private String[] currentQuestion;
    private boolean retryMode;

    public QuizletApp() {
        // Initialize word pairs list and statistics map
        wordPairs = new ArrayList<>();
        selectedWords = new ArrayList<>();
        wordStats = new HashMap<>();
        retryMode = false;

        // Set up the frame
        frame = new JFrame("Quizlet-like App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);

        // Set up the display area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setLineWrap(true);
        displayArea.setWrapStyleWord(true);
        displayArea.setFont(new Font("SansSerif", Font.PLAIN, 18));
        JScrollPane scrollPane = new JScrollPane(displayArea);

        // Set up the select words button
        selectWordsButton = new JButton("Select 3 Words");
        selectWordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectWordsWithLowestAccuracy();
            }
        });

        // Set up the start quiz button
        startQuizButton = new JButton("Start Quiz");
        startQuizButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startQuiz();
            }
        });

        // Layout setup
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.add(selectWordsButton);
        buttonPanel.add(startQuizButton);

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // Load the CSV file automatically
        loadCSVFile("./word_pairs.csv");

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
            wordStats.clear();

            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    wordPairs.add(new String[]{parts[0].trim(), parts[1].trim(), parts[2].trim()});
                    int correct = 0;
                    int attempts = 0;
                    try {
                        correct = Integer.parseInt(parts[2].split("/")[0]);
                        attempts = Integer.parseInt(parts[2].split("/")[1]);
                    } catch (Exception ignored) {
                        // Default to 0 if parsing fails
                    }
                    wordStats.put(parts[0].trim(), new Integer[]{correct, attempts});
                }
            }

            displayArea.setText("Loaded " + wordPairs.size() + " word pairs successfully!");

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(frame, "Error reading the file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void selectWordsWithLowestAccuracy() {
        if (wordPairs.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No words available to select.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Sort the word pairs by their accuracy (correct/attempts)
        wordPairs.sort((a, b) -> {
            Integer[] statsA = wordStats.get(a[0]);
            Integer[] statsB = wordStats.get(b[0]);
            double accuracyA = statsA[1] == 0 ? 0 : (double) statsA[0] / statsA[1];
            double accuracyB = statsB[1] == 0 ? 0 : (double) statsB[0] / statsB[1];
            return Double.compare(accuracyA, accuracyB);
        });

        selectedWords = new ArrayList<>(wordPairs.subList(0, Math.min(3, wordPairs.size())));

        StringBuilder sb = new StringBuilder("Selected 3 Words (Lowest Accuracy):\n\n");
        for (String[] pair : selectedWords) {
            Integer[] stats = wordStats.get(pair[0]);
            double accuracy = stats[1] == 0 ? 0 : (double) stats[0] / stats[1];
            sb.append(pair[0]).append(" - ").append(pair[1]).append(" (Accuracy: ").append(String.format("%.2f", accuracy)).append(")\n");
        }

        displayArea.setText(sb.toString());
    }

    private void startQuiz() {
        if (selectedWords.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No words selected! Please select words first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Map<String, Integer> correctAnswersInRound = new HashMap<>(); // Tracks correct answers in the current round
        for (String[] pair : selectedWords) {
            correctAnswersInRound.put(pair[0], 0);
        }

        List<String[]> quizPool = new ArrayList<>(selectedWords);
        while (!quizPool.isEmpty()) {
            Collections.shuffle(quizPool); // Shuffle the pool for randomness
            String[] currentPair = quizPool.get(0);
            String question = "What is the English word for: " + currentPair[0] + "?";
            String answer = JOptionPane.showInputDialog(frame, question);

            Integer[] stats = wordStats.get(currentPair[0]);
            stats[1]++; // Increment attempts

            if (answer != null && answer.equalsIgnoreCase(currentPair[1])) {
                stats[0]++; // Increment correct answers
                int count = correctAnswersInRound.get(currentPair[0]) + 1;
                correctAnswersInRound.put(currentPair[0], count);

                if (count >= 2) {
                    quizPool.remove(0); // Remove word from pool after 2 correct answers
                }
            } else {
                String correctAnswerPrompt = "Incorrect! The correct answer is: " + currentPair[1] + ".\nType it correctly to proceed:";
                while (true) {
                    String retryAnswer = JOptionPane.showInputDialog(frame, correctAnswerPrompt);
                    if (retryAnswer != null && retryAnswer.equalsIgnoreCase(currentPair[1])) {
                        break; // Move on only if the correct answer is typed
                    }
                }
            }
        }

        displayArea.setText("Quiz Complete! Accuracy for each word:\n\n" + getAccuracyReport());
        saveStatsToCSV("./word_pairs.csv");
    }

    private String getAccuracyReport() {
        StringBuilder sb = new StringBuilder();
        for (String[] pair : wordPairs) {
            Integer[] stats = wordStats.get(pair[0]);
            double accuracy = stats[1] == 0 ? 0 : (double) stats[0] / stats[1];
            sb.append(pair[0]).append(" - Accuracy: ").append(String.format("%.2f", accuracy)).append("\n");
        }
        return sb.toString();
    }

    private void saveStatsToCSV(String filePath) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            for (String[] pair : wordPairs) {
                Integer[] stats = wordStats.get(pair[0]);
                double accuracy = stats[1] == 0 ? 0 : (double) stats[0] / stats[1];
                pw.println(pair[0] + "," + pair[1] + "," + stats[0] + "/" + stats[1]);
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error saving stats to file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new QuizletApp());
    }
}
