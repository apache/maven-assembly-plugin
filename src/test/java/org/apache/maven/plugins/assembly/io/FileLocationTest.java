//Testcases written by Kaushik Dhola for io/FileLocation.java
package org.apache.maven.plugins.assembly.io;
import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FileLocationTest{


        @Test
        public void testSetFile_Success() throws Exception {

            FileLocation fileLocation = new FileLocation("testSpecification");
            File file = new File("testFile.txt");

            // Set file to the location
            fileLocation.setFile(file);

            // check id the file is in the given location
            File obtainedFile = fileLocation.unsafeGetFile();
            assertEquals(file, obtainedFile);
        }


    @Test(expected = IllegalStateException.class)
    public void testSetFile_WhenChannelOpen() throws Exception {


//        Testing the file when the channel is open
        FileLocation fileLocation = mock(FileLocation.class);
        doThrow(IllegalStateException.class).when(fileLocation).setFile(any(File.class));

//        Simulates open file location
        fileLocation.open();

//        Trying to set the file when chennel is open
        fileLocation.setFile(new File("testFile.txt"));
    }
}
