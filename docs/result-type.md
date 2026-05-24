# ResultType

ResultType is a utility class for mapping SQL query results to Java objects.


```java

class Account {
    Long id;
    String name;
    Tenant tenant;
}

class Tenant {
    Long id;
    String name;
}


```
## Object Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .select("id", "name", "email") 
    .where("id", Op.eq(1));

Account account = jdbcClient.queryForObject(sqlSelect, Account.class);

// OR

Account account = jdbcClient.queryForObject(sqlSelect, ResultType.of(Account.class));

```

## List Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .select("id", "name", "email") 
    .orderBy("id");

List<Account> accounts = jdbcClient.queryForList(sqlSelect, ResultType.forList(Account.class));

```

## Object Field Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a") 
    .select("a.id", "a.name") 
    .column("b.id", "tenant.id") // tenant.id mapping account.tenant.id 
    .column("b.name", "tenant.name") // tenant.name mapping account.tenant.name 
    .leftJoin("tenant b", "a.id", "b.account_id");

Account account = jdbcClient.executeQuery(sqlSelect, ResultType.of(Account.class));
// Result: account.getTenant().getId() and account.getTenant().getName() will be correctly set

```

## Configuring Alias to Object Field Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a") 
    .select("a.id", "a.name") 
    .column("b.id", "b.id") 
    .column("b.name", "b.name") 
    .leftJoin("tenant b", "a.id", "b.account_id");
ResultType<Account> resultType = ResultType.of(Account.class).typeAliasMapping("b", "tenant"); // Map alias "b" to "tenant" field
Account account = jdbcClient.executeQuery(sqlSelect, resultType);


```

## Multi-level Object Field Mapping

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a") 
    .select("a.id", "a.name") 
    .column("b.id", "tenant.id") 
    .column("b.name", "tenant.name") 
    .column("c.id", "tenant.paymentOrder.id") 
    .column("c.trade_no", "tenant.paymentOrder.tradeNo") 
    .leftJoin("tenant b", "a.id", "b.account_id") 
    .leftJoin("payment_order c", "b.id", "c.tenant_id");

Account account = jdbcClient.executeQuery(sqlSelect, ResultType.of(Account.class));

// Results: 
// account.getTenant().getId() 
// account.getTenant().getName() 
// account.getTenant().getPaymentOrder().getId() 
// account.getTenant().getPaymentOrder().getTradeNo()

```

## diable ignore Unknown fields

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts") 
    .select("id", "name", "code AS unknown_field");

ResultType<Account> resultType = ResultType.of(Account.class).disableIgnoreUnknownField();
Account account = jdbcClient.executeQuery(sqlSelect, resultType);

// throw NoSuchFieldException

```