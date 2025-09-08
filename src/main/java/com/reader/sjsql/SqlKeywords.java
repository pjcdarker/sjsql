package com.reader.sjsql;

import static com.reader.sjsql.SqlEscape.escape;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public enum SqlKeywords {

    SELECT("SELECT "),
    FROM("\n FROM "),
    JOIN("\n JOIN "),
    LEFT_JOIN("\n LEFT JOIN "),
    RIGHT_JOIN("\n RIGHT JOIN "),
    UNION("\n UNION \n"),
    UNION_ALL("\n UNION ALL \n"),
    ON(" ON "),
    WHERE("\n WHERE "),
    AND("\n AND "),
    OR("\n OR "),
    IN(" IN "),
    NOT_IN(" NOT IN "),
    LIKE(" LIKE "),
    GROUP_BY("\n GROUP BY "),
    DESC(" DESC "),
    LIMIT("\n LIMIT "),
    HAVING("\n HAVING "),
    ORDER_BY("\n ORDER BY "),
    BETWEEN("BETWEEN"),
    AS(" AS "),
    INSERT_INTO("INSERT INTO "),
    VALUES(" VALUES "),
    UPDATE("UPDATE "),
    SET(" SET "),

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

        private static final Map<String, Function<Op, String>> formatFunc = Map.of(
            "LIKE", op -> "LIKE ?",
            "_LIKE", op -> "LIKE ?",
            "LIKE_", op -> "LIKE ?",
            "_LIKE_", op -> "LIKE ?",
            "BETWEEN", op -> "BETWEEN ? AND ?",
            "IN", op -> {
                String logicalType = op.reverse ? "NOT IN" : "IN";
                return parametrizeList(op, logicalType);
            },
            "NOT IN", op -> {
                String logicalType = op.reverse ? "IN" : "NOT IN";
                return parametrizeList(op, logicalType);
            }
        );

        private static final Map<String, Function<Op, Object>> escapeParamFunc = Map.of(
            "_LIKE", op -> "%" + escape(op.param),
            "LIKE_", op -> escape(op.param) + "%",
            "_LIKE_", op -> "%" + escape(op.param) + "%"
        );

        private final String sign;
        private final Object param;
        private boolean reverse;

        private Op(String sign, Object param) {
            this.sign = sign;
            this.param = param;
        }

        private Op(String sign, Object param, boolean reverse) {
            this.sign = sign;
            this.param = param;
            this.reverse = reverse;
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

        public static Op like(Object param) {
            return new Op("LIKE", param);
        }

        public static Op _like(Object param) {
            return new Op("_LIKE", param);
        }

        public static Op like_(Object param) {
            return new Op("LIKE_", param);
        }

        public static Op _like_(Object param) {
            return new Op("_LIKE_", param);
        }

        public static <E> Op in(List<E> params) {
            return new Op("IN", params);
        }

        public static <E> Op in(List<E> params, boolean reverse) {
            return new Op("IN", params, reverse);
        }

        public static <E> Op not_in(List<E> params) {
            return new Op("NOT IN", params);
        }

        public static <E> Op between(E start, E end) {
            return new Op(BETWEEN.name(), List.of(start, end));
        }

        public static Op create(String op, Object param) {
            return new Op(op, param);
        }

        public String getSign() {
            return sign;
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


        public Object escapeParam() {
            Function<Op, Object> function = escapeParamFunc.get(this.sign);
            if (function != null) {
                return function.apply(this);
            }

            if (param instanceof Collection<?> list) {
                return list.stream().map(SqlEscape::escape).toList();
            }

            return escape(this.param);
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
