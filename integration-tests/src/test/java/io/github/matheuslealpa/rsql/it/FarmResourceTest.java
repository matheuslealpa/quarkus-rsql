package io.github.matheuslealpa.rsql.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Filters through the {@code fruits} collection of the farms, so that the correlated subqueries
 * run against the database.
 */
@QuarkusTest
class FarmResourceTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "fruits.name==Apple                  | Sunny Farm",
            "fruits.name!=Apple                  | Hill Farm,Empty Farm",
            "fruits.weight=gt=170                | Hill Farm",
            "fruits.color==GREEN;fruits.color==RED | Sunny Farm",
            "fruits.tags==imported               | Hill Farm",
            "fruits.tags=out=(promo)             | Empty Farm",
            "fruits.farm.country==BR             | Sunny Farm",
            "fruits=null=                        | Empty Farm",
            "fruits=notnull=                     | Sunny Farm,Hill Farm",
            "name==*Farm;fruits.tags==fresh      | Sunny Farm" })
    void listWhenFilterThroughCollectionThenMatchingFarms(String filter, String expected) {
        given().queryParam("filter", filter)
                .when().get("/farms")
                .then().statusCode(200)
                .body("$", contains(expected.split(",")));
    }

    @Test
    void listWhenNoFarmHasBothTagsThenEmpty() {
        given().queryParam("filter", "fruits.tags==fresh;fruits.tags==imported")
                .when().get("/farms")
                .then().statusCode(200)
                .body("$", empty());
    }

    @Test
    void listWhenCollectionComparedItselfThenBadRequest() {
        given().queryParam("filter", "fruits==1")
                .when().get("/farms")
                .then().statusCode(400)
                .body(containsString("compare a property of its elements"));
    }

}
