package com.reader.sjsql.result;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class TypeConverter {

    public static Object convert(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }

        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        return switch (targetType.getName()) {
            case "java.lang.String" -> toString(value);
            case "java.lang.Integer", "int" -> toInteger(value);
            case "java.lang.Long", "long" -> toLong(value);
            case "java.lang.Double", "double" -> toDouble(value);
            case "java.lang.Float", "float" -> toFloat(value);
            case "java.lang.Boolean", "boolean" -> toBoolean(value);
            case "java.lang.Byte", "byte" -> toByte(value);
            case "java.lang.Short", "short" -> toShort(value);
            case "java.math.BigDecimal" -> toBigDecimal(value);
            case "java.math.BigInteger" -> toBigInteger(value);
            case "java.time.LocalDate" -> toLocalDate(value);
            case "java.time.LocalTime" -> toLocalTime(value);
            case "java.time.LocalDateTime" -> toLocalDateTime(value);
            case "java.sql.Date" -> toDate(value);
            case "java.sql.Time" -> toTime(value);
            case "java.sql.Timestamp" -> toTimestamp(value);
            case "[B" -> toByteArray(value); // byte[]
            case "java.sql.Blob" -> toBlob(value);
            case "java.sql.Clob" -> toClob(value);
            default -> value;
        };
    }

    private static String toString(Object value) {
        if (value instanceof byte[] bytes) {
            return new String(bytes);
        }
        return value.toString();
    }

    private static Integer toInteger(Object value) {
        return switch (value) {
            case Number number -> number.intValue();
            case Boolean bool -> Boolean.TRUE.equals(bool) ? 1 : 0;
            default -> {
                try {
                    yield Integer.parseInt(value.toString());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    private static Long toLong(Object value) {
        return switch (value) {
            case BigInteger bigInteger -> bigInteger.longValue();
            case Number number -> number.longValue();
            case Boolean bool -> Boolean.TRUE.equals(bool) ? 1L : 0L;
            default -> {
                try {
                    yield Long.parseLong(value.toString());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Float toFloat(Object value) {
        if (value instanceof Number number) {
            return number.floatValue();
        }
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean toBoolean(Object value) {
        return switch (value) {
            case Boolean bool -> bool;
            case Number number -> number.intValue() != 0;
            default -> {
                String str = value.toString().toLowerCase();
                yield "true".equals(str) || "1".equals(str) || "yes".equals(str) || "on".equals(str);
            }
        };
    }

    private static Byte toByte(Object value) {
        if (value instanceof Number number) {
            return number.byteValue();
        }
        try {
            return Byte.parseByte(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Short toShort(Object value) {
        return switch (value) {
            case Number number -> number.shortValue();
            case Boolean bool -> (short) (bool ? 1 : 0);
            default -> {
                try {
                    yield Short.parseShort(value.toString());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    private static BigDecimal toBigDecimal(Object value) {
        return switch (value) {
            case BigDecimal decimal -> decimal;
            case Number number -> new BigDecimal(number.toString());
            default -> {
                try {
                    yield new BigDecimal(value.toString());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    private static BigInteger toBigInteger(Object value) {
        return switch (value) {
            case BigInteger integer -> integer;
            case BigDecimal decimal -> decimal.toBigInteger();
            case Number number -> BigInteger.valueOf(number.longValue());
            default -> {
                try {
                    yield new BigInteger(value.toString());
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    private static LocalDate toLocalDate(Object value) {
        return switch (value) {
            case LocalDate date -> date;
            case Date date -> date.toLocalDate();
            case Timestamp timestamp -> timestamp.toLocalDateTime().toLocalDate();
            default -> {
                try {
                    yield LocalDate.parse(value.toString());
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }

    private static LocalTime toLocalTime(Object value) {
        return switch (value) {
            case LocalTime time -> time;
            case Time time -> time.toLocalTime();
            case Timestamp timestamp -> timestamp.toLocalDateTime().toLocalTime();
            default -> {
                try {
                    yield LocalTime.parse(value.toString());
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        return switch (value) {
            case LocalDateTime dateTime -> dateTime;
            case Timestamp timestamp -> timestamp.toLocalDateTime();
            case Date date -> date.toLocalDate().atStartOfDay();
            default -> {
                try {
                    yield LocalDateTime.parse(value.toString());
                } catch (Exception e) {
                    yield null;
                }
            }
        };
    }

    private static Date toDate(Object value) {
        return switch (value) {
            case Date date -> date;
            case LocalDate localDate -> Date.valueOf(localDate);
            case LocalDateTime localDateTime -> Date.valueOf(localDateTime.toLocalDate());
            case Timestamp timestamp -> new Date(timestamp.getTime());
            default -> null;
        };
    }

    private static Time toTime(Object value) {
        return switch (value) {
            case Time time -> time;
            case LocalTime localTime -> Time.valueOf(localTime);
            case Timestamp timestamp -> new Time(timestamp.getTime());
            default -> null;
        };
    }

    private static Timestamp toTimestamp(Object value) {
        return switch (value) {
            case Timestamp timestamp -> timestamp;
            case LocalDateTime localDateTime -> Timestamp.valueOf(localDateTime);
            case LocalDate localDate -> Timestamp.valueOf(localDate.atStartOfDay());
            case Date date -> new Timestamp(date.getTime());
            default -> null;
        };
    }

    private static byte[] toByteArray(Object value) {
        return switch (value) {
            case byte[] bytes -> bytes;
            case Blob blob -> {
                try {
                    yield blob.getBytes(1, (int) blob.length());
                } catch (Exception e) {
                    yield null;
                }
            }
            case String str -> str.getBytes();
            case Number number -> new byte[]{number.byteValue()};
            default -> value.toString().getBytes();
        };
    }

    private static Blob toBlob(Object value) {
        if (value instanceof Blob blob) {
            return blob;
        }
        return null;
    }

    private static Clob toClob(Object value) {
        if (value instanceof Clob clob) {
            return clob;
        }
        return null;
    }
}
