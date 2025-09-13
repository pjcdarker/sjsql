package io.github.reader.sjsql;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public enum SqlKeywords {

    SELECT("SELECT "),
    FROM(" FROM "),
    AS(" AS "),
    JOIN(" JOIN "),
    LEFT_JOIN(" LEFT JOIN "),
    RIGHT_JOIN(" RIGHT JOIN "),
    FULL_JOIN(" FULL OUTER JOIN "),
    UNION(" UNION "),
    UNION_ALL(" UNION ALL "),
    ON(" ON "),
    WHERE(" WHERE "),

    GROUP_BY(" GROUP BY "),
    HAVING(" HAVING "),
    ORDER_BY(" ORDER BY "),
    DESC(" DESC "),
    LIMIT(" LIMIT "),

    INSERT_INTO("INSERT INTO "),
    VALUES(" VALUES "),
    UPDATE("UPDATE "),
    SET(" SET "),
    DELETE("DELETE "),

    AND(" AND "),
    OR(" OR "),
    IS_NULL(" IS NULL "),
    IS_NOT_NULL(" IS NOT NULL "),
    IN(" IN "),
    NOT_IN(" NOT IN "),
    LIKE(" LIKE "),
    BETWEEN(" BETWEEN "),
    EXISTS("EXISTS "),
    NOT_EXISTS("NOT EXISTS "),
    ;

    private final String format;

    SqlKeywords(String format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return this.format;
    }

    public static class Op {

        private static final String IS_NULL = "IS NULL";
        private static final String IS_NOT_NULL = "IS NOT NULL";
        private static final String IN = "IN";
        private static final String NOT_IN = "NOT IN";
        private static final String BETWEEN = "BETWEEN";
        private static final String LIKE = "LIKE";

        private static final Map<String, Function<Op, String>> formatFunc = Map.of(
            LIKE, op -> "LIKE ?",
            BETWEEN, op -> "BETWEEN ? AND ?",
            IS_NULL, op -> IS_NULL,
            IS_NOT_NULL, op -> IS_NOT_NULL,
            IN, op -> {
                if (op.sqlSelect != null) {
                    return "IN (" + op.sqlSelect.toSql() + ")";
                }
                String logicalType = op.reverse ? NOT_IN : IN;
                return parametrizeList(op, logicalType);
            },
            NOT_IN, op -> {
                if (op.sqlSelect != null) {
                    return "NOT IN (" + op.sqlSelect.toSql() + ")";
                }
                String logicalType = op.reverse ? IN : NOT_IN;
                return parametrizeList(op, logicalType);
            }
        );

        private final String sign;
        private final Object param;
        private boolean reverse;
        private SqlSelect sqlSelect;

        private Op(String sign, Object param) {
            this.sign = sign;
            this.param = param;
        }

        private Op(String sign, Object param, boolean reverse) {
            this.sign = sign;
            this.param = param;
            this.reverse = reverse;
        }

        private static Op create(String sign, SqlSelect sqlSelect) {
            Op op = new Op(sign, Arrays.asList(sqlSelect.params()));
            op.sqlSelect = sqlSelect;
            return op;
        }

        public static Op eq(Object param) {
            return new Op("=", param);
        }

        public static Op neq(Object param) {
            return new Op("<>", param);
        }

        public static Op gt(Object param) {
            return new Op(">", param);
        }

        public static Op gte(Object param) {
            return new Op(">=", param);
        }

        public static Op lt(Object param) {
            return new Op("<", param);
        }

        public static Op lte(Object param) {
            return new Op("<=", param);
        }

        public static Op is_null() {
            return new Op(IS_NULL, null);
        }

        public static Op is_not_null() {
            return new Op(IS_NOT_NULL, null);
        }

        public static Op like(Object param) {
            return new Op(LIKE, param);
        }

        public static Op _like(Object param) {
            return new Op(LIKE, "%" + (param));
        }

        public static Op like_(Object param) {
            return new Op(LIKE, (param) + "%");
        }

        public static Op _like_(Object param) {
            return new Op(LIKE, "%" + (param) + "%");
        }

        public static <E> Op in(List<E> params) {
            return new Op(IN, params);
        }

        public static <E> Op in(List<E> params, boolean reverse) {
            return new Op(IN, params, reverse);
        }

        public static Op in(SqlSelect sqlSelect) {
            return create(IN, sqlSelect);
        }

        public static <E> Op not_in(List<E> params) {
            return new Op(NOT_IN, params);
        }

        public static Op not_in(SqlSelect sqlSelect) {
            return create(NOT_IN, sqlSelect);
        }

        public static <E> Op between(E start, E end) {
            return new Op(BETWEEN, List.of(start, end));
        }
        public static Op create(String op, Object param) {
            return new Op(op, param);
        }

        public String getSign() {
            return sign;
        }

        public boolean isNoneParam() {
            return IS_NULL.equals(sign)
                || IS_NOT_NULL.equals(sign);
        }

        public String format(String column) {
            final Function<Op, String> function = formatFunc.get(this.sign);
            if (function != null) {
                return column + " " + function.apply(this);
            }

            return column + sign + "?";
        }

        public Object getParam() {
            return param;
        }

        private static String parametrizeList(Op op, String opt) {
            int size = ((List<?>) op.param).size();
            String[] params = new String[size];
            Arrays.fill(params, "?");
            final String parametrization = String.join(",", params);
            return opt + " (" + parametrization + ")";
        }
    }

}
