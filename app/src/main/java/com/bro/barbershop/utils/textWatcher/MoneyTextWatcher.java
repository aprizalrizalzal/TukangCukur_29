package com.bro.barbershop.utils.textWatcher;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import com.google.android.material.textfield.TextInputEditText;

import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class MoneyTextWatcher implements TextWatcher {

    public static final NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    private final WeakReference<TextInputEditText> editTextWeakReference;

    public MoneyTextWatcher(TextInputEditText TextInputEditText) {
        editTextWeakReference = new WeakReference<>(TextInputEditText);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setRoundingMode(RoundingMode.FLOOR);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        TextInputEditText textInputEditText = editTextWeakReference.get();
        if (textInputEditText == null || Objects.requireNonNull(textInputEditText.getText()).toString().equals("")) {
            return;
        }
        textInputEditText.removeTextChangedListener(this);

        BigDecimal parsed = parseCurrencyValue(textInputEditText.getText().toString());
        String formatted = numberFormat.format(parsed);

        textInputEditText.setText(formatted);
        textInputEditText.setSelection(formatted.length());
        textInputEditText.addTextChangedListener(this);
    }

    public static String formatCurrency(Double number){
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setRoundingMode(RoundingMode.FLOOR);
        return numberFormat.format(number);
    }

    public static BigDecimal parseCurrencyValue(String value) {
        try {
            String replaceRegex = String.format("[%s,.\\s]", Objects.requireNonNull(numberFormat.getCurrency()).getDisplayName());
            String currencyValue = value.replaceAll(replaceRegex, "");
            return new BigDecimal(currencyValue);
        } catch (Exception e) {
            Log.e("MoneyTextWatcher", e.getMessage(), e);
        }
        return BigDecimal.ZERO;
    }

}
