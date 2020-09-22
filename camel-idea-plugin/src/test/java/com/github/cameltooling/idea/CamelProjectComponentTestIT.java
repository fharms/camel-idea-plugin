/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cameltooling.idea;

import java.io.*;
import java.util.*;

import com.github.cameltooling.idea.service.CamelService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.JavaModuleTestCase;
import com.intellij.testFramework.ModuleTestCase;
import com.intellij.util.ui.UIUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jetbrains.annotations.NotNull;

import static com.github.cameltooling.idea.CamelTestHelper.checkJavaSwingTimersAreDisposed;

/**
 * Test if the {@link CamelService} service is updated correctly when changes happen to
 * the Project and model configuration
 */
public class CamelProjectComponentTestIT extends JavaModuleTestCase {

    private String CAMEL_VERSION;
    private File root;

    @Override
    protected void setUp() throws Exception {
        final String projectRoot = System.getProperty("user.dir").substring(0, System.getProperty("user.dir").lastIndexOf('/'));
        Properties gradleProperties = new Properties();
        gradleProperties.load(new FileInputStream(projectRoot +"/gradle.properties"));
        CAMEL_VERSION = gradleProperties.getProperty("camelVersion");

        super.setUp();
        root = new File(FileUtil.getTempDirectory());
    }

    @Override
    protected void tearDown() throws Exception {
        checkJavaSwingTimersAreDisposed();
        super.tearDown();
    }

    @Override
    protected @NotNull LanguageLevel getProjectLanguageLevel() {
        return LanguageLevel.JDK_11;
    }

    protected Sdk getTestProjectJdk() {
        return IdeaTestUtil.getMockJdk9();
    }

    public void testAddLibrary() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        File camelJar = createTestArchive(String.format("camel-core-%s.jar", CAMEL_VERSION));
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(camelJar);

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);
        addLibraryToModule(virtualFile, projectLibraryTable, String.format("Maven: org.apache.camel:camel-core:%s-snapshot", CAMEL_VERSION));

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
        assertEquals(true, service.isCamelPresent());
    }

    public void testRemoveLibrary() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        VirtualFile camelCoreVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive(String.format("camel-core-%s.jar", CAMEL_VERSION)));
        VirtualFile camelSpringVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive(String.format("camel-spring-%s.jar", CAMEL_VERSION)));

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);

        Library springLibrary = addLibraryToModule(camelSpringVirtualFile, projectLibraryTable, String.format("Maven: org.apache.camel:camel-spring:%s-snapshot", CAMEL_VERSION));
        Library coreLibrary = addLibraryToModule(camelCoreVirtualFile, projectLibraryTable, String.format("Maven: org.apache.camel:camel-core:%s-snapshot", CAMEL_VERSION));

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(2, service.getLibraries().size());
        assertEquals(true, service.isCamelPresent());

        ApplicationManager.getApplication().runWriteAction(() -> projectLibraryTable.removeLibrary(springLibrary));

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
    }

    public void testAddModule() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        File camelJar = createTestArchive(String.format("camel-core-%s.jar", CAMEL_VERSION));
        VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(camelJar);

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);
        ApplicationManager.getApplication().runWriteAction(() -> {
            final Module moduleA = createModule("myNewModel.iml");
            Library library = projectLibraryTable.createLibrary(String.format("Maven: org.apache.camel:camel-core:%s-snapshot", CAMEL_VERSION));
            final Library.ModifiableModel libraryModifiableModel = library.getModifiableModel();
            libraryModifiableModel.addRoot(virtualFile, OrderRootType.CLASSES);
            libraryModifiableModel.commit();
            ModuleRootModificationUtil.addDependency(moduleA, library);
        });

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
    }

    public void testAddLegacyPackaging() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        VirtualFile camelCoreVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive(String.format("camel-core-%s.jar", CAMEL_VERSION)));
        VirtualFile legacyJarPackagingFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive("legacy-custom-file-0.12.snapshot.jar"));

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);

        addLibraryToModule(camelCoreVirtualFile, projectLibraryTable, String.format("Maven: org.apache.camel:camel-core:%s-snapshot", CAMEL_VERSION));
        addLibraryToModule(legacyJarPackagingFile, projectLibraryTable, "c:\\test\\libs\\legacy-custom-file-0.12.snapshot.jar");

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
        assertEquals(true, service.getLibraries().contains("camel-core"));
    }

    public void testAddLibWithoutMavenPackaging() throws IOException {
        CamelService service = ServiceManager.getService(myProject, CamelService.class);
        assertEquals(0, service.getLibraries().size());

        VirtualFile camelCoreVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(createTestArchive(String.format("camel-core-%s.jar", CAMEL_VERSION)));

        final LibraryTable projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(myProject);

        addLibraryToModule(camelCoreVirtualFile, projectLibraryTable, String.format("org.apache.camel:camel-core:%s-snapshot", CAMEL_VERSION));

        UIUtil.dispatchAllInvocationEvents();
        assertEquals(1, service.getLibraries().size());
        assertEquals(true, service.getLibraries().contains("camel-core"));

    }

    private File createTestArchive(String filename) throws IOException {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class, filename)
            .addClasses(CamelService.class);
        File file = new File(root, archive.getName());
        file.deleteOnExit();
        archive.as(ZipExporter.class).exportTo(file, true);
        return file;
    }

    private Library addLibraryToModule(VirtualFile camelSpringVirtualFile, LibraryTable projectLibraryTable, String name) {
        return ApplicationManager.getApplication().runWriteAction((Computable<Library>)() -> {
            Library library = projectLibraryTable.createLibrary(name);
            Library.ModifiableModel libraryModifiableModel = library.getModifiableModel();
            libraryModifiableModel.addRoot(camelSpringVirtualFile, OrderRootType.CLASSES);
            libraryModifiableModel.commit();
            ModuleRootModificationUtil.addDependency(getModule(), library);
            return library;
        });
    }
}
