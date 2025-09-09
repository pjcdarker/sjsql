package com.reader.sjsql.result;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ClassUtils {

    private static final Map<Class<?>, List<Field>> persistent_field_cache = new ConcurrentHashMap<>(32);
    private static final Map<Class<?>, List<Field>> all_declared_field_cache = new ConcurrentHashMap<>(32);
    private static final Map<Class<?>, List<Field>> declared_field_cache = new ConcurrentHashMap<>(32);

    private ClassUtils() {
    }

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

    public static Object getFieldValue(Object instance, String fieldName)
        throws Throwable {
        final Field field = getFieldByName(instance.getClass(), fieldName);
        if (field == null) {
            return null;
        }
        final MethodHandle getter = MethodHandles
            .privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup())
            .findGetter(field.getDeclaringClass(), field.getName(), field.getType());
        if (getter == null) {
            return null;
        }
        return getter.invoke(instance);
    }

    public static List<Field> getDeclaredFields(Class<?> clazz) {
        return all_declared_field_cache.computeIfAbsent(clazz, key -> {
            List<Field> fields = new ArrayList<>();
            Class<?> tClazz = clazz;
            while (tClazz != null && tClazz != Object.class) {
                fields.addAll(Arrays.asList(tClazz.getDeclaredFields()));
                tClazz = tClazz.getSuperclass();
            }
            return fields;
        });
    }

    static Field getFieldByName(Class<?> clazz, String fieldName) throws Exception {
        String newFieldName = toCamelCase(fieldName);
        try {
            return clazz.getDeclaredField(newFieldName);
        } catch (NoSuchFieldException e) {
            // ignore
        }

        List<Field> fields = declared_field_cache.computeIfAbsent(clazz, key -> List.of(clazz.getDeclaredFields()));
        for (Field field : fields) {
            if (field.getName().equalsIgnoreCase(newFieldName)) {
                return field;
            }
        }

        return getDeclaredFields(clazz.getSuperclass())
            .stream()
            .filter(field -> field.getName().equalsIgnoreCase(newFieldName))
            .findFirst()
            .orElse(null);
    }

    public static String toCamelCase(String fieldName) {
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

    public static List<Field> getPersistentFields(Class<?> clazz) {
        return persistent_field_cache.computeIfAbsent(clazz, key -> {
            final List<Field> fields = getDeclaredFields(clazz);
            return fields.stream()
                         .filter(ClassUtils::isPersistentField)
                         .toList();
        });
    }

    /**
     * skip [transient, static] modifier field or association field object.
     */
    public static boolean isPersistentField(Field field) {
        if (Modifier.isTransient(field.getModifiers())
            || Modifier.isStatic(field.getModifiers())
            || !isSimpleType(field.getType())) {
            return false;
        }
        return true;
    }

    public static <T> Map<String, Object> persistentFieldValues(T instance) {
        Map<String, Object> fieldValues = new LinkedHashMap<>();
        final List<Field> fieldList = getPersistentFields(instance.getClass());
        fieldList.forEach(field -> {
            try {
                fieldValues.put(field.getName(), ClassUtils.getFieldValue(instance, field));
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });

        return fieldValues;
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
