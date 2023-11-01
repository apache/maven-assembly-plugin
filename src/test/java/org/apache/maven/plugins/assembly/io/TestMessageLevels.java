//Test cases added bby Kaushik Dhola for MessageLevels.java

package org.apache.maven.plugins.assembly.io;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestMessageLevels {

    @Test
    public void testGetLevelLabelValid() {
        int validLevel = 1;
        //getting the label for valid message level
        String label = MessageLevels.getLevelLabel(validLevel);

        // Assert the label if matches with "INFO"
        assertEquals("INFO", label);
    }

    @Test
    public void testGetLevelLabelInvalid() {
        int invalidLevel = 10;

        try {
            //check if an IllegalArgumentException is thrown for the invalid level
            MessageLevels.getLevelLabel(invalidLevel);
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException e) {

            //Assert the thrown exception
            assertEquals("Invalid message level: " + invalidLevel, e.getMessage());
        }
    }
}