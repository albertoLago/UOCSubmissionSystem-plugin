import com.intellij.openapi.project.Project;
import edu.uoc.allago.uocsubmissionsystem.UserActionLogger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UserActionLoggerTest {

    @Mock
    Project mockProject;

    UserActionLogger userActionLogger;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        userActionLogger = new UserActionLogger(mockProject);
    }

    @Test
    public void shouldSkip_WithIrrelevantExtension_ReturnsTrue() {
        String fileName = "test.docx";
        boolean result = userActionLogger.shouldSkip(fileName);
        assertTrue(result);
    }

    @Test
    public void shouldSkip_WithRelevantExtension_ReturnsFalse() {
        String fileName = "test.java";
        boolean result = userActionLogger.shouldSkip(fileName);
        assertFalse(result);
    }

    @Test
    public void shouldSkip_WithNoExtension_ReturnsTrue() {
        String fileName = "test";
        boolean result = userActionLogger.shouldSkip(fileName);
        assertTrue(result);
    }

    @Test
    public void shouldSkip_WithExcludedFileName_ReturnsTrue() {
        String fileName = ".uoc.data";
        boolean result = userActionLogger.shouldSkip(fileName);
        assertTrue(result);
    }

    @Test
    public void shouldSkipCode_WithRelevantSequence_ReturnsTrue() {
        String code = "package MyPackage;";
        boolean result = userActionLogger.shouldSkipCode(code);
        assertTrue(result);
    }

    @Test
    public void shouldSkipCode_WithIrrelevantSequence_ReturnsFalse() {
        String code = "this is a test;";
        boolean result = userActionLogger.shouldSkipCode(code);
        assertFalse(result);
    }
}
