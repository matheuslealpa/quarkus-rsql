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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.github.matheuslealpa.rsql.Rsql;
import io.github.matheuslealpa.rsql.RsqlException;
import io.github.matheuslealpa.rsql.RsqlFilter;
import io.github.matheuslealpa.rsql.RsqlQuery;
import io.github.matheuslealpa.rsql.RsqlSort;
import io.github.matheuslealpa.rsql.test.Fruit.Color;
import io.quarkus.test.QuarkusUnitTest;

class RsqlTest {

    private static final String FROM = "select r from Fruit r where ";

    // QuarkusUnitTest, deprecated since Quarkus 3.35, is the only test extension of Quarkus 3.27
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
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
    void toQueryWhenGetterOfFieldAccessAttributeIsIgnoredThenUnknownProperty() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("farm.owner.apiToken==x"))
                .withMessage("Unknown property 'farm.owner.apiToken' for Fruit");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("farm.owner.verified==true"))
                .withMessage("Unknown property 'farm.owner.verified' for Fruit");
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
    void toQueryWhenElementCollectionThenExistsOverElements() {
        RsqlQuery query = toQuery("tags==fresh");
        assertThat(query.query()).isEqualTo(FROM + "exists (select 1 from r.tags c0 where c0 = :p0)");
        assertThat(query.parameters()).containsExactly(entry("p0", "fresh"));
        assertThat(toQuery("tags==pro*").query())
                .isEqualTo(FROM + "exists (select 1 from r.tags c0 where c0 like :p0 escape '\\')");
        assertThat(toQuery("tags=in=(fresh,promo)").query())
                .isEqualTo(FROM + "exists (select 1 from r.tags c0 where c0 in :p0)");
        assertThat(toQuery("tags==fresh;tags==promo").query()).isEqualTo(FROM
                + "(exists (select 1 from r.tags c0 where c0 = :p0) and exists (select 1 from r.tags c1 where c1 = :p1))");
    }

    @Test
    void toQueryWhenNegativeOperatorOnCollectionThenNoElementMatches() {
        assertThat(toQuery("tags!=promo").query())
                .isEqualTo(FROM + "not exists (select 1 from r.tags c0 where c0 = :p0)");
        assertThat(toQuery("tags=out=(fresh,promo)").query())
                .isEqualTo(FROM + "not exists (select 1 from r.tags c0 where c0 in :p0)");
        assertThat(toQuery("fruits.name!=Apple", Farm.class).query())
                .isEqualTo("select r from Farm r where not exists (select 1 from r.fruits c0 where c0.name = :p0)");
    }

    @Test
    void toQueryWhenNullOnCollectionThenEmpty() {
        assertThat(toQuery("tags=null=").query()).isEqualTo(FROM + "r.tags is empty");
        assertThat(toQuery("tags=notnull=").query()).isEqualTo(FROM + "r.tags is not empty");
        assertThat(toQuery("fruits.tags=null=", Farm.class).query())
                .isEqualTo("select r from Farm r where exists (select 1 from r.fruits c0 where c0.tags is empty)");
    }

    @Test
    void toQueryWhenEntityCollectionThenExistsOverEntities() {
        RsqlQuery query = toQuery("fruits.name==Apple", Farm.class);
        assertThat(query.query())
                .isEqualTo("select r from Farm r where exists (select 1 from r.fruits c0 where c0.name = :p0)");
        assertThat(query.parameters()).containsExactly(entry("p0", "Apple"));
        assertThat(toQuery("fruits.weight=gt=100", Farm.class).parameters()).containsExactly(entry("p0", 100));
        assertThat(toQuery("fruits.tags==fresh", Farm.class).query()).isEqualTo("select r from Farm r where "
                + "exists (select 1 from r.fruits c0 where exists (select 1 from c0.tags c1 where c1 = :p0))");
        assertThat(toQuery("fruits.farm.owner.name==Ann", Farm.class).query()).isEqualTo("select r from Farm r where "
                + "exists (select 1 from r.fruits c0 left join c0.farm j0 left join j0.owner j1 where j1.name = :p0)");
    }

    @Test
    void toQueryWhenUnknownPropertyThroughCollectionThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("tags.name==x"))
                .withMessage("Unknown property 'tags.name' for Fruit");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("fruits.supplierCode==x", Farm.class))
                .withMessage("Unknown property 'fruits.supplierCode' for Farm");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("fruits.label==x", Farm.class))
                .withMessage("Unknown property 'fruits.label' for Farm");
    }

    @Test
    void toQueryWhenCollectionOfEntitiesComparedItselfThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("fruits==1", Farm.class))
                .withMessage("Property 'fruits' is a collection of entities or embeddables,"
                        + " compare a property of its elements instead");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("fruits=in=(1,2)", Farm.class))
                .withMessage("Property 'fruits' is a collection of entities or embeddables,"
                        + " compare a property of its elements instead");
        assertThat(toQuery("fruits=null=", Farm.class).query()).isEqualTo("select r from Farm r where r.fruits is empty");
    }

    @Test
    void toQueryWhenMapInPathThenException() {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("attributes==x", Farm.class))
                .withMessage("Map property 'attributes' isn't supported in 'attributes'");
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> toQuery("attributes.size==x", Farm.class))
                .withMessage("Map property 'attributes' isn't supported in 'attributes.size'");
    }

    @Test
    void toQueryWhenSortThroughCollectionThenException() {
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> Rsql.toQuery(null, "fruits.name", Farm.class))
                .withMessage("Property 'fruits.name' can't be sorted, it's a collection or goes through one");
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> Rsql.toQuery(null, "-fruits.tags", Farm.class))
                .withMessage("Property 'fruits.tags' can't be sorted, it's a collection or goes through one");
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

    @Test
    void toQueryWhenSortThenOrderBy() {
        assertThat(query(Rsql.toQuery(null, "-weight,name", Fruit.class)).query())
                .isEqualTo("select r from Fruit r order by r.weight desc, r.name asc");
        assertThat(query(Rsql.toQuery(null, " id , -harvested ", Fruit.class)).query())
                .isEqualTo("select r from Fruit r order by r.id asc, r.harvested desc");
    }

    @Test
    void toQueryWhenSortIsNullOrBlankThenNoOrderBy() {
        assertThat(Rsql.toQuery("name==Apple", null, Fruit.class).query()).isEqualTo(FROM + "r.name = :p0");
        assertThat(Rsql.toQuery("name==Apple", " ", Fruit.class).query()).isEqualTo(FROM + "r.name = :p0");
    }

    @Test
    void toQueryWhenSortThroughAssociationsThenSharesJoinsWithFilter() {
        RsqlQuery query = query(Rsql.toQuery("farm.name==Sunny", "farm.owner.name,-farm.address.city", Fruit.class));
        assertThat(query.query()).isEqualTo("select r from Fruit r left join r.farm j0 left join j0.owner j1"
                + " where j0.name = :p0 order by j1.name asc, j0.address.city desc");
        assertThat(query.parameters()).containsExactly(entry("p0", "Sunny"));
    }

    @Test
    void toQueryWhenSortWithSelectorsThenOnlyThoseAreAccepted() {
        Set<String> selectors = Set.of("name", "farm.name");
        assertThat(query(Rsql.toQuery(null, "farm.name,-name", Fruit.class, selectors)).query())
                .isEqualTo("select r from Fruit r left join r.farm j0 order by j0.name asc, r.name desc");
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> Rsql.toQuery(null, "-weight", Fruit.class, selectors))
                .withMessage("Unknown property 'weight' for Fruit");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "unknown                      | Unknown property 'unknown' for Fruit",
            "supplierCode                 | Unknown property 'supplierCode' for Fruit",
            "label                        | Unknown property 'label' for Fruit",
            "farm.owner.passwordHash      | Unknown property 'farm.owner.passwordHash' for Fruit",
            "'name desc; drop table x'    | Unknown property 'name desc; drop table x' for Fruit",
            "farm                         | Property 'farm' can't be sorted, only basic properties can",
            "farm.address                 | Property 'farm.address' can't be sorted, only basic properties can",
            "tags                         | Property 'tags' can't be sorted, it's a collection or goes through one",
            "'name,,id'                   | Invalid RSQL sort: name,,id",
            "'-'                          | Invalid RSQL sort: -" })
    void toQueryWhenInvalidSortThenException(String sort, String message) {
        assertThatExceptionOfType(RsqlException.class).isThrownBy(() -> Rsql.toQuery(null, sort, Fruit.class))
                .withMessage(message);
    }

    @Test
    void rsqlFilterWhenSortThenOrderBy() {
        RsqlFilter filter = RsqlFilter.fromString("color==RED");
        assertThat(query(filter.toQuery(Fruit.class, RsqlSort.fromString("-farm.name"))).query())
                .isEqualTo("select r from Fruit r left join r.farm j0 where r.color = :p0 order by j0.name desc");
        assertThat(filter.toQuery(Fruit.class, (RsqlSort) null)).isEqualTo(filter.toQuery(Fruit.class));
        assertThatExceptionOfType(RsqlException.class)
                .isThrownBy(() -> filter.toQuery(Fruit.class, RsqlSort.fromString("weight"), Set.of("color")))
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
