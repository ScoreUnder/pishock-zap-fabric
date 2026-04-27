package moe.score.pishockzap.backend.model.openshock;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Control {
    String id;
    ControlType type;
    int intensity;
    int duration;
    boolean exclusive;
}
