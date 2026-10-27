package io.github.matheuslealpa.rsql.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class FruitResourceTest {

    @Test
    void listWhenNoFilterThenAll() {
        given().when().get("/fruits").then().statusCode(200).body("$", hasSize(5));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "name==Apple                         | Apple",
            "color==GREEN;weight=gt=100          | Green Apple,50%_Off Pear",
            "color==RED,weight=lt=100            | Apple,Lime",
            "color=in=(RED,YELLOW)               | Apple,Banana",
            "color=out=(GREEN)                   | Apple,Banana",
            "name==*Apple                        | Apple,Green Apple",
            "name==50%_*                         | 50%_Off Pear",
            "harvested=null=                     | Banana",
            "harvested=ge=2026-10-05             | Green Apple,50%_Off Pear",
            "organic==true;color!=YELLOW         | Apple,50%_Off Pear",
            "id==4                               | Lime" })
    void listWhenFilterThenMatchingFruits(String filter, String expected) {
        given().queryParam("filter", filter)
                .when().get("/fruits")
                .then().statusCode(200)
                .body("$", contains(expected.split(",")));
    }

    @Test
    void listWhenWildcardWouldMatchLikeSpecialCharactersThenTheyAreLiteral() {
        given().queryParam("filter", "name==*%*")
                .when().get("/fruits")
                .then().statusCode(200)
                .body("$", contains("50%_Off Pear"));
        given().queryParam("filter", "name==_pple*")
                .when().get("/fruits")
                .then().statusCode(200)
                .body("$", empty());
    }

    @Test
    void listWhenValueContainsHqlThenNoInjection() {
        given().queryParam("filter", "name==\"x' or '1'='1\"")
                .when().get("/fruits")
                .then().statusCode(200)
                .body("$", empty());
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "unknown==1      | Unknown property 'unknown'",
            "weight==heavy   | Invalid value 'heavy'",
            "color==BLUE     | expected one of: RED, GREEN, YELLOW",
            "name==          | Invalid RSQL filter" })
    void listWhenInvalidFilterThenBadRequest(String filter, String message) {
        given().queryParam("filter", filter)
                .when().get("/fruits")
                .then().statusCode(400)
                .body(containsString(message));
    }

}
