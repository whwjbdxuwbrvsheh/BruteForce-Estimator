package com.example.bruteforce;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.*;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    private static final int LOWERCASE_COUNT = 26;
    private static final int UPPERCASE_COUNT = 26;
    private static final int DIGIT_COUNT = 10;
    private static final int SYMBOL_COUNT = 33;
    private static final double GUESSES_PER_SECOND = 10_000_000_000.0;

    private EditText editTextPassword;
    private CheckBox checkBoxShowPassword;
    private ProgressBar progressBarStrength;
    private TextView textViewStrength, textViewCrackingTime;
    private TextView textViewPasswordLength, textViewCharacterTypes;
    private TextView textViewCharacterSet, textViewCombinations;
    private Button buttonRecommendation;

    private String currentRecommendation = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });

        editTextPassword = findViewById(R.id.editTextPassword);
        checkBoxShowPassword = findViewById(R.id.checkBoxShowPassword);
        progressBarStrength = findViewById(R.id.progressBarStrength);
        textViewStrength = findViewById(R.id.textViewStrength);
        textViewCrackingTime = findViewById(R.id.textViewCrackingTime);
        textViewPasswordLength = findViewById(R.id.textViewPasswordLength);
        textViewCharacterTypes = findViewById(R.id.textViewCharacterTypes);
        textViewCharacterSet = findViewById(R.id.textViewCharacterSet);
        textViewCombinations = findViewById(R.id.textViewCombinations);
        buttonRecommendation = findViewById(R.id.buttonRecommendation);

        checkBoxShowPassword.setOnCheckedChangeListener((button, checked) -> {
            int selection = editTextPassword.getSelectionEnd();

            editTextPassword.setInputType(
                    InputType.TYPE_CLASS_TEXT |
                            (checked
                                    ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                                    : InputType.TYPE_TEXT_VARIATION_PASSWORD));

            editTextPassword.setSelection(selection);
        });

        findViewById(R.id.buttonTestPassword)
                .setOnClickListener(v -> performAnalysis());

        buttonRecommendation.setOnClickListener(
                v -> showRecommendationDialog());

        findViewById(R.id.buttonReset)
                .setOnClickListener(v -> resetAll());
    }

    private void performAnalysis() {
        String password = editTextPassword.getText().toString();

        if (password.isEmpty()) {
            resetAll();
            return;
        }

        if (password.length() > 64) {
            editTextPassword.setError("Password must be 64 characters or fewer.");
            return;
        }

        editTextPassword.setError(null);

        int length = password.length();
        int charsetSize = 0;
        int typeCount = 0;

        boolean hasLower = false;
        boolean hasUpper = false;
        boolean hasDigit = false;
        boolean hasSymbol = false;

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

        if (types.length() > 0) {
            types.setLength(types.length() - 2);
        }

        double combinations = Math.pow(charsetSize, length);
        double seconds = combinations / GUESSES_PER_SECOND;

        textViewPasswordLength.setText(
                getString(R.string.length_result, length));

        textViewCharacterTypes.setText(
                getString(R.string.characters_result, types.toString()));

        textViewCharacterSet.setText(
                getString(R.string.charset_result, charsetSize));

        textViewCombinations.setText(
                getString(
                        R.string.combinations_result,
                        new DecimalFormat("#.##E0").format(combinations)));

        textViewCrackingTime.setText(
                getString(
                        R.string.cracking_time_result,
                        formatTime(seconds)));

        updateStrength(length, typeCount, seconds);
    }

    private void updateStrength(int length, int typeCount, double seconds) {
        int score = 0;

        if (length >= 8) score++;
        if (length >= 12) score++;
        if (typeCount >= 3) score++;
        if (seconds > 31_536_000) score++;

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

        int color = ContextCompat.getColor(this, colorRes);

        textViewStrength.setText(
                getString(R.string.strength_result, strength));
        textViewStrength.setTextColor(color);

        progressBarStrength.setProgress(progress);
        progressBarStrength.setProgressTintList(
                ColorStateList.valueOf(color));

        currentRecommendation = recommendation;
        buttonRecommendation.setVisibility(View.VISIBLE);
    }

    private String formatTime(double seconds) {
        if (seconds < 1) return "Instant";
        if (seconds < 60) return (int) seconds + " seconds";
        if (seconds < 3600) return (int) (seconds / 60) + " minutes";
        if (seconds < 86400) return (int) (seconds / 3600) + " hours";
        if (seconds < 2_592_000)
            return (int) (seconds / 86400) + " days";
        if (seconds < 31_536_000)
            return (int) (seconds / 2_592_000) + " months";
        if (seconds < 31_536_000_000.0)
            return (int) (seconds / 31_536_000) + " years";

        return "Centuries";
    }

    private void showRecommendationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.recommendation_dialog_title)
                .setMessage(currentRecommendation)
                .setPositiveButton(R.string.button_close, null)
                .show();
    }

    private void resetAll() {
        editTextPassword.setText("");
        checkBoxShowPassword.setChecked(false);

        progressBarStrength.setProgress(0);
        progressBarStrength.setProgressTintList(
                ColorStateList.valueOf(
                        ContextCompat.getColor(
                                this, R.color.strength_empty)));

        textViewStrength.setText(R.string.strength_default);
        textViewStrength.setTextColor(
                ContextCompat.getColor(this, R.color.text_primary));

        textViewCrackingTime.setText(R.string.cracking_time_default);
        textViewPasswordLength.setText(R.string.length_default);
        textViewCharacterTypes.setText(R.string.characters_default);
        textViewCharacterSet.setText(R.string.charset_default);
        textViewCombinations.setText(R.string.combinations_default);

        buttonRecommendation.setVisibility(View.GONE);
        currentRecommendation = "";
    }
}
