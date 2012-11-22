package io.brooklyn;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

import java.io.Serializable;

/**
 * Abstract message that contains some general purpose stuff like toString/equals/hash and also implements
 * serializable.
 *
 * You do not need to use this message if you don't want to.. it is just here for lazy people.
 */
public abstract class AbstractMessage implements Serializable {

    public int hashCode(){
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object thatObj){
        return EqualsBuilder.reflectionEquals(this, thatObj);
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }
}
