package moe.score.pishockzap.util;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface Either<L, R> {
    boolean isLeft();

    default boolean isRight() {
        return !isLeft();
    }

    L left();

    R right();

    static <L, R> Either<L, R> left(L value) {
        return new Left<>(value);
    }

    static <L, R> Either<L, R> right(R value) {
        return new Right<>(value);
    }

    final class Left<L, R> implements Either<L, R> {
        private final L value;

        public Left(L value) {
            this.value = value;
        }

        @Override
        public boolean isLeft() {
            return true;
        }

        @Override
        public L left() {
            return value;
        }

        @Override
        public R right() {
            throw new UnsupportedOperationException("right() on left-sided Either");
        }
    }

    final class Right<L, R> implements Either<L, R> {
        private final R value;

        public Right(R value) {
            this.value = value;
        }

        @Override
        public boolean isLeft() {
            return false;
        }

        @Override
        public L left() {
            throw new UnsupportedOperationException("left() on right-sided Either");
        }

        @Override
        public R right() {
            return value;
        }
    }
}
