package com.celik.kingdoms;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/** Edits a validated copy so a rejected setting never changes the running configuration. */
final class AdminSettings {
    static List<String> keys(Class<?> type) {
        return Arrays.stream(type.getFields()).filter(f -> f.getType().isPrimitive() || f.getType() == String.class).map(Field::getName).sorted().toList();
    }
    static void set(Object target, String key, String value) {
        try {
            Field field = target.getClass().getField(key);
            Class<?> type = field.getType();
            if (type == boolean.class) {
                if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) throw new IllegalArgumentException("Use true or false.");
                field.setBoolean(target, Boolean.parseBoolean(value));
            } else if (type == int.class) field.setInt(target, Integer.parseInt(value));
            else if (type == long.class) field.setLong(target, MoneyInput.parse(value));
            else if (type == float.class) field.setFloat(target, (float) number(value));
            else if (type == double.class) field.setDouble(target, number(value));
            else if (type == String.class) field.set(target, value);
            else throw new IllegalArgumentException("Edit this collection in config.json, or use its dedicated command.");
        } catch (ReflectiveOperationException e) { throw new IllegalArgumentException("Unknown setting: " + key); }
    }
    static double number(String value) {
        value = value.trim();
        double result = value.endsWith("%") ? Double.parseDouble(value.substring(0, value.length()-1))/100 : Double.parseDouble(value);
        if (!Double.isFinite(result)) throw new IllegalArgumentException("Use a finite number.");
        return result;
    }
    static double chance(String value) {
        double result = number(value);
        if (result < 0 || result > 1) throw new IllegalArgumentException("Use a probability from 0 to 1, or a percentage such as 0.1%.");
        return result;
    }
    static void update(KingdomService service, Consumer<Config> edit) {
        Config next = Store.JSON.fromJson(Store.JSON.toJson(service.config), Config.class);
        edit.accept(next);
        next.validate();
        service.airDrops.validateConfiguration(next.airDrops);
        service.artifacts.validateConfiguration(next.artifacts);
        KingdomsMod.configStore.write("config.json", next);
        service.config = next;
    }
    private AdminSettings() {}
}
