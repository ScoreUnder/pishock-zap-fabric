package moe.score.pishockzap.backend.model.openshock;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Getter
@Accessors(fluent = true)
public class Shocker {
    String name;
    boolean isPaused;
    String createdOn;
    String id;
    int rfId;
    ShockCollarModel model;
}
