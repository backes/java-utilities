package de.hammacher.util;


/**
 * A simple class consisting of two independent values.
 *
 * @author Clemens Hammacher
 * @param <T1> the type of the first value
 * @param <T2> the type of the second value
 */
public class Pair<T1, T2> {

    private T1 first;
    private T2 second;

    /**
     * Constructor that initializes both fields with <code>null</code>.
     */
    public Pair() {
    	// nop
    }

    /**
     * Constructor that initializes both values.
     *
     * @param first first initial value
     * @param second second initial value
     */
    public Pair(final T1 first, final T2 second) {
        this.first = first;
        this.second = second;
    }

    public T1 getFirst() {
        return this.first;
    }

    public void setFirst(final T1 first) {
        this.first = first;
    }

    public T2 getSecond() {
        return this.second;
    }

    public void setSecond(final T2 second) {
        this.second = second;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.first == null) ? 0 : this.first.hashCode());
        result = prime * result + ((this.second == null) ? 0 : this.second.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Pair<?,?> other = (Pair<?,?>) obj;
        if (this.first == null) {
            if (other.first != null)
                return false;
        } else if (!this.first.equals(other.first))
            return false;
        if (this.second == null) {
            if (other.second != null)
                return false;
        } else if (!this.second.equals(other.second))
            return false;
        return true;
    }

    @Override
    public String toString() {
        final String firstString = String.valueOf(this.first);
        final String secondString = String.valueOf(this.second);
        return new StringBuilder(firstString.length() + secondString.length() + 4)
            .append('<').append(firstString).append(", ").append(secondString)
            .append('>').toString();
    }

}
