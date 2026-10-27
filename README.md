# Quarkus RSQL

Filter Hibernate ORM with Panache entities using [RSQL](https://github.com/jirutka/rsql-parser) query strings, such as `name==Apple;weight=gt=100`.

```java
@GET
public List<Fruit> list(@QueryParam("filter") String filter) {
    RsqlQuery query = Rsql.toQuery(filter, Fruit.class);
    return Fruit.find(query.query(), query.parameters()).list();
}
```

Supported operators: `==`, `!=`, `=gt=`, `=ge=`, `=lt=`, `=le=`, `=in=`, `=out=`, `=null=` and `=notnull=`, combined with `;` (and) and `,` (or).
Selectors can be fields of the entity or paths through its associations and embeddables, such as `farm.name`, which are joined with left joins.
Selectors must be persistent attributes of the entity, and values are always bound as parameters, so filters can't inject HQL.
Hide sensitive attributes with `@RsqlIgnore`, or accept only some selectors with `Rsql.toQuery(filter, Fruit.class, Set.of("name", "farm.name"))`.

## Requirements

* Java 17 or later (tested on 17, 21 and 25)
* Quarkus 3.x (tested with 3.40)
* Hibernate ORM, with or without Panache

## Installation

```xml
<dependency>
    <groupId>io.github.matheuslealpa</groupId>
    <artifactId>quarkus-rsql</artifactId>
    <version>1.0.0-RC1</version>
</dependency>
```

See the [documentation](docs/index.adoc) for details.

> **Status:** experimental. Collections in paths (`tags.name`) and native mode aren't supported yet.

## Building

```shell
mvn install
```
