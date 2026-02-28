package org.popcraft.bolt.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class BoltPlayer {
    private final UUID uuid;
    private final Set<Mode> modes = new HashSet<>();
    private Action action;
    private Action lastAction;
    private boolean interacted;
    private boolean interactionCancelled;

    public BoltPlayer(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void clearAction() {
        final Action triggered = action;
        if (triggered != null && !hasMode(Mode.PERSIST)) {
            this.lastAction = triggered;
            this.action = null;
        }
    }

    public boolean triggeredAction() {
        return lastAction != null;
    }

    public boolean hasInteracted() {
        return interacted;
    }

    public boolean isInteractionCancelled() {
        return interactionCancelled;
    }

    public void setInteracted(final boolean cancelled) {
        this.interacted = true;
        this.interactionCancelled = cancelled;
    }

    public void clearInteraction() {
        this.lastAction = null;
        this.interacted = false;
        this.interactionCancelled = false;
    }

    public boolean hasMode(final Mode mode) {
        return this.modes.contains(mode);
    }

    public void toggleMode(final Mode mode) {
        if (this.modes.contains(mode)) {
            modes.remove(mode);
        } else {
            modes.add(mode);
        }
        if (!this.modes.contains(Mode.PERSIST)) {
            this.action = null;
        }
    }
}
