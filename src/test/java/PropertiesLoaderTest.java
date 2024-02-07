import org.junit.Assert;
import org.junit.Test;
import edu.uoc.allago.uocsubmissionsystem.PropertiesLoader;

public class PropertiesLoaderTest {

    @Test
    public void testGetProperty() {
        String property = PropertiesLoader.getProperty("key");
        Assert.assertEquals("uoc31416", property);
    }
}

/*
public class PropertiesLoaderTest {

    @Test
    public void testGetProperty() {
        String property = PropertiesLoader.getProperty("key");
        Assert.assertNotNull("Property key should not be null", property);
    }
}
 */