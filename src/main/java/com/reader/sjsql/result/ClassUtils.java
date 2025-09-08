package com.reader.sjsql.result;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public final class ClassUtils {

    static void setFieldValue(Object instance, Field field, Object value)
        throws NoSuchFieldException, IllegalAccessException {
        MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup())
                     .findVarHandle(field.getDeclaringClass(), field.getName(), field.getType())
                     .set(instance, value);
    }

    public static Object getFieldValue(Object instance, Field field)
        throws Throwable {
        final MethodHandle getter = MethodHandles
            .privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup())
            .findGetter(field.getDeclaringClass(), field.getName(), field.getType());
        if (getter == null) {
            return null;
        }
        return getter.invoke(instance);
    }

    public static List<Field> getDeclaredFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }

    static Field getFieldByName(Class<?> clazz, String fieldName) throws Exception {
        String camelCaseName = toCamelCase(fieldName);
        try {
            return clazz.getDeclaredField(camelCaseName);
        } catch (NoSuchFieldException e) {
            // ignore
        }

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(camelCaseName)) {
                return field;
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        while (superClass != null && superClass != Object.class) {
            try {
                return getFieldByName(superClass, camelCaseName);
            } catch (NoSuchFieldException ex) {
                superClass = superClass.getSuperclass();
            }
        }
        return null;
    }

    static String toCamelCase(String fieldName) {
        if (fieldName == null || !fieldName.contains("_")) {
            return fieldName;
        }

        StringBuilder result = new StringBuilder();
        String[] parts = fieldName.split("_");
        result.append(parts[0]);

        for (int i = 1; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                result.append(Character.toUpperCase(parts[i].charAt(0)))
                      .append(parts[i].substring(1).toLowerCase());
            }
        }

        return result.toString();
    }

    public static String toSnakeCase(String fieldName) {
        StringBuilder snakeCase = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    snakeCase.append("_");
                }
                snakeCase.append(Character.toLowerCase(c));
            } else {
                snakeCase.append(c);
            }
        }
        return snakeCase.toString();
    }


    public static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive()
            || clazz == String.class
            || clazz == Byte.class
            || clazz == Short.class
            || clazz == Integer.class
            || clazz == Long.class
            || clazz == Boolean.class
            || clazz == Double.class
            || clazz == Float.class
            || clazz == BigDecimal.class
            || clazz == BigInteger.class
            || clazz == LocalDateTime.class
            || clazz == LocalDate.class
            || clazz == Date.class;
    }
}
