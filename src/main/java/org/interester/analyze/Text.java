package org.interester.analyze;

import lombok.experimental.UtilityClass;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@UtilityClass
public class Text {

    public String languageName(String lang) {
        String name = Locale.forLanguageTag(lang).getDisplayLanguage(Locale.ENGLISH);
        return name.isEmpty() || name.equals(lang) ? lang : name;
    }

    public String monthName(String twoDigitMonth, TextStyle style) {
        return Month.of(Integer.parseInt(twoDigitMonth)).getDisplayName(style, Locale.ENGLISH);
    }

    public String pct(double value) {
        return String.format(Locale.ENGLISH, "%+.1f%%", value);
    }

    public String number(long value) {
        return String.format(Locale.ENGLISH, "%,d", value);
    }
}
