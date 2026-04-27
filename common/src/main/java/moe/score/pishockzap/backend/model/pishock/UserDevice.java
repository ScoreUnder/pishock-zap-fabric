package moe.score.pishockzap.backend.model.pishock;

import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor
public class UserDevice {
    public int clientId;
    public String name;
    public int userId;
    public String username;
    public List<Shocker> shockers;
}
