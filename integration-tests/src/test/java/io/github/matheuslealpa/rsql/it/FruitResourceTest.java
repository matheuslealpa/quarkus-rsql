package io.github.matheuslealpa.rsql.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

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
            "farm.country!=BR,weight=lt=100      | Lime,50%_Off Pear",
            "tags==fresh                         | Apple,Green Apple",
            "tags==pro*                          | Apple,Banana,50%_Off Pear",
            "tags!=promo                         | Green Apple,Lime",
            "tags=in=(fresh,imported)            | Apple,Green Apple,50%_Off Pear",
            "tags=out=(fresh,promo)              | Lime",
            "tags=null=                          | Lime",
            "tags=notnull=;color==GREEN          | Green Apple,50%_Off Pear",
            "tags==fresh;tags==promo             | Apple" })
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
            "farm.name==\"Sunny Farm\",name==Lime  | 3",
            "tags==promo                         | 3" })
    void countWhenFilterThenMatchingCount(String filter, long expected) {
        given().queryParam("filter", filter == null ? "" : filter)
                .when().get("/fruits/count")
                .then().statusCode(200)
                .body(org.hamcrest.Matchers.is(String.valueOf(expected)));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "-weight                | 50%_Off Pear,Apple,Green Apple,Banana,Lime",
            "color,-name            | Lime,Green Apple,50%_Off Pear,Apple,Banana",
            "farm.country,name      | Banana,Lime,Apple,Green Apple,50%_Off Pear" })
    void listWhenSortThenOrdered(String sort, String expected) {
        given().queryParam("sort", sort)
                .when().get("/fruits")
                .then().statusCode(200)
                .body("$", contains(expected.split(",")));
    }

    @Test
    void listWhenFilterAndSortThroughSameAssociationThenOrdered() {
        given().queryParam("filter", "farm=notnull=")
                .queryParam("sort", "-farm.name,name")
                .when().get("/fruits")
                .then().statusCode(200)
                .body("$", contains("Apple", "Green Apple", "50%_Off Pear"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "unknown        | Unknown property 'unknown'",
            "supplierCode   | Unknown property 'supplierCode'",
            "farm           | can't be sorted",
            "'name,,id'     | Invalid RSQL sort" })
    void listWhenInvalidSortThenBadRequest(String sort, String message) {
        given().queryParam("sort", sort)
                .when().get("/fruits")
                .then().statusCode(400)
                .body(containsString(message));
    }

    @Test
    void countWhenNoFilterThenAll() {
        given().when().get("/fruits/count").then().statusCode(200).body(org.hamcrest.Matchers.is("5"));
    }

    @Test
    void countWhenInvalidFilterThenBadRequest() {
        // Mapped by the extension, the application has no exception mapper
        given().queryParam("filter", "name==")
                .when().get("/fruits/count")
                .then().statusCode(400)
                .contentType(ContentType.TEXT)
                .body(containsString("Invalid RSQL filter"));
    }

    @Test
    void listWhenTooManyValuesThenBadRequest() {
        String values = String.join(",", Collections.nCopies(257, "x"));
        given().queryParam("filter", "name=in=(" + values + ")")
                .when().get("/fruits")
                .then().statusCode(400)
                .body(containsString("Too many values for 'name', the maximum is 256"));
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
