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
            "id==4                               | Lime",
            "farm.name==\"Sunny Farm\"             | Apple,Green Apple",
            "farm.country==PT                    | 50%_Off Pear",
            "farm.name==*Farm;color==GREEN       | Green Apple,50%_Off Pear",
            "farm=null=                          | Banana,Lime",
            "farm.name==\"Sunny Farm\",name==Lime  | Apple,Green Apple,Lime",
            "farm.country!=BR,weight=lt=100      | Lime,50%_Off Pear" })
    void listWhenFilterThenMatchingFruits(String filter, String expected) {
        given().queryParam("filter", filter)
                .when().get("/fruits")
                .then().statusCode(200)
                .body("$", contains(expected.split(",")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "                                    | 5",
            "color==GREEN                        | 3",
            "farm.name==\"Sunny Farm\",name==Lime  | 3" })
    void countWhenFilterThenMatchingCount(String filter, long expected) {
        given().queryParam("filter", filter == null ? "" : filter)
                .when().get("/fruits/count")
                .then().statusCode(200)
                .body(org.hamcrest.Matchers.is(String.valueOf(expected)));
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
            "name==          | Invalid RSQL filter",
            "farm.owner==x   | Unknown property 'farm.owner'",
            "name.value==x   | Unknown property 'name.value'",
            "label==x        | Unknown property 'label'",
            "supplierCode==x | Unknown property 'supplierCode'" })
    void listWhenInvalidFilterThenBadRequest(String filter, String message) {
        given().queryParam("filter", filter)
                .when().get("/fruits")
                .then().statusCode(400)
                .body(containsString(message));
    }

    @Test
    void listWhenSelectorsThenOnlyThoseAreAccepted() {
        given().queryParam("filter", "color==GREEN;name==*Apple")
                .when().get("/fruits/by-name-or-color")
                .then().statusCode(200)
                .body("$", contains("Green Apple"));
        given().queryParam("filter", "weight=gt=100")
                .when().get("/fruits/by-name-or-color")
                .then().statusCode(400)
                .body(containsString("Unknown property 'weight'"));
    }

}
