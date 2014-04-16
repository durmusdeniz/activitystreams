package com.ibm.common.geojson;

import java.io.Serializable;
import java.util.Map;

import com.google.common.base.Objects;
import com.google.common.base.Supplier;

import static com.google.common.collect.ImmutableMap.copyOf;
import static com.google.common.collect.Maps.newLinkedHashMap;

@SuppressWarnings("unchecked")
public abstract class GeoObject<G extends GeoObject<G>>
  implements Serializable {

  private static final long serialVersionUID = 8852811044366213922L;

  public static enum Type {
    POINT,
    MULTIPOINT,
    LINESTRING,
    MULTILINESTRING,
    POLYGON,
    MULTIPOLYGON,
    GEOMETRYCOLLECTION,
    FEATURE,
    FEATURECOLLECTION
  }

  public static abstract class Builder
    <G extends GeoObject<G>, B extends Builder<G,B>> 
    implements Supplier<G> {
    
    protected boolean withBoundingBox = false;
    protected Type type;
    protected Map<String,Object> data = 
      newLinkedHashMap();
    
    public B calculateBoundingBox() {
      this.withBoundingBox = true;
      return (B)this;
    }
    
    protected B from(GeoObject<?> geo) {
      data.putAll(geo.data);
      return (B)this;
    }
    
    public B type(Type type) {
      this.type = type;
      return (B)this;
    }
    
    public B crs(CRS crs) {
      return set("crs", crs);
    }
    
    public B boundingBox(BoundingBox bbox) {
      return set("bbox", bbox);
    }
    
    public B set(String name, Object val) {
      if (val != null)
        this.data.put(name,val);
      else if (this.data.containsKey(name))
        this.data.remove(name);
      return (B)this;
    }
    
    public final G get() {
      preGet();
      G g =  doGet();
      return withBoundingBox ? g.withBoundingBox() : g;
    }
    
    protected void preGet() {}
    
    protected abstract G doGet();
  }
  
  final Type type;
  final Map<String,Object> data;
  
  protected GeoObject(Builder<?,?> builder) {
    this.type = builder.type;
    this.data = copyOf(builder.data);
  }
  
  public Type type() {
    return type;
  }
  
  public <T>T get(String name) {
    return (T)data.get(name);
  }
  
  public <T>T get(String name, T defaultValue) {
    T val = get(name);
    return val != null ? val : defaultValue;
  }
  
  public boolean has(String name) {
    return data.containsKey(name);
  }
  
  public CRS crs() {
    return this.<CRS>get("crs", null);
  }
  
  public BoundingBox boundingBox() {
    return this.<BoundingBox>get("bbox", null);
  }

  public final G withBoundingBox() {
    return has("bbox") ? 
      (G)this : makeWithBoundingBox();
  }
  
  protected abstract G makeWithBoundingBox();
  
  public static final Position position(float x, float y) {
    return new Position.Builder()
      .northing(x)
      .easting(y)
      .get();
  }
  
  public static final Position position(float x, float y, float z) {
    return new Position.Builder()
      .northing(x)
      .easting(y)
      .altitude(z)
      .get();
  }
  
  public String toString() {
    return Objects.toStringHelper(GeoObject.class)
      .add("type", type)
      .add("data", data)
      .toString();
  }
    
  protected static abstract class AbstractSerializedForm
    <G extends GeoObject<G>, B extends GeoObject.Builder<G,B>>
      implements Serializable {
    private static final long serialVersionUID = -1950126276150975248L;
    private Type type;
    private Map<String,Object> data;
    AbstractSerializedForm(G obj) {
      this.type = obj.type();
      this.data = obj.data;
    }
    protected Object doReadResolve() {
      B builder = builder();
      builder.type(type);
      for (Map.Entry<String,Object> entry : data.entrySet()) {
        String key = entry.getKey();
        Object val = entry.getValue();
        if (!handle(builder, key,val))
          builder.data.put(key,val);
      }
      return builder.get();
    }
    protected boolean handle(
      B builder, 
      String key, 
      Object val) {
        return false;
    }
    protected abstract B builder();
  }
}
