package com.reader.sjsql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.reader.sjsql.SqlKeywords.Op;
import com.reader.sjsql.model.Account;
import com.reader.sjsql.model.Tenant;
import com.reader.sjsql.result.ResultType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class ResultTypeTest extends DatabaseTest {

    @Test
    void should_be_correct_result_type() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name", "a.create_time", "a.enabled")
            .where("id", Op.eq(1));

        final Account account = jdbcClient.executeQuery(sqlSelect, Account.class);

        assertEquals(1L, account.getId());
        assertEquals("Alice", account.getName());
        assertNotNull(account.getCreateTime());
        assertTrue(account.getEnabled());
    }

    @Test
    void should_be_correct_result_type_using_column_alias() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name")
            .addColumn("a.create_time", "updateTime")
            .where("id", Op.eq(1));

        ResultType<Account> resultType = ResultType.of(Account.class);

        final Account account = jdbcClient.executeQuery(sqlSelect, resultType);

        System.err.println(account);
        assertEquals(1L, account.getId());
        assertEquals("Alice", account.getName());
        assertNotNull(account.getUpdateTime());
    }

    @Test
    void should_be_correct_result_type_when_found_unknown_fields() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name")
            .addColumn("a.create_time", "createAt")
            .where("id", Op.eq(1));

        ResultType<Account> resultType = ResultType.of(Account.class)
                                                   .ignoreUnknownField(true);

        Account account = jdbcClient.executeQuery(sqlSelect, resultType);

        System.err.println(account);
        assertEquals(1L, account.getId());
        assertEquals("Alice", account.getName());
        assertNull(account.getCreateTime());
    }

    @Test
    void should_throw_exception_when_found_unknown_fields() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.create_time")
            .addColumn("a.name", "`a.name`")
            .where("id", Op.eq(1));

        ResultType<Account> resultType = ResultType.of(Account.class);

        assertThrows(NoSuchFieldException.class, () -> {
            jdbcClient.executeQuery(sqlSelect, resultType);
        });
    }

    @Test
    void should_be_correct_result_type_using_alias_object_column_name() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name", "a.create_time")
            .addColumn("b.id", "`tenant.id`")
            .addColumn("b.name", "`tenant.name`")
            .leftJoin(T_TENANT, "b", "a.id", "b.account_id")
            .where("a.id", Op.eq(1));

        ResultType<Account> resultType = ResultType.of(Account.class);

        final Account account = jdbcClient.executeQuery(sqlSelect, resultType);

        System.err.println("account: " + account);

        assertEquals(1L, account.getId());
        assertEquals("Alice", account.getName());

        Tenant tenant = account.getTenant();
        System.err.println("tenant: " + tenant);
        assertNotNull(tenant);
        assertEquals(1, tenant.getId());
        assertEquals("T1@test", tenant.getName());
    }

    @Test
    void should_be_correct_result_type_when_config_alias_object_field() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name", "a.create_time")
            .addColumn("b.id", "`b.id`")
            .addColumn("b.name", "`b.name`")
            .leftJoin(T_TENANT, "b", "a.id", "b.account_id")
            .where("a.id", Op.eq(1));

        ResultType<Account> resultType = ResultType.of(Account.class)
                                                   .aliasObjectField("b", "tenant");

        final Account account = jdbcClient.executeQuery(sqlSelect, resultType);

        System.err.println("account: " + account);

        assertEquals(1L, account.getId());
        assertEquals("Alice", account.getName());

        Tenant tenant = account.getTenant();
        System.err.println("tenant: " + tenant);
        assertNotNull(tenant);
        assertEquals(1, tenant.getId());
        assertEquals("T1@test", tenant.getName());
    }

    @Test
    void should_be_list_result_type() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name", "a.create_time")
            .orderBy("id");

        ResultType<List<Account>> resultType = ResultType.forList(Account.class);

        final List<Account> accounts = jdbcClient.executeQuery(sqlSelect, resultType);

        accounts.forEach(System.err::println);
        assertEquals(4, accounts.size());
        assertEquals(1L, accounts.getFirst().getId());
        assertEquals("Alice", accounts.getFirst().getName());
    }

    @Test
    void should_be_list_map_result_type() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name", "a.create_time")
            .orderBy("id");

        ResultType<List<Map<String, Object>>> resultType = ResultType.forMapList();

        final List<Map<String, Object>> accounts = jdbcClient.executeQuery(sqlSelect, resultType);

        accounts.forEach(System.err::println);
        assertEquals(4, accounts.size());
        assertEquals(1L, (Long) accounts.getFirst().get("id"));
        assertEquals("Alice", accounts.getFirst().get("name"));
    }

    @Test
    void should_be_correct_result_type_with_multiple_nested_fields() throws Throwable {
        // a.tenant.paymentOrder.tradeNo = Account.tenant.paymentOrder.tradeNo
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name", "a.email")
            .addColumn("b.id", "`tenant.id`")
            .addColumn("b.name", "`tenant.name`")
            .addColumn("c.id", "`tenant.paymentOrder.id`")
            .addColumn("c.trade_no", "`tenant.paymentOrder.tradeNo`")
            .leftJoin(T_TENANT, "b", "a.id", "b.account_id")
            .leftJoin(T_PAYMENT_ORDER, "c", "b.id", "c.tenant_id")
            .where("a.id", SqlKeywords.Op.eq(1));

        ResultType<Account> resultType = ResultType.of(Account.class);

        final Account account = jdbcClient.executeQuery(sqlSelect, resultType);

        System.err.println("account: " + account);

        assertNotNull(account);
        assertEquals(1L, account.getId());
        assertEquals("Alice", account.getName());
        //
        assertNotNull(account.getTenant());
        assertEquals(1L, account.getTenant().getId());
        assertEquals("T1@test", account.getTenant().getName());
        //
        assertNotNull(account.getTenant().getPaymentOrder());
        assertEquals(1L, account.getTenant().getPaymentOrder().getId());
        assertEquals("TRADE001", account.getTenant().getPaymentOrder().getTradeNo());
    }

    // object nested map

    @Test
    void should_be_correct_result_type_with_all_related_entities() throws Throwable {
        SqlSelect sqlSelect = SqlSelect
            .from(T_ACCOUNT, "a")
            .select("a.id", "a.name", "a.email")
            .addColumn("b.id", "`tenant.id`")
            .addColumn("b.name", "`tenant.name`")
            .addColumn("c.id", "`paymentOrder.id`")
            .addColumn("c.trade_no", "`paymentOrder.tradeNo`")
            .addColumn("d.id", "`tenant.paymentOrder.id`")
            .addColumn("d.trade_no", "`tenant.paymentOrder.tradeNo`")
            .leftJoin(T_TENANT, "b", "a.id", "b.account_id")
            .leftJoin(T_PAYMENT_ORDER, "c", "a.id", "c.account_id AND c.tenant_id IS NULL")
            .leftJoin(T_PAYMENT_ORDER, "d", "b.id", "d.tenant_id")
            .where("a.id", SqlKeywords.Op.eq(1))
            .orderBy("c.id", false);

        ResultType<Account> resultType = ResultType.of(Account.class);

        final Account account = jdbcClient.executeQuery(sqlSelect, resultType);

        System.err.println("account: " + account);

        assertNotNull(account);
        assertEquals(1L, account.getId());
        assertEquals("Alice", account.getName());

        //
        assertNotNull(account.getPaymentOrder());
        assertEquals(2L, account.getPaymentOrder().getId());
        assertEquals("TRADE002", account.getPaymentOrder().getTradeNo());

        //
        assertNotNull(account.getTenant());
        assertEquals(1L, account.getTenant().getId());
        assertEquals("T1@test", account.getTenant().getName());

        //
        assertNotNull(account.getTenant().getPaymentOrder());
        assertEquals(1L, account.getTenant().getPaymentOrder().getId());
        assertEquals("TRADE001", account.getTenant().getPaymentOrder().getTradeNo());
    }


}
