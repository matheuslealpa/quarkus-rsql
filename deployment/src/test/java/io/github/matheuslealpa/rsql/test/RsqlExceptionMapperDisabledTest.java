package io.github.matheuslealpa.rsql.test;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * With {@code quarkus.rsql.map-exceptions=false}, the extension registers no mapper and the
 * exception propagates, so that the application can map it itself.
 */
class RsqlExceptionMapperDisabledTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BaseEntity.class, Fruit.class, Farm.class, Address.class,
                    Owner.class, Vegetable.class, FruitResource.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:rsql-mapper-disabled")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")
            .overrideConfigKey("quarkus.rsql.map-exceptions", "false");

    @Test
    void queryWhenFilterIsInvalidThenInternalServerError() {
        given().queryParam("filter", "unknown==1")
                .when().get("/fruits")
                .then().statusCode(500);
    }

}
