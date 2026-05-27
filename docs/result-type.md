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

## Nested Object Mapping

Map result columns to nested object fields.

### Field Path Mapping

Map a column alias directly to an object field path:

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a")
    .select("a.id", "a.name")
    .column("b.id", "tenant.id")      // b.id -> account.tenant.id
    .column("b.name", "tenant.name")  // b.name -> account.tenant.name
    .leftJoin("tenant b", "a.id", "b.account_id");

Account account = jdbcClient.executeQuery(sqlSelect, ResultType.of(Account.class));
// account.getTenant().getId() and account.getTenant().getName() are set

```

### Alias Prefix Mapping

Map all columns with the same alias prefix to a nested object field:

```java

SqlSelect sqlSelect = SqlSelect
    .from("accounts", "a")
    .select("a.id", "a.name")
    .column("b.id", "b.id")
    .column("b.name", "b.name")
    .leftJoin("tenant b", "a.id", "b.account_id");

ResultType<Account> resultType = ResultType.of(Account.class)
    .typeAliasMapping("b", "tenant");  // All "b.xxx" -> account.tenant.xxx

Account account = jdbcClient.executeQuery(sqlSelect, resultType);

```

### Multi-level Nesting

Support arbitrary depth of nested objects:

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