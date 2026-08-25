package io.github.happyduke96.squell.condition;

import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;

public class ConditionTest {

    private final PostTable posts = new PostTable();

    @Test
    public void eqBuildsAParameterizedEqualityCondition() {
        Condition built = posts.title().eq("Zero-reflection SQL");

        assertEquals(built.sql(), "title = ?");
        assertEquals(built.values(), List.of("Zero-reflection SQL"));
    }

    @Test
    public void andCombinesTwoConditionsWithParensAndMergedValues() {
        Condition built = posts.title().eq("Zero-reflection SQL").and(posts.status().eq(Post.Status.PUBLISHED));

        assertEquals(built.sql(), "(title = ? AND status = ?)");
        assertEquals(built.values(), List.of("Zero-reflection SQL", "PUBLISHED"));
    }

    @Test
    public void orCombinesTwoConditionsWithParensAndMergedValues() {
        Condition built = posts.status().eq(Post.Status.DRAFT).or(posts.status().eq(Post.Status.PUBLISHED));

        assertEquals(built.sql(), "(status = ? OR status = ?)");
    }

    @Test
    public void betweenBindsLowAndHigh() {
        Condition built = posts.title().between("a", "m");

        assertEquals(built.sql(), "title BETWEEN ? AND ?");
        assertEquals(built.values(), List.of("a", "m"));
    }

    @Test
    public void inBindsEachValue() {
        Condition built = posts.status().in(List.of(Post.Status.DRAFT, Post.Status.PUBLISHED));

        assertEquals(built.sql(), "status IN (?, ?)");
        assertEquals(built.values(), List.of("DRAFT", "PUBLISHED"));
    }

    @Test
    public void notInBindsEachValue() {
        Condition built = posts.status().notIn(List.of(Post.Status.DRAFT));

        assertEquals(built.sql(), "status NOT IN (?)");
        assertEquals(built.values(), List.of("DRAFT"));
    }

    @Test
    public void inWithNoValuesIsAlwaysFalseInsteadOfInvalidSql() {
        Condition built = posts.status().in(List.of());

        assertEquals(built.sql(), "1 = 0");
        assertEquals(built.values(), List.of());
    }

    @Test
    public void notInWithNoValuesIsAlwaysTrueInsteadOfInvalidSql() {
        Condition built = posts.status().notIn(List.of());

        assertEquals(built.sql(), "1 = 1");
        assertEquals(built.values(), List.of());
    }

    @Test
    public void likeBuildsAParameterizedPattern() {
        Condition built = posts.title().like("Zero%");

        assertEquals(built.sql(), "title LIKE ?");
        assertEquals(built.values(), List.of("Zero%"));
    }

    @Test
    public void notLikeBuildsAParameterizedPattern() {
        Condition built = posts.title().notLike("Zero%");

        assertEquals(built.sql(), "title NOT LIKE ?");
        assertEquals(built.values(), List.of("Zero%"));
    }

    @Test
    public void qualifiedByPrefixesTheFieldNameWithTheAlias() {
        Field<UUID> aliased = posts.id().qualifiedBy("p");

        assertEquals(aliased.name(), "p.id");
    }

    @Test
    public void eqFieldComparesTwoColumnsWithoutAnyBoundValue() {
        Condition built = posts.id().qualifiedBy("p").eqField(posts.id().qualifiedBy("c"));

        assertEquals(built.sql(), "p.id = c.id");
        assertEquals(built.values(), List.of());
    }

    @Test
    public void isNullBuildsAnUnparameterizedCondition() {
        Condition built = posts.title().isNull();

        assertEquals(built.sql(), "title IS NULL");
        assertEquals(built.values(), List.of());
    }

    @Test
    public void isNotNullBuildsAnUnparameterizedCondition() {
        Condition built = posts.title().isNotNull();

        assertEquals(built.sql(), "title IS NOT NULL");
        assertEquals(built.values(), List.of());
    }

    @Test
    public void escapeLikeEscapesPercentUnderscoreAndBackslash() {
        assertEquals(Field.escapeLike("50%_off\\sale"), "50\\%\\_off\\\\sale");
    }

    @Test
    public void aggregateCountGtBuildsAHavingCondition() {
        Condition built = Aggregate.count().gt(1L);

        assertEquals(built.sql(), "COUNT(*) > ?");
        assertEquals(built.values(), List.of(1L));
    }

    @Test
    public void aggregateMinOnAFieldNamesTheColumnInsideTheExpression() {
        Condition built = Aggregate.min(posts.title()).ge("m");

        assertEquals(built.sql(), "MIN(title) >= ?");
        assertEquals(built.values(), List.of("m"));
    }

    @Test
    public void eqIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.title().eqIf(false, "unused");

        assertEquals(built.sql(), "");
        assertEquals(built.values(), List.of());
    }

    @Test
    public void eqIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.title().eqIf(true, "Zero-reflection SQL");

        assertEquals(built.sql(), "title = ?");
    }

    @Test
    public void eqIfFalseChainsThroughAndWithoutAnyNullCheck() {
        Condition built = posts.status().eq(Post.Status.PUBLISHED).and(posts.title().eqIf(false, "unused"));

        assertEquals(built.sql(), "status = ?");
    }

    @Test
    public void conditionBuilderCombinesEntriesWithAndSkippingNoConditionOnes() {
        Condition built = new ConditionBuilder()
                .add(posts.title().eq("Zero-reflection SQL"))
                .add(posts.status().eqIf(false, Post.Status.PUBLISHED))
                .add(posts.status().eqIf(true, Post.Status.DRAFT))
                .buildAnd();

        assertEquals(built.sql(), "(title = ? AND status = ?)");
        assertEquals(built.values(), List.of("Zero-reflection SQL", "DRAFT"));
    }

    @Test
    public void conditionBuilderAddValueSkipsNullEmptyStringAndEmptyCollection() {
        Condition built = new ConditionBuilder()
                .add("", title -> posts.title().eq(title))
                .add((String) null, title -> posts.title().eq(title))
                .add(List.<String>of(), values -> posts.title().in(values))
                .buildAnd();

        assertEquals(built.sql(), "");
    }

    @Test
    public void conditionBuilderAddValuePresentBuildsTheCondition() {
        Condition built = new ConditionBuilder()
                .add("Zero-reflection SQL", posts.title()::eq)
                .buildAnd();

        assertEquals(built.sql(), "title = ?");
        assertEquals(built.values(), List.of("Zero-reflection SQL"));
    }

    @Test
    public void conditionBuilderWithNothingAddedBuildsNoCondition() {
        Condition built = new ConditionBuilder().buildAnd();

        assertEquals(built.sql(), "");
        assertEquals(built.values(), List.of());
    }

    @Test
    public void negateWrapsAConditionInNot() {
        Condition built = posts.status().eq(Post.Status.DRAFT).negate();

        assertEquals(built.sql(), "NOT (status = ?)");
        assertEquals(built.values(), List.of("DRAFT"));
    }

    @Test
    public void noConditionAndOtherReturnsTheOtherUnchanged() {
        Condition other = posts.title().eq("x");

        assertEquals(new NoCondition().and(other), other);
        assertEquals(new NoCondition().or(other), other);
    }

    @Test
    public void otherAndNoConditionReturnsTheOtherUnchanged() {
        Condition other = posts.title().eq("x");

        assertEquals(other.and(new NoCondition()), other);
        assertEquals(other.or(new NoCondition()), other);
    }

    @Test
    public void noConditionRendersAsEmptyTextWithNoValues() {
        Condition none = new NoCondition();

        assertEquals(none.sql(), "");
        assertEquals(none.values(), List.of());
    }

    /// Maps each `Condition` subtype to a label.
    private static String kindOf(Condition condition) {
        return switch (condition) {
            case Eq<?> e -> "equality";
            case Ne<?> n -> "inequality";
            case Gt<?> g -> "greater-than";
            case Lt<?> l -> "less-than";
            case Between<?> b -> "range";
            case In<?> i -> "membership";
            case NotIn<?> i -> "non-membership";
            case InSubQuery<?> i -> "subquery membership";
            case NotInSubQuery<?> i -> "subquery non-membership";
            case Like l -> "pattern match";
            case NotLike l -> "pattern non-match";
            case EqField<?> e -> "field equality";
            case IsNull n -> "nullness";
            case IsNotNull n -> "non-nullness";
            case AggregateEq<?> a -> "aggregate equality";
            case AggregateNe<?> a -> "aggregate inequality";
            case AggregateGt<?> a -> "aggregate greater-than";
            case AggregateGe<?> a -> "aggregate greater-or-equal";
            case AggregateLt<?> a -> "aggregate less-than";
            case AggregateLe<?> a -> "aggregate less-or-equal";
            case BinaryCondition b -> "combination";
            case Not n -> "negation";
            case NoCondition n -> "absent";
        };
    }

    @Test
    public void sealedConditionsAreExhaustivelyMatchedInASwitch() {
        assertEquals(kindOf(posts.id().eq(UUID.randomUUID())), "equality");
        assertEquals(kindOf(posts.status().eq(Post.Status.PUBLISHED).and(posts.title().eq("x"))), "combination");
        assertEquals(kindOf(posts.status().eq(Post.Status.PUBLISHED).negate()), "negation");
        assertEquals(kindOf(new NoCondition()), "absent");
    }
}
