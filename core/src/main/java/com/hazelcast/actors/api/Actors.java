package com.hazelcast.actors.api;

import java.io.Serializable;

import static com.hazelcast.actors.utils.Util.notNull;

public interface Actors {

    public static class Terminate implements Serializable {
    }

    public static class Exit implements Serializable {
        private final ActorRef actorRef;

        public Exit(ActorRef actorRef) {
            this.actorRef = notNull(actorRef, "actorRef");
        }

        public ActorRef getActorRef() {
            return actorRef;
        }

        @Override
        public boolean equals(Object thatObj) {
            if (this == thatObj) return true;
            if (!(thatObj instanceof Exit)) return false;

            Exit that = (Exit) thatObj;
            if (!this.actorRef.equals(that.actorRef)) return false;
            return true;
        }

        @Override
        public int hashCode() {
            return actorRef != null ? actorRef.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "ActorTermination{" +
                    "actorRef=" + actorRef +
                    '}';
        }
    }
}
