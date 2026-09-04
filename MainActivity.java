package com.example.bruteforce;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    // Character set sizes
    private static final int LOWERCASE_COUNT = 26;
    private static final int UPPERCASE_COUNT = 26;
    private static final int DIGIT_COUNT = 10;
    private static final int SYMBOL_COUNT = 33;

    // Assumed brute-force speed: 10 billion guesses per second
    private static final double GUESSES_PER_SECOND = 10_000_000_000.0;

    private EditText editTextPassword;
    private CheckBox checkBoxShowPassword;
    private Button buttonTestPassword;
    private ProgressBar progressBarStrength;
    private TextView textViewStrength;
    private TextView textViewCrackingTime;
    private TextView textViewPasswordLength;
    private TextView textViewCharacterTypes;
    private TextView textViewCharacterSet;
    private TextView textViewCombinations;
    private Button buttonRecommendation;
    private Button buttonReset;

    private String currentRecommendation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // Initialize UI components
        editTextPassword = findViewById(R.id.editTextPassword);
        checkBoxShowPassword = findViewById(R.id.checkBoxShowPassword);
        buttonTestPassword = findViewById(R.id.buttonTestPassword);
        progressBarStrength = findViewById(R.id.progressBarStrength);
        textViewStrength = findViewById(R.id.textViewStrength);
        textViewCrackingTime = findViewById(R.id.textViewCrackingTime);
        textViewPasswordLength = findViewById(R.id.textViewPasswordLength);
        textViewCharacterTypes = findViewById(R.id.textViewCharacterTypes);
        textViewCharacterSet = findViewById(R.id.textViewCharacterSet);
        textViewCombinations = findViewById(R.id.textViewCombinations);
        buttonRecommendation = findViewById(R.id.buttonRecommendation);
        buttonReset = findViewById(R.id.buttonReset);

        // Show/Hide password
        checkBoxShowPassword.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {

                    int selection = editTextPassword.getSelectionEnd();

                    if (isChecked) {
                        editTextPassword.setInputType(
                                InputType.TYPE_CLASS_TEXT |
                                        InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        );
                    } else {
                        editTextPassword.setInputType(
                                InputType.TYPE_CLASS_TEXT |
                                        InputType.TYPE_TEXT_VARIATION_PASSWORD
                        );
                    }

                    editTextPassword.setSelection(selection);
                }
        );

        // Test password
        buttonTestPassword.setOnClickListener(v -> performAnalysis());

        // Show recommendation
        buttonRecommendation.setOnClickListener(
                v -> showRecommendationDialog()
        );

        // Reset
        buttonReset.setOnClickListener(v -> resetAll());
    }

    /**
     * Analyzes the password and updates the result displayed on screen.
     */
    private void performAnalysis() {

        String password = editTextPassword.getText().toString();

        // If no password was entered, reset the screen
        if (password.isEmpty()) {
            resetAll();
            return;
        }

        int length = password.length();

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

        /*
         * Detect character types.
         * ASCII ranges are used so that the character-set calculation
         * matches the assumed character counts.
         */
        for (char c : password.toCharArray()) {

            if (c >= 'a' && c <= 'z') {
                hasLower = true;

            } else if (c >= 'A' && c <= 'Z') {
                hasUpper = true;

            } else if (c >= '0' && c <= '9') {
                hasDigit = true;

            } else {
                hasSymbol = true;
            }
        }

        // Calculate character-set size
        int charsetSize = 0;
        int typeCount = 0;

        StringBuilder types = new StringBuilder();

        if (hasLower) {
            charsetSize += LOWERCASE_COUNT;
            typeCount++;
            types.append("Lowercase, ");
        }

        if (hasUpper) {
            charsetSize += UPPERCASE_COUNT;
            typeCount++;
            types.append("Uppercase, ");
        }

        if (hasDigit) {
            charsetSize += DIGIT_COUNT;
            typeCount++;
            types.append("Digits, ");
        }

        if (hasSymbol) {
            charsetSize += SYMBOL_COUNT;
            typeCount++;
            types.append("Symbols, ");
        }

        // Remove final comma and space
        if (types.length() > 0) {
            types.setLength(types.length() - 2);
        }

        /*
         * Brute-force calculation:
         *
         * Total combinations = character set size ^ password length
         */
        double combinations =
                Math.pow(charsetSize, length);

        /*
         * Estimated cracking time based on the assumed
         * brute-force speed.
         */
        double secondsToCrack =
                combinations / GUESSES_PER_SECOND;

        // Update password information
        textViewPasswordLength.setText(
                getString(R.string.length_default)
                        .replace("—", String.valueOf(length))
        );

        textViewCharacterTypes.setText(
                getString(R.string.characters_default)
                        .replace("—", types.toString())
        );

        textViewCharacterSet.setText(
                getString(R.string.charset_default)
                        .replace("—", String.valueOf(charsetSize))
        );

        DecimalFormat df =
                new DecimalFormat("#.##E0");

        textViewCombinations.setText(
                getString(R.string.combinations_default)
                        .replace("—", df.format(combinations))
        );

        textViewCrackingTime.setText(
                getString(R.string.cracking_time_default)
                        .replace("—", formatTime(secondsToCrack))
        );

        // Calculate and display password strength
        updateStrengthDisplay(
                length,
                typeCount,
                secondsToCrack
        );
    }

    /**
     * Calculates the password strength using:
     * - Password length
     * - Number of character types
     * - Estimated cracking time
     */
    private void updateStrengthDisplay(
            int length,
            int typeCount,
            double seconds) {

        int score = 0;

        // Length scoring
        if (length >= 8) {
            score++;
        }

        if (length >= 12) {
            score++;
        }

        // Character variety
        if (typeCount >= 3) {
            score++;
        }

        // More than one year to theoretically crack
        if (seconds > 31_536_000) {
            score++;
        }

        String strength;
        int colorRes;
        int progress;
        String recommendation;

        if (score <= 1) {

            strength = "Very Weak";
            colorRes = R.color.strength_very_weak;
            progress = 25;
            recommendation =
                    "Try a longer password with more variety of characters.";

        } else if (score == 2) {

            strength = "Weak";
            colorRes = R.color.strength_weak;
            progress = 50;
            recommendation =
                    "Adding numbers or special characters will improve security.";

        } else if (score == 3) {

            strength = "Good";
            colorRes = R.color.strength_good;
            progress = 75;
            recommendation =
                    "This is a good password. For even better security, make it longer.";

        } else {

            strength = "Strong";
            colorRes = R.color.strength_strong;
            progress = 100;
            recommendation =
                    "Excellent! This password is very difficult to crack.";
        }

        // Update strength text
        textViewStrength.setText(
                getString(R.string.strength_default)
                        .split(":")[0]
                        + ": "
                        + strength
        );

        // Update strength color
        int color =
                ContextCompat.getColor(this, colorRes);

        textViewStrength.setTextColor(color);

        // Update progress bar
        progressBarStrength.setProgress(progress);

        progressBarStrength.setProgressTintList(
                ColorStateList.valueOf(color)
        );

        // Save recommendation
        currentRecommendation = recommendation;

        // Show recommendation button
        buttonRecommendation.setVisibility(
                android.view.View.VISIBLE
        );
    }

    /**
     * Converts seconds into a readable cracking-time format.
     */
    private String formatTime(double seconds) {

        if (seconds < 1) {
            return "Instant";
        }

        if (seconds < 60) {
            return (int) seconds + " seconds";
        }

        if (seconds < 3600) {
            return (int) (seconds / 60) + " minutes";
        }

        if (seconds < 86400) {
            return (int) (seconds / 3600) + " hours";
        }

        if (seconds < 2_592_000) {
            return (int) (seconds / 86400) + " days";
        }

        if (seconds < 31_536_000) {
            return (int) (seconds / 2_592_000) + " months";
        }

        if (seconds < 31_536_000_000.0) {
            return (int) (seconds / 31_536_000) + " years";
        }

        return "Centuries";
    }

    /**
     * Displays the recommendation dialog.
     */
    private void showRecommendationDialog() {

        new AlertDialog.Builder(this)
                .setTitle(R.string.recommendation_dialog_title)
                .setMessage(currentRecommendation)
                .setPositiveButton(
                        R.string.button_close,
                        null
                )
                .show();
    }

    /**
     * Resets all password analysis results.
     */
    private void resetAll() {
        editTextPassword.setText("");
        checkBoxShowPassword.setChecked(false);
        progressBarStrength.setProgress(0);
        progressBarStrength.setProgressTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this,
                                R.color.strength_empty
                        )
                )
        );
        textViewStrength.setText(
                R.string.strength_default
        );
        textViewStrength.setTextColor(
                ContextCompat.getColor(
                        this,
                        R.color.text_primary
                )
        );
        textViewCrackingTime.setText(
                R.string.cracking_time_default
        );
        textViewPasswordLength.setText(
                R.string.length_default
        );
        textViewCharacterTypes.setText(
                R.string.characters_default
        );
        textViewCharacterSet.setText(
                R.string.charset_default
        );
        textViewCombinations.setText(
                R.string.combinations_default
        );
        buttonRecommendation.setVisibility(
                android.view.View.GONE
        );
        currentRecommendation = "";
    }
}