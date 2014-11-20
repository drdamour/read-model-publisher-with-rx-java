package com.bodybuilding.commerce.assets;

public class Asset {
    private final Integer Id;
    private final String type;
    private final String source;

    public String getSource() {
        return source;
    }

    public Integer getId() {
        return Id;
    }

    public String getType() {
        return type;
    }

    public Asset(String id, String type, String source) {
        Id = Integer.parseInt(id);
        this.type = type;
        this.source = source;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Asset asset = (Asset) o;

        if (!Id.equals(asset.Id)) return false;
        if (!source.equals(asset.source)) return false;
        if (!type.equals(asset.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = Id.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + source.hashCode();
        return result;
    }
}
