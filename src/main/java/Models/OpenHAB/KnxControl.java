package Models.OpenHAB;

public interface KnxControl {

    String toThingFormat();

    String toItemFormat();

    String toSitemapFormat();

    String getName();
}
