package io.github.happyduke96.squell.condition;

import io.github.happyduke96.squell.CommentTable;
import io.github.happyduke96.squell.Post;
import io.github.happyduke96.squell.PostTable;
import org.testng.annotations.Test;

import java.util.List;
import java.util.UUID;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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
    public void inSubQueryEmbedsTheSubqueryTextAndValues() {
        SubQuery<Post.Status> subQuery = new SubQuery<>("SELECT status FROM posts WHERE title = ?", List.of("x"));
        Condition built = posts.status().in(subQuery);

        assertEquals(built.sql(), "status IN (SELECT status FROM posts WHERE title = ?)");
        assertEquals(built.values(), List.of("x"));
    }

    @Test
    public void notInSubQueryEmbedsTheSubqueryTextAndValues() {
        SubQuery<Post.Status> subQuery = new SubQuery<>("SELECT status FROM posts WHERE title = ?", List.of("x"));
        Condition built = posts.status().notIn(subQuery);

        assertEquals(built.sql(), "status NOT IN (SELECT status FROM posts WHERE title = ?)");
        assertEquals(built.values(), List.of("x"));
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
    public void neIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.title().neIf(false, "unused");

        assertEquals(built.sql(), "");
    }

    @Test
    public void neIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.title().neIf(true, "Zero-reflection SQL");

        assertEquals(built.sql(), "title <> ?");
        assertEquals(built.values(), List.of("Zero-reflection SQL"));
    }

    @Test
    public void gtIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.title().gtIf(false, "unused");

        assertEquals(built.sql(), "");
    }

    @Test
    public void gtIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.title().gtIf(true, "m");

        assertEquals(built.sql(), "title > ?");
        assertEquals(built.values(), List.of("m"));
    }

    @Test
    public void ltIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.title().ltIf(false, "unused");

        assertEquals(built.sql(), "");
    }

    @Test
    public void ltIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.title().ltIf(true, "m");

        assertEquals(built.sql(), "title < ?");
        assertEquals(built.values(), List.of("m"));
    }

    @Test
    public void betweenIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.title().betweenIf(false, "a", "m");

        assertEquals(built.sql(), "");
    }

    @Test
    public void betweenIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.title().betweenIf(true, "a", "m");

        assertEquals(built.sql(), "title BETWEEN ? AND ?");
        assertEquals(built.values(), List.of("a", "m"));
    }

    @Test
    public void inIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.status().inIf(false, List.of(Post.Status.DRAFT));

        assertEquals(built.sql(), "");
    }

    @Test
    public void inIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.status().inIf(true, List.of(Post.Status.DRAFT, Post.Status.PUBLISHED));

        assertEquals(built.sql(), "status IN (?, ?)");
        assertEquals(built.values(), List.of("DRAFT", "PUBLISHED"));
    }

    @Test
    public void notInIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.status().notInIf(false, List.of(Post.Status.DRAFT));

        assertEquals(built.sql(), "");
    }

    @Test
    public void notInIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.status().notInIf(true, List.of(Post.Status.DRAFT));

        assertEquals(built.sql(), "status NOT IN (?)");
        assertEquals(built.values(), List.of("DRAFT"));
    }

    @Test
    public void likeIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.title().likeIf(false, "unused");

        assertEquals(built.sql(), "");
    }

    @Test
    public void likeIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.title().likeIf(true, "Zero%");

        assertEquals(built.sql(), "title LIKE ?");
        assertEquals(built.values(), List.of("Zero%"));
    }

    @Test
    public void notLikeIfIsNoConditionWhenTestIsFalse() {
        Condition built = posts.title().notLikeIf(false, "unused");

        assertEquals(built.sql(), "");
    }

    @Test
    public void notLikeIfBuildsTheConditionWhenTestIsTrue() {
        Condition built = posts.title().notLikeIf(true, "Zero%");

        assertEquals(built.sql(), "title NOT LIKE ?");
        assertEquals(built.values(), List.of("Zero%"));
    }

    @Test
    public void constraintsExposeGeneratedNonNullAndUniqueFlags() {
        ColumnConstraints idConstraints = posts.id().constraints();
        assertTrue(idConstraints.nonNull());
        assertTrue(idConstraints.unique());
        assertFalse(idConstraints.generated());
        assertTrue(posts.id().nonNull());
        assertTrue(posts.id().unique());
        assertFalse(posts.id().generated());

        ColumnConstraints titleConstraints = posts.title().constraints();
        assertFalse(titleConstraints.nonNull());
        assertFalse(titleConstraints.unique());
        assertFalse(titleConstraints.generated());
    }

    @Test
    public void generatedFieldReportsGeneratedTrue() {
        assertTrue(new CommentTable().id().generated());
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
    public void conditionBuilderCombinesEntriesWithOrSkippingNoConditionOnes() {
        Condition built = new ConditionBuilder()
                .add(posts.status().eq(Post.Status.DRAFT))
                .add(posts.status().eqIf(false, Post.Status.PUBLISHED))
                .add(posts.status().eqIf(true, Post.Status.PUBLISHED))
                .buildOr();

        assertEquals(built.sql(), "(status = ? OR status = ?)");
        assertEquals(built.values(), List.of("DRAFT", "PUBLISHED"));
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
            case Eq<?> _ -> "equality";
            case Ne<?> _ -> "inequality";
            case Gt<?> _ -> "greater-than";
            case Lt<?> _ -> "less-than";
            case Between<?> _ -> "range";
            case In<?> _ -> "membership";
            case NotIn<?> _ -> "non-membership";
            case InSubQuery<?> _ -> "subquery membership";
            case NotInSubQuery<?> _ -> "subquery non-membership";
            case Like _ -> "pattern match";
            case NotLike _ -> "pattern non-match";
            case EqField<?> _ -> "field equality";
            case IsNull _ -> "nullness";
            case IsNotNull _ -> "non-nullness";
            case AggregateEq<?> _ -> "aggregate equality";
            case AggregateNe<?> _ -> "aggregate inequality";
            case AggregateGt<?> _ -> "aggregate greater-than";
            case AggregateGe<?> _ -> "aggregate greater-or-equal";
            case AggregateLt<?> _ -> "aggregate less-than";
            case AggregateLe<?> _ -> "aggregate less-or-equal";
            case BinaryCondition _ -> "combination";
            case Not _ -> "negation";
            case NoCondition _ -> "absent";
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
