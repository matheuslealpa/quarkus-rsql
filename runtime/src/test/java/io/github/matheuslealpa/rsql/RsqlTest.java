package io.github.matheuslealpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class RsqlTest {

    @Test
    void toQueryWhenFilterIsNullOrBlankThenMatchesAll() {
        assertThat(Rsql.toQuery(null, Fruit.class)).isEqualTo(RsqlQuery.MATCH_ALL);
        assertThat(Rsql.toQuery("  ", Fruit.class)).isEqualTo(RsqlQuery.MATCH_ALL);
    }

    @Test
    void toQueryWhenEqualThenBindsConvertedValue() {
        RsqlQuery query = Rsql.toQuery("weight==150", Fruit.class);
        assertThat(query.query()).isEqualTo("weight = :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", 150));
    }

    @Test
    void toQueryWhenNotEqualThenUsesNotEqualOperator() {
        RsqlQuery query = Rsql.toQuery("color!=RED", Fruit.class);
        assertThat(query.query()).isEqualTo("color <> :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", Color.RED));
    }

    @Test
    void toQueryWhenComparisonOperatorsThenUsesHqlOperators() {
        assertThat(Rsql.toQuery("weight=gt=1", Fruit.class).query()).isEqualTo("weight > :p0");
        assertThat(Rsql.toQuery("weight>=1", Fruit.class).query()).isEqualTo("weight >= :p0");
        assertThat(Rsql.toQuery("weight=lt=1", Fruit.class).query()).isEqualTo("weight < :p0");
        assertThat(Rsql.toQuery("weight<=1", Fruit.class).query()).isEqualTo("weight <= :p0");
    }

    @Test
    void toQueryWhenInThenBindsListOfValues() {
        RsqlQuery query = Rsql.toQuery("color=in=(RED,GREEN)", Fruit.class);
        assertThat(query.query()).isEqualTo("color in :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", List.of(Color.RED, Color.GREEN)));
    }

    @Test
    void toQueryWhenOutThenUsesNotIn() {
        assertThat(Rsql.toQuery("color=out=(RED)", Fruit.class).query()).isEqualTo("color not in :p0");
    }

    @Test
    void toQueryWhenNullOperatorsThenNoParameters() {
        RsqlQuery isNull = Rsql.toQuery("harvested=null=", Fruit.class);
        assertThat(isNull.query()).isEqualTo("harvested is null");
        assertThat(isNull.parameters()).isEmpty();
        assertThat(Rsql.toQuery("harvested=notnull=", Fruit.class).query()).isEqualTo("harvested is not null");
    }

    @Test
    void toQueryWhenStringWithWildcardThenUsesLikeAndEscapesSpecialCharacters() {
        RsqlQuery query = Rsql.toQuery("name=='*50%_off*'", Fruit.class);
        assertThat(query.query()).isEqualTo("name like :p0 escape '\\'");
        assertThat(query.parameters()).containsExactly(entry("p0", "%50\\%\\_off%"));
        assertThat(Rsql.toQuery("name!=App*", Fruit.class).query()).isEqualTo("name not like :p0 escape '\\'");
    }

    @Test
    void toQueryWhenAndOrThenGroupsWithParentheses() {
        RsqlQuery query = Rsql.toQuery("name==Apple;weight=gt=100,color==GREEN", Fruit.class);
        assertThat(query.query()).isEqualTo("((name = :p0 and weight > :p1) or color = :p2)");
        assertThat(query.parameters()).containsOnly(entry("p0", "Apple"), entry("p1", 100), entry("p2", Color.GREEN));
    }

    @Test
    void toQueryWhenInheritedFieldThenResolved() {
        assertThat(Rsql.toQuery("id==1", Fruit.class).parameters()).containsExactly(entry("p0", 1L));
    }

    @Test
    void toQueryWhenTypesThenConverted() {
        assertThat(Rsql.toQuery("harvested==2026-10-27", Fruit.class).parameters())
                .containsExactly(entry("p0", LocalDate.of(2026, 10, 27)));
        assertThat(Rsql.toQuery("price==1.50", Fruit.class).parameters())
                .containsExactly(entry("p0", new BigDecimal("1.50")));
        assertThat(Rsql.toQuery("organic==TRUE", Fruit.class).parameters()).containsExactly(entry("p0", true));
    }

    @Test
    void toQueryWhenUnknownPropertyThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> Rsql.toQuery("unknown==1", Fruit.class))
                .withMessage("Unknown property 'unknown' for Fruit");
    }

    @Test
    void toQueryWhenSelectorIsNotASimplePropertyThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> Rsql.toQuery("name.length==1", Fruit.class));
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> Rsql.toQuery("SECRET==1", Fruit.class));
    }

    @Test
    void toQueryWhenInvalidValueThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> Rsql.toQuery("weight==heavy", Fruit.class))
                .withMessage("Invalid value 'heavy' for property 'weight' of type int");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> Rsql.toQuery("color==BLUE", Fruit.class))
                .withMessage("Invalid value 'BLUE' for property 'color', expected one of: RED, GREEN, YELLOW");
    }

    @Test
    void toQueryWhenMalformedFilterThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> Rsql.toQuery("name==", Fruit.class))
                .withMessage("Invalid RSQL filter: name==");
    }

    @Test
    void toQueryWhenValueContainsHqlThenBoundAsParameter() {
        RsqlQuery query = Rsql.toQuery("name==\"x' or '1'='1\"", Fruit.class);
        assertThat(query.query()).isEqualTo("name = :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", "x' or '1'='1"));
    }

    enum Color {
        RED,
        GREEN,
        YELLOW
    }

    static class BaseEntity {
        public Long id;
    }

    static class Fruit extends BaseEntity {
        static final String SECRET = "secret";
        public String name;
        public int weight;
        public Color color;
        public LocalDate harvested;
        public BigDecimal price;
        public Boolean organic;
    }

}
