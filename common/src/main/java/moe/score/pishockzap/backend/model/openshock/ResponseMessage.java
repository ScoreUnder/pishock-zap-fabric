package moe.score.pishockzap.backend.model.openshock;

import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

@Getter
@Accessors(fluent = true)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class ResponseMessage<T> {
    String message;
    T data;
}
