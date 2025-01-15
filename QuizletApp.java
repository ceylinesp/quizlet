import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;


public class QuizletApp {

    private JFrame frame;
    private JTextArea displayArea;
    private JButton startQuizButton;
    private List<String[]> wordPairs;
    private List<String[]> selectedWords;
    private Map<String, Integer[]> wordStats; // Tracks correct and attempted counts for each word
    private JPanel optionPanel;
    private String[] currentQuestion;
    private List<String[]> quizPool;
    private Map<String, Integer> correctAnswersInRound;
    private String lastQuestionWord;
    private int wordsPerRound = 10; 


    public QuizletApp() {
        // Initialize word pairs list and statistics map
        wordPairs = new ArrayList<>();
        selectedWords = new ArrayList<>();
        wordStats = new HashMap<>();

        // Set up the frame
        frame = new JFrame("Quizlet-like App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.getContentPane().setBackground(new Color(240, 248, 255));
        frame.setLocationRelativeTo(null);


        JLabel headerLabel = new JLabel("Quizlet-like App", JLabel.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(new Color(33, 37, 41)); // Dark gray text
        frame.add(headerLabel, BorderLayout.NORTH);
        

        // Set up the display area
        displayArea = new JTextArea();
        displayArea.setEditable(false);
        displayArea.setLineWrap(true);
        displayArea.setWrapStyleWord(true);
        displayArea.setFont(new Font("SansSerif", Font.PLAIN, 18));
        displayArea.setBackground(new Color(255, 255, 255)); // White background
        displayArea.setForeground(new Color(33, 37, 41)); // Dark gray text
        displayArea.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        frame.add(scrollPane, BorderLayout.CENTER);
        
        // Set up the start quiz button
        startQuizButton = new JButton("Start Quiz");
        startQuizButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initializeQuiz();
            }
        });

        // Layout setup
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 248, 255));
        buttonPanel.add(startQuizButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);


        optionPanel = new JPanel(new GridLayout(2, 2, 15, 15));
        optionPanel.setBackground(new Color(240, 248, 255));
        optionPanel.setVisible(false);
        frame.add(optionPanel, BorderLayout.EAST);


        // Load the CSV file automatically
        loadCSVFile("./word_pairs.csv");
        selectWordsWithLowestAccuracy();


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

            displayArea.setText("");

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

        selectedWords = new ArrayList<>(wordPairs.subList(0, Math.min(wordsPerRound, wordPairs.size())));


        StringBuilder sb = new StringBuilder("Words to study:\n");
        for (String[] pair : selectedWords) {
            Integer[] stats = wordStats.get(pair[0]);
            sb.append(pair[0]).append(" -  ").append(pair[1]).append("\n");
        }

        displayArea.setText(sb.toString());
    }

    private void initializeQuiz() {
        if (selectedWords.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No words selected! Please select words first.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        correctAnswersInRound = new HashMap<>();
        for (String[] pair : selectedWords) {
            correctAnswersInRound.put(pair[0], 0);
        }

        quizPool = new ArrayList<>(selectedWords);
        lastQuestionWord = null;
        loadNextQuestion();
    }

    private void loadNextQuestion() {
        if (quizPool.isEmpty()) {
            displayArea.setText("Quiz Complete! Accuracy for each word:\n\n" + getAccuracyReport());
            saveStatsToCSV("./word_pairs.csv");
            optionPanel.setVisible(false);
            return;
        }

        // Ensure the next question is different from the last one
        do {
            Collections.shuffle(quizPool);
            currentQuestion = quizPool.get(0);
        } while (currentQuestion[0].equals(lastQuestionWord) && quizPool.size() > 1);

        lastQuestionWord = currentQuestion[0];

        if (new Random().nextBoolean()) {
            // Written question
            displayArea.setText("" + currentQuestion[1] + "");
            optionPanel.setVisible(false);

            String answer = JOptionPane.showInputDialog(frame, "German:");
            handleWrittenAnswer(answer);
        } else {
            // Multiple choice question
            String question = "" + currentQuestion[1] + "";
            displayArea.setText(question);

            List<String> options = generateOptions(currentQuestion[0]);
            displayOptions(options, currentQuestion[1]);
        }
    }

    private void handleWrittenAnswer(String answer) {
        Integer[] stats = wordStats.get(currentQuestion[0]);
        stats[1]++; // Increment attempts

        if (answer != null && answer.equalsIgnoreCase(currentQuestion[0])) {
            stats[0]++; // Increment correct answers
            int count = correctAnswersInRound.get(currentQuestion[0]) + 1;
            correctAnswersInRound.put(currentQuestion[0], count);

            if (count >= 2) {
                quizPool.remove(0); // Remove word from pool after 2 correct answers
            }

            displayArea.setText("Correct!\n\nNext Question...");
            loadNextQuestion();
        } else {
            String retryAnswer = JOptionPane.showInputDialog(frame, "Incorrect! The correct answer was: " + currentQuestion[0] + ".\nType it correctly to proceed:");
            if (retryAnswer != null && retryAnswer.equalsIgnoreCase(currentQuestion[0])) {
                loadNextQuestion();
            } else {
                handleWrittenAnswer(retryAnswer);
            }
        }
    }

    private void displayOptions(List<String> options, String correctAnswer) {
        optionPanel.removeAll();
        optionPanel.setVisible(true);

        for (String option : options) {
            JButton button = new JButton(option);
            button.setFont(new Font("SansSerif", Font.PLAIN, 18));
            button.setBackground(Color.LIGHT_GRAY);
            button.setFocusPainted(false);
            button.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    handleOptionSelection(option, correctAnswer);
                }
            });
            optionPanel.add(button);
        }

        frame.revalidate();
        frame.repaint();
    }

    private void handleOptionSelection(String selectedOption, String correctAnswer) {
        Integer[] stats = wordStats.get(currentQuestion[0]);
        stats[1]++; // Increment attempts

        if (selectedOption.equalsIgnoreCase(correctAnswer)) {
            stats[0]++; // Increment correct answers
            int count = correctAnswersInRound.get(currentQuestion[0]) + 1;
            correctAnswersInRound.put(currentQuestion[0], count);

            if (count >= 2) {
                quizPool.remove(0); // Remove word from pool after 2 correct answers
            }

            displayArea.setText("Correct!\n\nNext Question...");
        } else {
            displayArea.setText("Incorrect! The correct answer was: " + correctAnswer);
        }

        loadNextQuestion();
    }

    private List<String> generateOptions(String correctAnswer) {
        List<String> options = new ArrayList<>();
        options.add(correctAnswer);
        Random random = new Random();

        while (options.size() < 4) {
            String[] randomPair = wordPairs.get(random.nextInt(wordPairs.size()));
            if (!options.contains(randomPair[0])) {
                options.add(randomPair[0]);
            }
        }

        Collections.shuffle(options);
        return options;
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
