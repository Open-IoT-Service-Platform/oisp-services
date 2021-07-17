package info.oisp;

import static org.junit.jupiter.api.Assertions.*;

class GetJsonFieldTest {

    private final GetJsonField getJsonField = new GetJsonField();

    @org.junit.jupiter.api.Test
    void eval() {
        String testObject1 = null;
        String testObject2 = "";
        String testObject3 = "{}";
        String testObject4 = "{\"id\":\"urn:123\"}";
        String testObject5 = "{\"depth\": {\"type\": \"Property\", \"value\": \"1.0\"}}";

        assertEquals(getJsonField.eval(testObject1,"id"), null);
        assertEquals(getJsonField.eval(testObject2,"id"), null);
        assertEquals(getJsonField.eval(testObject2,""), null);
        assertEquals(getJsonField.eval(testObject2,null), null);
        assertEquals(getJsonField.eval(testObject3,"id"), null);
        assertEquals(getJsonField.eval(testObject4,"id"), "urn:123");
        assertEquals(getJsonField.eval(testObject4,"depth"), null);
        assertEquals(getJsonField.eval(testObject5,"depth"), "1.0");
    }
}