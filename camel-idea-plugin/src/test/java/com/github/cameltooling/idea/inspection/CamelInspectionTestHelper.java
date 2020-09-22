package com.github.cameltooling.idea.inspection;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.LanguageLevelModuleExtension;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.InspectionTestCase;
import com.intellij.testFramework.JavaInspectionTestCase;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JpsJavaSdkType;

import static com.github.cameltooling.idea.CamelTestHelper.checkJavaSwingTimersAreDisposed;

/**
 * Test helper for setup test case to test inspection code. The class create a new {@link LightProjectDescriptor} for
 * each test to make sure it start with a clean state and all previous added libraries is removed
 *
 */
public abstract class CamelInspectionTestHelper extends JavaInspectionTestCase {

    private static final String BUILD_MOCK_JDK_DIRECTORY = "build/mockJDK-";

    public static final String CAMEL_CORE_MAVEN_ARTIFACT = "org.apache.camel:camel-core:2.22.0";

    @Override
    protected void tearDown() throws Exception {
        checkJavaSwingTimersAreDisposed();
        super.tearDown();
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        LanguageLevel languageLevel = LanguageLevel.JDK_1_8;
        return new DefaultLightProjectDescriptor() {
            @Override
            public Sdk getSdk() {
                return IdeaTestUtil.getMockJdk18();
            }

            @Override
            public void configureModule(@NotNull Module module, @NotNull ModifiableRootModel model, @NotNull ContentEntry contentEntry) {
                model.getModuleExtension( LanguageLevelModuleExtension.class ).setLanguageLevel( languageLevel );
            }
        };
    }

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/";
    }
}
