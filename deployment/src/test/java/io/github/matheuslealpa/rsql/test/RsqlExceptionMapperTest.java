package io.github.matheuslealpa.rsql.test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.http.ContentType;

/**
 * With Quarkus REST present, the extension reports invalid filters as {@code 400 Bad Request}.
 */
class RsqlExceptionMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BaseEntity.class, Fruit.class, Farm.class, Address.class,
                    Owner.class, Vegetable.class, FruitResource.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:rsql-mapper")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create");

    @Test
    void queryWhenFilterIsInvalidThenBadRequestWithMessage() {
        given().queryParam("filter", "unknown==1")
                .when().get("/fruits")
                .then().statusCode(400)
                .contentType(ContentType.TEXT)
                .body(is("Unknown property 'unknown' for Fruit"));
    }

    @Test
    void queryWhenFilterIsValidThenOk() {
        given().queryParam("filter", "name==Apple")
                .when().get("/fruits")
                .then().statusCode(200)
                .body(is("select r from Fruit r where r.name = :p0"));
    }

}
