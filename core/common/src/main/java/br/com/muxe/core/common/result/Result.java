package br.com.muxe.core.common.result;

import java.util.Objects;
import java.util.function.Function;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static Result<Void> success() {
        return new Success<>(null);
    }

    static <T> Result<T> failure(Problem problem) {
        return new Failure<>(problem);
    }

    static <T> Result<T> failure(String code, String message) {
        return new Failure<>(new Problem(code, message));
    }

    boolean isSuccess();

    default boolean isFailure() {
        return !isSuccess();
    }

    default <U> Result<U> map(Function<? super T, ? extends U> mapper) {
        return switch (this) {
            case Success<T> success -> new Success<>(mapper.apply(success.value()));
            case Failure<T> failure -> new Failure<>(failure.problem());
        };
    }

    default T orElseThrow() {
        return switch (this) {
            case Success<T> success -> success.value();
            case Failure<T> failure -> throw new IllegalStateException(failure.problem().code() + ": " + failure.problem().message());
        };
    }

    record Success<T>(T value) implements Result<T> {

        @Override
        public boolean isSuccess() {
            return true;
        }
    }

    record Failure<T>(Problem problem) implements Result<T> {

        public Failure {
            Objects.requireNonNull(problem, "problem");
        }

        @Override
        public boolean isSuccess() {
            return false;
        }
    }
}
