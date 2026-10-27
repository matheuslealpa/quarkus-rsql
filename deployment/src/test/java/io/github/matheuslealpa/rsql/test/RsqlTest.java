package io.github.matheuslealpa.rsql.test;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.github.matheuslealpa.rsql.Rsql;
import io.github.matheuslealpa.rsql.RsqlException;
import io.github.matheuslealpa.rsql.RsqlFilter;
import io.github.matheuslealpa.rsql.RsqlQuery;
import io.github.matheuslealpa.rsql.test.Fruit.Color;
import io.quarkus.test.QuarkusExtensionTest;

class RsqlTest {

    private static final String FROM = "select r from Fruit r where ";

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(BaseEntity.class, Fruit.class, Farm.class, Address.class,
                    Owner.class, Vegetable.class))
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:rsql")
            .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create");

    @Inject
    EntityManagerFactory entityManagerFactory;

    @Test
    void toQueryWhenFilterIsNullOrBlankThenMatchesAll() {
        assertThat(toQuery(null)).isEqualTo(new RsqlQuery("select r from Fruit r", Map.of()));
        assertThat(toQuery("  ")).isEqualTo(new RsqlQuery("select r from Fruit r", Map.of()));
    }

    @Test
    void toQueryWhenEntityHasCustomNameThenUsesIt() {
        assertThat(toQuery("name==Carrot", Vegetable.class).query())
                .isEqualTo("select r from Veggie r where r.name = :p0");
    }

    @Test
    void toQueryWhenEqualThenBindsConvertedValue() {
        RsqlQuery query = toQuery("weight==150");
        assertThat(query.query()).isEqualTo(FROM + "r.weight = :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", 150));
    }

    @Test
    void toQueryWhenNotEqualThenUsesNotEqualOperator() {
        RsqlQuery query = toQuery("color!=RED");
        assertThat(query.query()).isEqualTo(FROM + "r.color <> :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", Color.RED));
    }

    @Test
    void toQueryWhenComparisonOperatorsThenUsesHqlOperators() {
        assertThat(toQuery("weight=gt=1").query()).isEqualTo(FROM + "r.weight > :p0");
        assertThat(toQuery("weight>=1").query()).isEqualTo(FROM + "r.weight >= :p0");
        assertThat(toQuery("weight=lt=1").query()).isEqualTo(FROM + "r.weight < :p0");
        assertThat(toQuery("weight<=1").query()).isEqualTo(FROM + "r.weight <= :p0");
    }

    @Test
    void toQueryWhenInThenBindsListOfValues() {
        RsqlQuery query = toQuery("color=in=(RED,GREEN)");
        assertThat(query.query()).isEqualTo(FROM + "r.color in :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", List.of(Color.RED, Color.GREEN)));
    }

    @Test
    void toQueryWhenOutThenUsesNotIn() {
        assertThat(toQuery("color=out=(RED)").query()).isEqualTo(FROM + "r.color not in :p0");
    }

    @Test
    void toQueryWhenNullOperatorsThenNoParameters() {
        RsqlQuery isNull = toQuery("harvested=null=");
        assertThat(isNull.query()).isEqualTo(FROM + "r.harvested is null");
        assertThat(isNull.parameters()).isEmpty();
        assertThat(toQuery("harvested=notnull=").query()).isEqualTo(FROM + "r.harvested is not null");
    }

    @Test
    void toQueryWhenStringWithWildcardThenUsesLikeAndEscapesSpecialCharacters() {
        RsqlQuery query = toQuery("name=='*50%_off*'");
        assertThat(query.query()).isEqualTo(FROM + "r.name like :p0 escape '\\'");
        assertThat(query.parameters()).containsExactly(entry("p0", "%50\\%\\_off%"));
        assertThat(toQuery("name!=App*").query()).isEqualTo(FROM + "r.name not like :p0 escape '\\'");
    }

    @Test
    void toQueryWhenAndOrThenGroupsWithParentheses() {
        RsqlQuery query = toQuery("name==Apple;weight=gt=100,color==GREEN");
        assertThat(query.query()).isEqualTo(FROM + "((r.name = :p0 and r.weight > :p1) or r.color = :p2)");
        assertThat(query.parameters()).containsOnly(entry("p0", "Apple"), entry("p1", 100), entry("p2", Color.GREEN));
    }

    @Test
    void toQueryWhenInheritedAttributeThenResolved() {
        assertThat(toQuery("id==1").parameters()).containsExactly(entry("p0", 1L));
    }

    @Test
    void toQueryWhenTypesThenConverted() {
        assertThat(toQuery("harvested==2026-10-27").parameters())
                .containsExactly(entry("p0", LocalDate.of(2026, 10, 27)));
        assertThat(toQuery("price==1.50").parameters()).containsExactly(entry("p0", new BigDecimal("1.50")));
        assertThat(toQuery("organic==TRUE").parameters()).containsExactly(entry("p0", true));
    }

    @Test
    void toQueryWhenUnknownPropertyThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("unknown==1"))
                .withMessage("Unknown property 'unknown' for Fruit");
    }

    @Test
    void toQueryWhenSelectorIsNotAPersistentAttributeThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("name.length==1"));
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("SECRET==1"))
                .withMessage("Unknown property 'SECRET' for Fruit");
    }

    @Test
    void toQueryWhenTransientAttributeThenUnknownProperty() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("label==x"))
                .withMessage("Unknown property 'label' for Fruit");
    }

    @Test
    void toQueryWhenIgnoredAttributeThenUnknownProperty() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("supplierCode==x"))
                .withMessage("Unknown property 'supplierCode' for Fruit");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("farm.owner.passwordHash=gt=a"))
                .withMessage("Unknown property 'farm.owner.passwordHash' for Fruit");
    }

    @Test
    void toQueryWhenSelectorsThenOnlyThoseAreAccepted() {
        Set<String> selectors = Set.of("name", "farm.name");
        assertThat(query(Rsql.toQuery("name==Apple;farm.name==Sunny", Fruit.class, selectors)).query())
                .isEqualTo("select r from Fruit r left join r.farm j0 where (r.name = :p0 and j0.name = :p1)");
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> Rsql.toQuery("weight==1", Fruit.class, selectors))
                .withMessage("Unknown property 'weight' for Fruit");
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> Rsql.toQuery("farm.country==BR", Fruit.class, selectors))
                .withMessage("Unknown property 'farm.country' for Fruit");
    }

    @Test
    void toQueryWhenInvalidValueThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("weight==heavy"))
                .withMessageStartingWith("Invalid value 'heavy' for property 'weight' of type ");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("color==BLUE"))
                .withMessage("Invalid value 'BLUE' for property 'color', expected one of: RED, GREEN, YELLOW");
    }

    @Test
    void toQueryWhenMalformedFilterThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("name=="))
                .withMessage("Invalid RSQL filter: name==");
    }

    @Test
    void toQueryWhenValueContainsHqlThenBoundAsParameter() {
        RsqlQuery query = toQuery("name==\"x' or '1'='1\"");
        assertThat(query.query()).isEqualTo(FROM + "r.name = :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", "x' or '1'='1"));
    }

    @Test
    void toQueryWhenAssociationPathThenLeftJoins() {
        RsqlQuery query = toQuery("farm.name==Sunny");
        assertThat(query.query()).isEqualTo("select r from Fruit r left join r.farm j0 where j0.name = :p0");
        assertThat(query.parameters()).containsExactly(entry("p0", "Sunny"));
    }

    @Test
    void toQueryWhenSameAssociationUsedTwiceThenJoinedOnce() {
        assertThat(toQuery("farm.name==Sunny,farm.country==BR").query())
                .isEqualTo("select r from Fruit r left join r.farm j0 where (j0.name = :p0 or j0.country = :p1)");
    }

    @Test
    void toQueryWhenNestedAssociationsThenJoinsEachOne() {
        assertThat(toQuery("farm.owner.name==Ana").query())
                .isEqualTo("select r from Fruit r left join r.farm j0 left join j0.owner j1 where j1.name = :p0");
    }

    @Test
    void toQueryWhenEmbeddablePathThenNoJoin() {
        assertThat(toQuery("farm.address.city==Lisboa").query())
                .isEqualTo("select r from Fruit r left join r.farm j0 where j0.address.city = :p0");
    }

    @Test
    void toQueryWhenAssociationIsNullThenNoJoin() {
        assertThat(toQuery("farm=null=").query()).isEqualTo(FROM + "r.farm is null");
    }

    @Test
    void toQueryWhenUnknownNestedPropertyThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("farm.size==1"))
                .withMessage("Unknown property 'farm.size' for Fruit");
    }

    @Test
    void toQueryWhenCollectionInPathThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("tags.name==x"))
                .withMessage("Collection property 'tags' isn't supported in 'tags.name'");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("tags==x"))
                .withMessage("Collection property 'tags' isn't supported in 'tags'");
    }

    @Test
    void toQueryWhenClassIsNotAnEntityThenException() {
        assertThatIllegalArgumentException().isThrownBy(() -> Rsql.toQuery("city==x", Address.class))
                .withMessageContaining("isn't an entity of any persistence unit");
    }

    @Test
    void rsqlFilterWhenToQueryThenSameAsRsql() {
        RsqlFilter filter = RsqlFilter.fromString("name==Apple;farm.name==Sunny");
        assertThat(query(filter.toQuery(Fruit.class))).isEqualTo(Rsql.toQuery(filter.toString(), Fruit.class));
        assertThat(filter.toQuery(Fruit.class, Set.of("name", "farm.name")))
                .isEqualTo(Rsql.toQuery(filter.toString(), Fruit.class));
        assertThat(RsqlFilter.fromString(null).toQuery(Fruit.class))
                .isEqualTo(new RsqlQuery("select r from Fruit r", Map.of()));
    }

    @Test
    void rsqlFilterWhenInvalidThenExceptionOnlyOnToQuery() {
        RsqlFilter filter = RsqlFilter.fromString("name==");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> filter.toQuery(Fruit.class))
                .withMessage("Invalid RSQL filter: name==");
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> RsqlFilter.fromString("weight==1").toQuery(Fruit.class, Set.of("name")))
                .withMessage("Unknown property 'weight' for Fruit");
    }

    private RsqlQuery toQuery(String filter) {
        return toQuery(filter, Fruit.class);
    }

    private RsqlQuery toQuery(String filter, Class<?> entityClass) {
        return query(Rsql.toQuery(filter, entityClass));
    }

    /**
     * Checks that Hibernate ORM accepts the generated query, then returns it.
     */
    private RsqlQuery query(RsqlQuery query) {
        try (EntityManager entityManager = this.entityManagerFactory.createEntityManager()) {
            entityManager.createQuery(query.query());
        }
        return query;
    }

}
