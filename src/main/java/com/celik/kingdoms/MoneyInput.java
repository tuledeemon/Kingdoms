package com.celik.kingdoms;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses exact whole-currency inputs with human-friendly magnitude suffixes. */
public final class MoneyInput {
    private static final Pattern INPUT = Pattern.compile("^\\$?([+]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*([a-z]*)$", Pattern.CASE_INSENSITIVE);
    private static final String HELP = "Use a whole amount such as 1500, 1.5k, 10m, or 2 billion.";

    public static long parse(String input) {
        if(input==null)throw new IllegalArgumentException(HELP);
        String cleaned=input.trim().replace(",","").replace("_","");
        Matcher match=INPUT.matcher(cleaned);
        if(!match.matches())throw new IllegalArgumentException(HELP);
        long multiplier=switch(match.group(2).toLowerCase(Locale.ROOT)) {
            case "" -> 1L;
            case "k", "thousand", "thousands", "grand" -> 1_000L;
            case "m", "mil", "mill", "million", "millions" -> 1_000_000L;
            case "b", "bil", "bill", "billion", "billions" -> 1_000_000_000L;
            case "t", "tril", "trillion", "trillions" -> 1_000_000_000_000L;
            default -> throw new IllegalArgumentException(HELP);
        };
        try {
            long result=new BigDecimal(match.group(1)).multiply(BigDecimal.valueOf(multiplier)).longValueExact();
            if(result<0)throw new IllegalArgumentException(HELP);
            return result;
        } catch(ArithmeticException e) {
            throw new IllegalArgumentException("The amount must resolve to a whole number within the supported range.");
        }
    }

    public static long parsePositive(String input) {
        long value=parse(input);
        if(value<=0)throw new IllegalArgumentException("Amount must be positive.");
        return value;
    }

    private MoneyInput() {}
}
