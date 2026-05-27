#

## query Object

```java


Account account = jdbcClient.queryForObject(sql, params, Account.class);

// OR

Account account = jdbcClient.queryForObject(sqlSelect, Account.class);

```

## query List

```java


List<Account> accounts = jdbcClient.queryForList(sql, params, Account.class);

// OR

List<Account> accounts = jdbcClient.queryForList(sqlSelect, Account.class);

```

## insert

```java

GeneratedKey generatedKey = jdbcClient.insert(sql, params);

// OR

GeneratedKey generatedKey = jdbcClient.insert(sqlInsert);

// OR

GeneratedKey generatedKey = jdbcClient.insert(sql, params, List.of("id"));

// OR

GeneratedKey generatedKey = jdbcClient.insert(sqlInsert, List.of("id"));

// keyHolder.getKey().longValue()
// keyHolder.getKey(Long.class)

```

## update

```java

int affectedRows = jdbcClient.update(sql, params);

// OR

int affectedRows = jdbcClient.update(sqlCommand);

```


## batchUpdate batchSize

```java

jdbcClient.batchUpdate(sql, params, batchSize);

// OR

jdbcClient.batchUpdate(sqlCommand);

// OR

jdbcClient.batchUpdate(sqlCommand, batchSize);

```

## transaction

```java

jdbcClient.transaction(() -> {
    jdbcClient.update(sql, params);
})

```

## nest transaction 

If a transaction already exists, the method joins it. If no transaction exists, a new one is created.

```java

jdbcClient.transaction(() -> {
    jdbcClient.update(sql, params);
    
    jdbcClient.transaction(() -> {
        jdbcClient.update(sql, params);
    })
})

```
