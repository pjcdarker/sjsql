#

## query Object

```java


Account account = jdbcClient.queryForObject(sql, params, Account.class);

```

## query List

```java


List<Account> accounts = jdbcClient.queryForList(sql, params, Account.class);

```

## insert

```java

GeneratedKey generatedKey = jdbcClient.insert(sql, params);

// OR
GeneratedKey generatedKey = jdbcClient.insert(sql, params, List.of("id"));

// keyHolder.getKey().longValue()
// keyHolder.getKey(Long.class)

```

## update

```java

int affectedRows = jdbcClient.update(sql, params);

```


## batchUpdate batchSize

```java

jdbcClient.batchUpdate(sql, params, batchSize);

```