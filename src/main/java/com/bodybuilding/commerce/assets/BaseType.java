package com.bodybuilding.commerce.assets;

public abstract class BaseType {
    public String getSource() {
        return Source;
    }

    public Integer getId() {
        return Id;
    }

    protected final Integer Id;
    protected final String Source;
    protected final long threadCreatedOnId;

    public BaseType(Integer id, String source) {
        Id = id;
        Source = source;
        threadCreatedOnId = Thread.currentThread().getId();
    }

    public abstract String getType();

    public String makeSourceString(){
        return this.getType() + ":" + this.Id + "[T=" + threadCreatedOnId + "]->" + this.getSource();
    }

    @Override
    public String toString() {
        return this.getType() + "{" +
            "Id='" + Id + '\'' +
            ", Source='" + Source + '\'' +
            ", thread='" + threadCreatedOnId + '\'' +
            '}';
    }

    public long getThreadCreatedOnId() {
        return threadCreatedOnId;
    }

    @Override
    public int hashCode() {
        return Id.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseType that = (BaseType) o;

        //We purposely do not compare type or threadId here...B:55 and A:55 are considered equal in this very fake scenario!
        if (!Id.equals(that.Id)) return false;

        return true;
    }
}
