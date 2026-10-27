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
Selectors must be fields of the entity, and values are always bound as parameters, so filters can't inject HQL.

See the [documentation](docs/index.adoc) for details.

> **Status:** experimental. Nested paths (`owner.name`) and native mode aren't supported yet.

## Building

```shell
mvn install
```
