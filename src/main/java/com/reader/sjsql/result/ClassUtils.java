package com.reader.sjsql.result;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

final class ClassUtils {

    static void setFieldValue(Object instance, Field field, Object value)
        throws NoSuchFieldException, IllegalAccessException {
        MethodHandles.privateLookupIn(field.getDeclaringClass(), MethodHandles.lookup())
                     .findVarHandle(field.getDeclaringClass(), field.getName(), field.getType())
                     .set(instance, value);
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
}
