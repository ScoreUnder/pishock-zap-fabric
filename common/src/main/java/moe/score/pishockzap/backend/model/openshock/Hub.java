package moe.score.pishockzap.backend.model.openshock;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Accessors(fluent = true)
@FieldDefaults(level = lombok.AccessLevel.PRIVATE)
public class Hub {
    List<Shocker> shockers = List.of();
    String id;
    String name;
    String createdOn;
}
