package io.github.matheuslealpa.rsql.test;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.matheuslealpa.rsql.Rsql;
import io.github.matheuslealpa.rsql.RsqlException;
import io.quarkus.test.QuarkusUnitTest;

/**
 * The limits on the filters, lowered so that they are easy to reach.
 */
class RsqlLimitsTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClasses(BaseEntity.class, Fruit.class, Farm.class, Address.class,
                    Owner.class, Vegetable.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:rsql-limits")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")
            .overrideConfigKey("quarkus.rsql.max-filter-length", "40")
            .overrideConfigKey("quarkus.rsql.max-comparisons", "3")
            .overrideConfigKey("quarkus.rsql.max-in-values", "3");

    @Test
    void toQueryWhenFilterIsTooLongThenException() {
        String filter = "name==" + "a".repeat(34);
        assertThatNoException().isThrownBy(() -> Rsql.toQuery(filter, Fruit.class));
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> Rsql.toQuery(filter + "a", Fruit.class))
                .withMessage("RSQL filter is too long: 41 characters, the maximum is 40");
    }

    @Test
    void toQueryWhenSortIsTooLongThenException() {
        String sort = "weight,".repeat(5);
        assertThatNoException().isThrownBy(() -> Rsql.toQuery(null, sort + "-name", Fruit.class));
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> Rsql.toQuery(null, sort + "name,id", Fruit.class))
                .withMessage("RSQL sort is too long: 42 characters, the maximum is 40");
    }

    @Test
    void toQueryWhenTooManyComparisonsThenException() {
        String filter = "name==a;weight==1,tags==x";
        assertThatNoException().isThrownBy(() -> Rsql.toQuery(filter, Fruit.class));
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> Rsql.toQuery(filter + ";color==RED", Fruit.class))
                .withMessage("RSQL filter has too many comparisons, the maximum is 3");
    }

    @Test
    void toQueryWhenTooManyValuesThenException() {
        assertThatNoException().isThrownBy(() -> Rsql.toQuery("color=in=(RED,GREEN,YELLOW)", Fruit.class));
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> Rsql.toQuery("name=out=(a,b,c,d)", Fruit.class))
                .withMessage("Too many values for 'name', the maximum is 3");
    }

}
