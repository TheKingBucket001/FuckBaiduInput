package com.fuckbaiduinput;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;

public class FeatureConfigurationTest {
    private static final HookFeature REQUESTED = HookFeature.CLIPBOARD_CAPACITY;
    private static final HookFeature UNREQUESTED = HookFeature.ACCOUNT_ISOLATION;

    private static Map<String, Object> values(long generation, long revision) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(FeatureConfiguration.SCHEMA_KEY, 1);
        values.put(FeatureConfiguration.GENERATION_KEY, generation);
        values.put(FeatureConfiguration.REVISION_KEY, revision);
        values.put(UNREQUESTED.key(), true);
        return values;
    }

    private static Map<HookFeature, Boolean> change(HookFeature feature, boolean value) {
        return Collections.singletonMap(feature, value);
    }

    @Test
    public void legacySchemaOneKeepsFeatureKeysAndUsesGenerationZero() {
        Map<String, Object> legacy = values(0L, 7L);
        legacy.remove(FeatureConfiguration.GENERATION_KEY);
        legacy.put("offline_shop_cleanup", true);
        FeatureConfiguration parsed = FeatureConfiguration.from(legacy);
        assertTrue(parsed.isValid());
        assertEquals(0L, parsed.generation());
        assertEquals(7L, parsed.revision());
        assertTrue(parsed.isEnabled(HookFeature.ONLINE_SKIN_SHOP_CLEANUP));
        assertTrue(parsed.isEnabled(UNREQUESTED));
        assertFalse(parsed.isEnabled(REQUESTED));
    }

    @Test
    public void corruptSchemaRepairDisablesEveryUnrequestedFeature() {
        Map<String, Object> corrupt = values(3L, 10L);
        corrupt.put(FeatureConfiguration.SCHEMA_KEY, 2);
        corrupt.put(HookFeature.EMOTION_SHOP_CLEANUP.key(), true);
        FeatureConfiguration.WritePlan repair = new FeatureConfiguration.Transactions()
                .plan(corrupt, change(REQUESTED, true));
        assertTrue(repair.clearFirst);
        assertTrue(repair.requiresCommit);
        assertEquals(4L, repair.configuration.generation());
        assertEquals(1L, repair.configuration.revision());
        assertEquals(Collections.singleton(REQUESTED), repair.configuration.enabledFeatures());
    }

    @Test
    public void malformedAndNegativeRevisionsRepairInNewGeneration() {
        for (Object revision : new Object[] { -1L, 5, "5", null }) {
            Map<String, Object> corrupt = values(2L, 5L);
            corrupt.put(FeatureConfiguration.REVISION_KEY, revision);
            assertFalse(FeatureConfiguration.from(corrupt).isValid());
            FeatureConfiguration repaired = new FeatureConfiguration.Transactions()
                    .plan(corrupt, change(REQUESTED, true)).configuration;
            assertEquals(3L, repaired.generation());
            assertEquals(1L, repaired.revision());
            assertFalse(repaired.isEnabled(UNREQUESTED));
        }
    }

    @Test
    public void malformedFeatureInvalidatesTheSameStateInReaderAndWriter() {
        Map<String, Object> corrupt = values(0L, 8L);
        corrupt.put(HookFeature.CLOUD_INPUT.key(), "true");
        FeatureConfiguration parsed = FeatureConfiguration.from(corrupt);
        assertFalse(parsed.isValid());
        assertTrue(parsed.enabledFeatures().isEmpty());
        FeatureConfiguration repaired = new FeatureConfiguration.Transactions()
                .plan(corrupt, change(REQUESTED, true)).configuration;
        assertEquals(Collections.singleton(REQUESTED), repaired.enabledFeatures());
    }

    @Test
    public void illegalGenerationIsRejectedInsteadOfGuessed() {
        for (Object generation : new Object[] { -1L, 1, "1", null }) {
            Map<String, Object> corrupt = values(0L, 3L);
            corrupt.put(FeatureConfiguration.GENERATION_KEY, generation);
            assertFalse(FeatureConfiguration.from(corrupt).isValid());
            assertThrows(IllegalArgumentException.class, () -> new FeatureConfiguration.Transactions()
                    .plan(corrupt, change(REQUESTED, true)));
        }
    }

    @Test
    public void generationCannotOverflowDuringRepairOrRevisionRollover() {
        Map<String, Object> corrupt = values(Long.MAX_VALUE, 3L);
        corrupt.put(FeatureConfiguration.SCHEMA_KEY, 0);
        assertThrows(IllegalArgumentException.class, () -> new FeatureConfiguration.Transactions()
                .plan(corrupt, change(REQUESTED, true)));
        Map<String, Object> exhausted = values(Long.MAX_VALUE, Long.MAX_VALUE);
        assertThrows(IllegalArgumentException.class, () -> new FeatureConfiguration.Transactions()
                .plan(exhausted, change(REQUESTED, true)));
    }

    @Test
    public void healthyRevisionRolloverKeepsUnrequestedFeatures() {
        FeatureConfiguration configuration = new FeatureConfiguration.Transactions()
                .plan(values(4L, Long.MAX_VALUE), change(REQUESTED, true)).configuration;
        assertEquals(5L, configuration.generation());
        assertEquals(1L, configuration.revision());
        assertTrue(configuration.isEnabled(UNREQUESTED));
        assertTrue(configuration.isEnabled(REQUESTED));
    }

    @Test
    public void initialEmptyStorageUsesAllOffBaseline() {
        FeatureConfiguration.WritePlan initial = new FeatureConfiguration.Transactions()
                .plan(Collections.emptyMap(), change(REQUESTED, true));
        assertEquals(0L, initial.configuration.generation());
        assertEquals(1L, initial.configuration.revision());
        assertEquals(Collections.singleton(REQUESTED), initial.configuration.enabledFeatures());
    }

    @Test
    public void noOpRequiresAConfirmedCommitThenDoesNotAdvanceRevision() {
        FeatureConfiguration.Transactions transactions = new FeatureConfiguration.Transactions();
        FeatureConfiguration.WritePlan first = transactions.plan(values(0L, 9L),
                change(UNREQUESTED, true));
        assertTrue(first.requiresCommit);
        transactions.committed(first.configuration);
        FeatureConfiguration.WritePlan noOp = transactions.plan(first.configuration.values(),
                change(UNREQUESTED, true));
        assertFalse(noOp.requiresCommit);
        assertEquals(first.configuration, noOp.configuration);
        assertEquals(10L, noOp.configuration.revision());
    }

    @Test
    public void failedCommitDirtyCacheCannotTurnAnIdenticalRetryIntoNoOp() {
        FeatureConfiguration.Transactions transactions = new FeatureConfiguration.Transactions();
        FeatureConfiguration.WritePlan failed = transactions.plan(values(0L, 5L),
                change(REQUESTED, true));
        transactions.failed(failed);
        FeatureConfiguration.WritePlan retry = transactions.plan(failed.configuration.values(),
                change(REQUESTED, true));
        assertTrue(retry.requiresCommit);
        assertEquals(7L, retry.configuration.revision());
        assertTrue(retry.configuration.isEnabled(REQUESTED));
        transactions.committed(retry.configuration);
        assertFalse(transactions.plan(retry.configuration.values(),
                change(REQUESTED, true)).requiresCommit);
    }

    @Test
    public void failedChangeIsNotInheritedByAnUnrelatedLaterRequest() {
        FeatureConfiguration.Transactions transactions = new FeatureConfiguration.Transactions();
        FeatureConfiguration.WritePlan failed = transactions.plan(values(0L, 5L),
                change(REQUESTED, true));
        transactions.failed(failed);
        FeatureConfiguration.WritePlan later = transactions.plan(failed.configuration.values(),
                change(HookFeature.CLOUD_INPUT, true));
        assertTrue(later.requiresCommit);
        assertFalse(later.configuration.isEnabled(REQUESTED));
        assertTrue(later.configuration.isEnabled(UNREQUESTED));
        assertTrue(later.configuration.isEnabled(HookFeature.CLOUD_INPUT));
    }

    @Test
    public void newServiceConnectionCannotSkipCommitUsingOldConfirmation() {
        FeatureConfiguration.Transactions transactions = new FeatureConfiguration.Transactions();
        FeatureConfiguration first = transactions.plan(values(0L, 5L),
                change(REQUESTED, true)).configuration;
        transactions.committed(first);
        transactions.newConnection();
        assertTrue(transactions.plan(first.values(), change(REQUESTED, true)).requiresCommit);
    }

    @Test
    public void sameConnectionUncertaintyKeepsFailedBaselineOutOfUnrelatedRequest() {
        FeatureConfiguration.Transactions transactions = new FeatureConfiguration.Transactions();
        FeatureConfiguration.WritePlan failed = transactions.plan(values(0L, 5L),
                change(REQUESTED, true));
        transactions.failed(failed);
        transactions.uncertain();
        FeatureConfiguration.WritePlan later = transactions.plan(failed.configuration.values(),
                change(HookFeature.CLOUD_INPUT, true));
        assertTrue(later.requiresCommit);
        assertFalse(later.configuration.isEnabled(REQUESTED));
        assertTrue(later.configuration.isEnabled(UNREQUESTED));
        assertTrue(later.configuration.isEnabled(HookFeature.CLOUD_INPUT));
        assertEquals(7L, later.configuration.revision());
    }

    @Test
    public void sameConnectionUncertaintyRetainsConfirmedOrderingButForcesCommit() {
        FeatureConfiguration.Transactions transactions = new FeatureConfiguration.Transactions();
        FeatureConfiguration confirmed = transactions.plan(values(3L, 7L),
                change(REQUESTED, true)).configuration;
        transactions.committed(confirmed);
        transactions.uncertain();
        FeatureConfiguration.WritePlan same = transactions.plan(confirmed.values(),
                change(REQUESTED, true));
        assertTrue(same.requiresCommit);
        assertEquals(3L, same.configuration.generation());
        assertEquals(9L, same.configuration.revision());
        FeatureConfiguration restored = transactions.plan(values(0L, 1L),
                change(REQUESTED, true)).configuration;
        assertEquals(4L, restored.generation());
        assertEquals(1L, restored.revision());
        assertFalse(restored.isEnabled(UNREQUESTED));
    }

    @Test
    public void newConnectionDropsOldFailedBaselineAndUsesFreshPersistedValues() {
        FeatureConfiguration.Transactions transactions = new FeatureConfiguration.Transactions();
        FeatureConfiguration confirmed = transactions.plan(values(3L, 7L),
                change(REQUESTED, true)).configuration;
        transactions.committed(confirmed);
        FeatureConfiguration.WritePlan failed = transactions.plan(confirmed.values(),
                change(UNREQUESTED, false));
        transactions.failed(failed);
        transactions.newConnection();
        Map<String, Object> fresh = values(0L, 2L);
        fresh.put(REQUESTED.key(), false);
        fresh.put(UNREQUESTED.key(), false);
        FeatureConfiguration.WritePlan later = transactions.plan(fresh,
                change(HookFeature.CLOUD_INPUT, true));
        assertTrue(later.requiresCommit);
        assertEquals(0L, later.configuration.generation());
        assertEquals(3L, later.configuration.revision());
        assertFalse(later.configuration.isEnabled(REQUESTED));
        assertFalse(later.configuration.isEnabled(UNREQUESTED));
        assertTrue(later.configuration.isEnabled(HookFeature.CLOUD_INPUT));
    }

    @Test
    public void schemaDiagnosticContainsOnlyItsFixedCategory() {
        Map<String, Object> corrupt = values(0L, 3L);
        corrupt.put(FeatureConfiguration.SCHEMA_KEY, "private-schema-value");
        assertEquals("invalid_schema", FeatureConfiguration.invalidReason(corrupt));
        assertFalse(FeatureConfiguration.from(corrupt).isValid());
    }

    @Test
    public void revisionDiagnosticContainsOnlyItsFixedCategory() {
        Map<String, Object> corrupt = values(0L, 3L);
        corrupt.put(FeatureConfiguration.REVISION_KEY, "private-revision-value");
        assertEquals("invalid_revision", FeatureConfiguration.invalidReason(corrupt));
        assertFalse(FeatureConfiguration.from(corrupt).isValid());
    }

    @Test
    public void generationDiagnosticContainsOnlyItsFixedCategory() {
        Map<String, Object> corrupt = values(0L, 3L);
        corrupt.put(FeatureConfiguration.GENERATION_KEY, "private-generation-value");
        assertEquals("invalid_generation", FeatureConfiguration.invalidReason(corrupt));
        assertFalse(FeatureConfiguration.from(corrupt).isValid());
    }

    @Test
    public void featureDiagnosticContainsOnlyItsFixedCategory() {
        Map<String, Object> corrupt = values(0L, 3L);
        corrupt.put(REQUESTED.key(), "private-feature-value");
        assertEquals("invalid_feature_type", FeatureConfiguration.invalidReason(corrupt));
        assertFalse(FeatureConfiguration.from(corrupt).isValid());
    }

    @Test
    public void emptyAndValidLegacyConfigurationsHaveNoCorruptionDiagnostic() {
        assertNull(FeatureConfiguration.invalidReason(null));
        assertNull(FeatureConfiguration.invalidReason(Collections.emptyMap()));
        Map<String, Object> legacy = values(0L, 3L);
        legacy.remove(FeatureConfiguration.GENERATION_KEY);
        assertNull(FeatureConfiguration.invalidReason(legacy));
        assertTrue(FeatureConfiguration.from(legacy).isValid());
    }

    @Test
    public void invalidReadPreservesOrderingAndTurnsAllFeaturesOff() {
        FeatureConfiguration current = FeatureConfiguration.from(values(2L, 10L));
        FeatureConfiguration disabled = current.accept(FeatureConfiguration.disabled());
        assertFalse(disabled.isValid());
        assertTrue(disabled.enabledFeatures().isEmpty());
        assertEquals(2L, disabled.generation());
        assertEquals(10L, disabled.revision());
        assertSame(disabled, disabled.accept(FeatureConfiguration.from(values(2L, 9L))));
        assertTrue(disabled.accept(current).isValid());
    }

    @Test
    public void resetAcknowledgementWinsOverEveryOldGenerationCallback() {
        FeatureConfiguration old = FeatureConfiguration.from(values(0L, 10L));
        Map<String, Object> corrupt = new LinkedHashMap<>(old.values());
        corrupt.put(FeatureConfiguration.REVISION_KEY, -1L);
        FeatureConfiguration repaired = new FeatureConfiguration.Transactions()
                .plan(corrupt, change(REQUESTED, true)).configuration;
        FeatureConfiguration accepted = old.accept(repaired);
        assertEquals(1L, accepted.generation());
        assertEquals(1L, accepted.revision());
        assertSame(accepted, accepted.accept(FeatureConfiguration.from(values(0L, Long.MAX_VALUE))));
        assertSame(accepted, accepted.accept(old));
    }

    @Test
    public void fullAcknowledgementsAndCallbacksDoNotLoseOtherWritersFlags() {
        FeatureConfiguration.Transactions writer = new FeatureConfiguration.Transactions();
        FeatureConfiguration first = writer.plan(values(0L, 1L),
                change(REQUESTED, true)).configuration;
        writer.committed(first);
        FeatureConfiguration second = writer.plan(first.values(),
                change(HookFeature.CLOUD_INPUT, true)).configuration;
        FeatureConfiguration local = FeatureConfiguration.disabled().accept(second);
        assertSame(local, local.accept(first));
        assertTrue(local.isEnabled(REQUESTED));
        assertTrue(local.isEnabled(UNREQUESTED));
        assertTrue(local.isEnabled(HookFeature.CLOUD_INPUT));
        assertEquals(second, FeatureConfiguration.from(second.values()));
        assertEquals(HookFeature.values().length + 3, second.values().size());
    }

    @Test
    public void rolledBackStorageIsRepairedAboveTheLastConfirmedGeneration() {
        FeatureConfiguration.Transactions writer = new FeatureConfiguration.Transactions();
        FeatureConfiguration confirmed = writer.plan(values(3L, 7L),
                change(REQUESTED, true)).configuration;
        writer.committed(confirmed);
        FeatureConfiguration restored = writer.plan(values(0L, 1L),
                change(REQUESTED, true)).configuration;
        assertEquals(4L, restored.generation());
        assertEquals(1L, restored.revision());
        assertFalse(restored.isEnabled(UNREQUESTED));
    }
}
