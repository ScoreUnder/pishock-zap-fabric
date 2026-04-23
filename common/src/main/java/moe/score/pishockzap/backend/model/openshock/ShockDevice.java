package moe.score.pishockzap.backend.model.openshock;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

@SuppressWarnings("ClassCanBeRecord")  // Records confuse older gson & we serialise this to config
@Accessors(fluent = true)
@Getter
@AllArgsConstructor
@EqualsAndHashCode
public class ShockDevice {
    private final ShockCollarModel model;
    private final int id;
}
