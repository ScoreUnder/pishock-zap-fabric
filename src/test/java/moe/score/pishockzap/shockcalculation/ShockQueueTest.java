package moe.score.pishockzap.shockcalculation;

import moe.score.pishockzap.config.PishockZapConfig;
import moe.score.pishockzap.config.ShockDistribution;
import moe.score.pishockzap.pishockapi.OpType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ShockQueueTest {
    static PishockZapConfig makeTestConfig() {
        var config = new PishockZapConfig();

        config.setDuration(1.0f);
        config.setMaxDuration(4.5f);

        // Vibration intensity
        // Expected results:
        // 0 dmg = (nop)
        // 1 dmg = 20
        // 2 dmg = 40
        // 3 dmg = 60
        // 4 dmg = 80
        // >4 dmg = shock (because vibration threshold is set to 4)
        config.setVibrationIntensityMin(20);
        config.setVibrationIntensityMax(80);

        // Shock intensity
        // Expected results:
        // <=4 dmg = vibration instead
        // 5 dmg = 20
        // 6 dmg = 30
        // 7 dmg = 40
        // 8 dmg = 50
        // 9 dmg = 60
        // >=10 dmg = 70 (because max damage is set to 10)
        config.setShockIntensityMin(20);
        config.setShockIntensityMax(70);

        config.setShockOnDeath(true);
        config.setShockDurationDeath(2.0f);
        config.setShockIntensityDeath(95);

        config.setVibrationThreshold(4);
        config.setMaxDamage(10);

        return config;
    }

    static Stream<Arguments> vibrationArguments() {
        return Stream.of(
            Arguments.of(1, 20),
            Arguments.of(2, 40),
            Arguments.of(3, 60),
            Arguments.of(4, 80)
        );
    }

    static Stream<Arguments> shockArguments() {
        return Stream.of(
            Arguments.of(5, 20),
            Arguments.of(6, 30),
            Arguments.of(7, 40),
            Arguments.of(8, 50),
            Arguments.of(9, 60),
            Arguments.of(10, 70),
            Arguments.of(11, 70),
            Arguments.of(471, 70)
        );
    }

    @ParameterizedTest
    @MethodSource("vibrationArguments")
    void vibrationReturnsExpectedValue(int damage, int expectedIntensity) throws InterruptedException {
        var queue = new ShockQueue(makeTestConfig());
        queue.queueShock(ShockDistribution.ALL, false, damage);

        assertFalse(queue.isEmpty());

        var calculated = queue.takeAndMergeShocks();

        assertEquals(ShockDistribution.ALL, calculated.distribution());
        assertEquals(OpType.VIBRATE, calculated.type());
        assertEquals(expectedIntensity, calculated.intensity());
        assertEquals(1.0f, calculated.duration(), 0.001f);

        assertTrue(queue.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("shockArguments")
    void shockReturnsExpectedValue(int damage, int expectedIntensity) throws InterruptedException {
        var queue = new ShockQueue(makeTestConfig());
        queue.queueShock(ShockDistribution.ALL, false, damage);

        assertFalse(queue.isEmpty());

        var calculated = queue.takeAndMergeShocks();

        assertEquals(ShockDistribution.ALL, calculated.distribution());
        assertEquals(OpType.SHOCK, calculated.type());
        assertEquals(expectedIntensity, calculated.intensity());
        assertEquals(1.0f, calculated.duration(), 0.001f);

        assertTrue(queue.isEmpty());
    }

    @Test
    void deathShockReturnsExpectedValue() throws InterruptedException {
        var queue = new ShockQueue(makeTestConfig());
        queue.queueShock(ShockDistribution.ALL, true, 1);

        assertFalse(queue.isEmpty());

        var calculated = queue.takeAndMergeShocks();

        assertEquals(ShockDistribution.ALL, calculated.distribution());
        assertEquals(OpType.SHOCK, calculated.type());
        assertEquals(95, calculated.intensity());
        assertEquals(2.0f, calculated.duration(), 0.001f);

        assertTrue(queue.isEmpty());
    }

    @Test
    void differentMaxDamageGivesDifferentShock() throws InterruptedException {
        var config = makeTestConfig();
        config.setMaxDamage(7);
        var queue = new ShockQueue(config);

        queue.queueShock(ShockDistribution.ALL, false, 6);

        assertFalse(queue.isEmpty());

        var calculated = queue.takeAndMergeShocks();

        assertEquals(ShockDistribution.ALL, calculated.distribution());
        assertEquals(OpType.SHOCK, calculated.type());
        assertEquals(45, calculated.intensity());
        assertEquals(1.0f, calculated.duration(), 0.001f);

        assertTrue(queue.isEmpty());
    }

    @Test
    void differentShockDistributionIsRespectedOnDamage() throws InterruptedException {
        var queue = new ShockQueue(makeTestConfig());

        queue.queueShock(ShockDistribution.RANDOM, false, 6);

        assertFalse(queue.isEmpty());

        var calculated = queue.takeAndMergeShocks();

        assertEquals(ShockDistribution.RANDOM, calculated.distribution());
        assertEquals(OpType.SHOCK, calculated.type());
        assertEquals(30, calculated.intensity());
        assertEquals(1.0f, calculated.duration(), 0.001f);

        assertTrue(queue.isEmpty());
    }

    @Test
    void differentShockDistributionIsRespectedOnDeath() throws InterruptedException {
        var queue = new ShockQueue(makeTestConfig());

        queue.queueShock(ShockDistribution.RANDOM, true, 1);

        assertFalse(queue.isEmpty());

        var calculated = queue.takeAndMergeShocks();

        assertEquals(ShockDistribution.RANDOM, calculated.distribution());
        assertEquals(OpType.SHOCK, calculated.type());
        assertEquals(95, calculated.intensity());
        assertEquals(2.0f, calculated.duration(), 0.001f);

        assertTrue(queue.isEmpty());
    }

    @Nested
    class QueueDifferentTest {
        static ShockQueue makeQueue() {
            var config = makeTestConfig();

            config.setQueueDifferent(true);

            return new ShockQueue(config);
        }

        @Test
        void twoDifferentVibrationsAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, 1);
            queue.queueShock(ShockDistribution.ALL, false, 2);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.VIBRATE, calculated.type());
            assertEquals(20, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            // Should still have the other vibration waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.VIBRATE, calculated.type());
            assertEquals(40, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Test
        void twoDifferentShocksAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, 5);
            queue.queueShock(ShockDistribution.ALL, false, 6);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(20, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            // Should still have the other shock waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(30, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Test
        void vibrationAndShockAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, 2);
            queue.queueShock(ShockDistribution.ALL, false, 5);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.VIBRATE, calculated.type());
            assertEquals(40, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            // Should still have the other shock waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(20, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Test
        void vibrationAndDeathShockAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, 2);
            queue.queueShock(ShockDistribution.ALL, true, 1);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.VIBRATE, calculated.type());
            assertEquals(40, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            // Should still have the other shock waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Test
        void shockAndDeathShockAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, 5);
            queue.queueShock(ShockDistribution.ALL, true, 1);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(20, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            // Should still have the other shock waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Test
        void twoDeathShocksAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, true, 1);
            queue.queueShock(ShockDistribution.ALL, true, 1);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            // Should still have the other shock waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Nested
        class WithoutAccumulateDuration {
            static ShockQueue makeQueue(boolean withAccumulateIntensity) {
                var config = makeTestConfig();

                config.setQueueDifferent(true);
                // This should be overridden to behave as if it's false anyway
                config.setAccumulateIntensity(withAccumulateIntensity);
                config.setAccumulateDuration(false);

                return new ShockQueue(config);
            }

            @ParameterizedTest
            @ValueSource(booleans = {true, false})
            void oneVibrationIsUnchanged(boolean withAccumulateIntensity) throws InterruptedException {
                var queue = makeQueue(withAccumulateIntensity);
                queue.queueShock(ShockDistribution.ALL, false, 2);

                assertFalse(queue.isEmpty());

                var calculated = queue.takeAndMergeShocks();

                assertEquals(ShockDistribution.ALL, calculated.distribution());
                assertEquals(OpType.VIBRATE, calculated.type());
                assertEquals(40, calculated.intensity());
                assertEquals(1.0f, calculated.duration(), 0.001f);

                assertTrue(queue.isEmpty());
            }

            @ParameterizedTest
            @ValueSource(booleans = {true, false})
            void twoSameVibrationsAreCombined(boolean withAccumulateIntensity) throws InterruptedException {
                var queue = makeQueue(withAccumulateIntensity);
                queue.queueShock(ShockDistribution.ALL, false, 2);
                queue.queueShock(ShockDistribution.ALL, false, 2);

                assertFalse(queue.isEmpty());

                var calculated = queue.takeAndMergeShocks();

                assertEquals(ShockDistribution.ALL, calculated.distribution());
                assertEquals(OpType.VIBRATE, calculated.type());
                assertEquals(40, calculated.intensity());
                assertEquals(1.0f, calculated.duration(), 0.001f);

                assertTrue(queue.isEmpty());
            }

            @ParameterizedTest
            @ValueSource(booleans = {true, false})
            void twoSameShocksAreCombined(boolean withAccumulateIntensity) throws InterruptedException {
                var queue = makeQueue(withAccumulateIntensity);
                queue.queueShock(ShockDistribution.ALL, false, 5);
                queue.queueShock(ShockDistribution.ALL, false, 5);

                assertFalse(queue.isEmpty());

                var calculated = queue.takeAndMergeShocks();

                assertEquals(ShockDistribution.ALL, calculated.distribution());
                assertEquals(OpType.SHOCK, calculated.type());
                assertEquals(20, calculated.intensity());
                assertEquals(1.0f, calculated.duration(), 0.001f);

                assertTrue(queue.isEmpty());
            }
        }

        @Nested
        class WithAccumulateDuration {
            static ShockQueue makeQueue(boolean withAccumulateIntensity) {
                var config = makeTestConfig();

                config.setQueueDifferent(true);
                // This should be overridden to behave as if it's false anyway
                config.setAccumulateIntensity(withAccumulateIntensity);
                config.setAccumulateDuration(true);

                return new ShockQueue(config);
            }

            @ParameterizedTest
            @ValueSource(booleans = {true, false})
            void oneVibrationIsUnchanged(boolean withAccumulateIntensity) throws InterruptedException {
                var queue = makeQueue(withAccumulateIntensity);
                queue.queueShock(ShockDistribution.ALL, false, 2);

                assertFalse(queue.isEmpty());

                var calculated = queue.takeAndMergeShocks();

                assertEquals(ShockDistribution.ALL, calculated.distribution());
                assertEquals(OpType.VIBRATE, calculated.type());
                assertEquals(40, calculated.intensity());
                assertEquals(1.0f, calculated.duration(), 0.001f);

                assertTrue(queue.isEmpty());
            }

            @ParameterizedTest
            @ValueSource(booleans = {true, false})
            void twoSameVibrationsAreCombined(boolean withAccumulateIntensity) throws InterruptedException {
                var queue = makeQueue(withAccumulateIntensity);
                queue.queueShock(ShockDistribution.ALL, false, 2);
                queue.queueShock(ShockDistribution.ALL, false, 2);

                assertFalse(queue.isEmpty());

                var calculated = queue.takeAndMergeShocks();

                assertEquals(ShockDistribution.ALL, calculated.distribution());
                assertEquals(OpType.VIBRATE, calculated.type());
                assertEquals(40, calculated.intensity());
                assertEquals(2.0f, calculated.duration(), 0.001f);

                assertTrue(queue.isEmpty());
            }

            @ParameterizedTest
            @ValueSource(booleans = {true, false})
            void twoSameShocksAreCombined(boolean withAccumulateIntensity) throws InterruptedException {
                var queue = makeQueue(withAccumulateIntensity);
                queue.queueShock(ShockDistribution.ALL, false, 5);
                queue.queueShock(ShockDistribution.ALL, false, 5);

                assertFalse(queue.isEmpty());

                var calculated = queue.takeAndMergeShocks();

                assertEquals(ShockDistribution.ALL, calculated.distribution());
                assertEquals(OpType.SHOCK, calculated.type());
                assertEquals(20, calculated.intensity());
                assertEquals(2.0f, calculated.duration(), 0.001f);

                assertTrue(queue.isEmpty());
            }
        }
    }

    @Nested
    class MergeDifferentTest {
        static ShockQueue makeQueue() {
            var config = makeTestConfig();

            config.setQueueDifferent(false);

            return new ShockQueue(config);
        }

        @Test
        void vibrationAndDeathShockAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, 2);
            queue.queueShock(ShockDistribution.ALL, true, 1);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.VIBRATE, calculated.type());
            assertEquals(40, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            // Should still have the other shock waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Test
        void shockAndDeathShockAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, 5);
            queue.queueShock(ShockDistribution.ALL, true, 1);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(20, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            // Should still have the other shock waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Test
        void twoDeathShocksAreNotCombined() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, true, 1);
            queue.queueShock(ShockDistribution.ALL, true, 1);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            // Should still have the other shock waiting
            assertFalse(queue.isEmpty());

            calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.SHOCK, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Nested
        class WithoutAccumulateDuration {
            @Nested
            class WithoutAccumulateIntensity {
                static ShockQueue makeQueue() {
                    var config = makeTestConfig();

                    config.setQueueDifferent(false);
                    config.setAccumulateIntensity(false);
                    config.setAccumulateDuration(false);

                    return new ShockQueue(config);
                }

                @Test
                void oneVibrationIsUnchanged() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoDifferentVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoSameVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoDifferentShocksAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 5);
                    queue.queueShock(ShockDistribution.ALL, false, 6);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(30, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoSameShocksAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 5);
                    queue.queueShock(ShockDistribution.ALL, false, 5);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(20, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void vibrationAndShockAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 5);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(20, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void threeVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 1);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }
            }

            @Nested
            class WithAccumulateIntensity {
                static ShockQueue makeQueue() {
                    var config = makeTestConfig();

                    config.setQueueDifferent(false);
                    config.setAccumulateIntensity(true);
                    config.setAccumulateDuration(false);

                    return new ShockQueue(config);
                }

                @Test
                void oneVibrationIsUnchanged() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoDifferentVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(60, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoSameVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(80, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoVibrationsCombineToShock() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 4);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(30, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoDifferentShocksAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 5);
                    queue.queueShock(ShockDistribution.ALL, false, 6);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(70, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoSameShocksAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 5);
                    queue.queueShock(ShockDistribution.ALL, false, 5);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(70, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void vibrationAndShockAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 5);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void threeVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 1);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(80, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void threeVibrationsCombineToShock() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 3);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(30, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }
            }
        }

        @Nested
        class WithAccumulateDuration {
            @Nested
            class WithoutAccumulateIntensity {
                static ShockQueue makeQueue() {
                    var config = makeTestConfig();

                    config.setQueueDifferent(false);
                    config.setAccumulateIntensity(false);
                    config.setAccumulateDuration(true);

                    return new ShockQueue(config);
                }

                @Test
                void oneVibrationIsUnchanged() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoDifferentVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.5f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoSameVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(2.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoDifferentShocksAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 5);
                    queue.queueShock(ShockDistribution.ALL, false, 6);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(30, calculated.intensity());
                    assertEquals(11f / 6f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoSameShocksAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 5);
                    queue.queueShock(ShockDistribution.ALL, false, 5);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(20, calculated.intensity());
                    assertEquals(2.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void vibrationAndShockAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 5);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(20, calculated.intensity());
                    assertEquals(1.4f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void threeVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 1);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(2.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @ParameterizedTest
                @ValueSource(ints = {5, 6, 7})
                void maxDurationIsNotExceeded(int numShocks) throws InterruptedException {
                    var queue = makeQueue();

                    for (int i = 0; i < numShocks; i++) {
                        queue.queueShock(ShockDistribution.ALL, false, 1);
                    }

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(20, calculated.intensity());
                    assertEquals(4.5f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }
            }

            @Nested
            class WithAccumulateIntensity {
                static ShockQueue makeQueue() {
                    var config = makeTestConfig();

                    config.setQueueDifferent(false);
                    config.setAccumulateIntensity(true);
                    config.setAccumulateDuration(true);

                    return new ShockQueue(config);
                }

                @Test
                void oneVibrationIsUnchanged() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoDifferentVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(60, calculated.intensity());
                    assertEquals(1.5f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoSameVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 2);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(80, calculated.intensity());
                    assertEquals(2.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoVibrationsCombineToShock() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 4);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(30, calculated.intensity());
                    assertEquals(1.5f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoDifferentShocksAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 5);
                    queue.queueShock(ShockDistribution.ALL, false, 6);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(70, calculated.intensity());
                    assertEquals(11f / 6f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void twoSameShocksAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 5);
                    queue.queueShock(ShockDistribution.ALL, false, 5);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(70, calculated.intensity());
                    assertEquals(2.0f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void vibrationAndShockAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 5);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(40, calculated.intensity());
                    assertEquals(1.4f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void threeVibrationsAreCombined() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 1);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.VIBRATE, calculated.type());
                    assertEquals(80, calculated.intensity());
                    assertEquals(11f / 6f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }

                @Test
                void threeVibrationsCombineToShock() throws InterruptedException {
                    var queue = makeQueue();
                    queue.queueShock(ShockDistribution.ALL, false, 1);
                    queue.queueShock(ShockDistribution.ALL, false, 2);
                    queue.queueShock(ShockDistribution.ALL, false, 3);

                    assertFalse(queue.isEmpty());

                    var calculated = queue.takeAndMergeShocks();

                    assertEquals(ShockDistribution.ALL, calculated.distribution());
                    assertEquals(OpType.SHOCK, calculated.type());
                    assertEquals(30, calculated.intensity());
                    assertEquals(2.5f, calculated.duration(), 0.001f);

                    assertTrue(queue.isEmpty());
                }
            }
        }
    }

    @Nested
    class VibrationOnlyTest {
        static ShockQueue makeQueue() {
            var config = makeTestConfig();

            config.setQueueDifferent(false);
            config.setVibrationOnly(true);

            return new ShockQueue(config);
        }

        public static Stream<Arguments> shockArguments() {
            // Damage which would normally trigger a shock, but now triggers a vibration
            return Stream.of(
                Arguments.of(4, 40),
                Arguments.of(5, 47),
                Arguments.of(6, 53),
                Arguments.of(7, 60),
                Arguments.of(8, 67),
                Arguments.of(9, 73),
                Arguments.of(10, 80),
                Arguments.of(11, 80),
                Arguments.of(471, 80)
            );
        }

        @Test
        void vibrationIsSmallerRange() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, 2);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.VIBRATE, calculated.type());
            assertEquals(27, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @ParameterizedTest
        @MethodSource("shockArguments")
        void shocksAreNowVibrations(int damage, int expectedIntensity) throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, false, damage);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.VIBRATE, calculated.type());
            assertEquals(expectedIntensity, calculated.intensity());
            assertEquals(1.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }

        @Test
        void deathShockIsVibration() throws InterruptedException {
            var queue = makeQueue();
            queue.queueShock(ShockDistribution.ALL, true, 1);

            assertFalse(queue.isEmpty());

            var calculated = queue.takeAndMergeShocks();

            assertEquals(ShockDistribution.ALL, calculated.distribution());
            assertEquals(OpType.VIBRATE, calculated.type());
            assertEquals(95, calculated.intensity());
            assertEquals(2.0f, calculated.duration(), 0.001f);

            assertTrue(queue.isEmpty());
        }
    }
}
