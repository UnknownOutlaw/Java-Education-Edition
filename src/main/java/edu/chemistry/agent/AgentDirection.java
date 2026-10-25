package edu.chemistry.agent;

/** The directions the /agent commands take, the way Education words them: forward, back, left and right turn with the Agent. */
public enum AgentDirection {
    FORWARD("forward"),
    BACK("back"),
    LEFT("left"),
    RIGHT("right"),
    UP("up"),
    DOWN("down");

    public final String word;

    AgentDirection(String word) {
        this.word = word;
    }
}
