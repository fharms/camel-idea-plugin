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
package org.apache.camel.idea.util;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.idea.catalog.CamelCatalogService;
import org.jetbrains.annotations.NotNull;

import static org.apache.camel.catalog.CatalogHelper.loadText;


/**
 * Service access for Camel libraries
 */
public class CamelService implements Disposable {

    Set<String> processedLibraries = new HashSet<>();

    boolean camelPresent;

    /**
     * @return true if Camel is present on the classpath
     */
    public boolean isCamelPresent() {
        return camelPresent;
    }

    /**
     * @param camelPresent - true if camel is present
     */
    public void setCamelPresent(boolean camelPresent) {
        this.camelPresent = camelPresent;
    }

    /**
     * @param lib - Add the of the library
     */
    public void addLibrary(String lib) {
        processedLibraries.add(lib);
    }

    /**
     * @return all cached library names
     */
    public Set<String> getLibraries() {
        return processedLibraries;
    }

    /**
     * Clean the library cache
     */
    public void clearLibraries() {
        processedLibraries.clear();
    }

    /**
     * @return true if the library name is cached
     */
    public boolean containsLibrary(String lib) {
        return processedLibraries.contains(lib);
    }

    /**
     * Scan for Apache Camel Libraries and update the cache and isCamelPresent
     */
    public void scanForCamelDependencies(@NotNull Module module) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

                String name = libraryOrderEntry.getPresentableName().toLowerCase();
                if (name.contains("org.apache.camel") && (libraryOrderEntry.getScope().isForProductionCompile() || libraryOrderEntry.getScope().isForProductionRuntime())) {
                    if (!isCamelPresent() && name.contains("camel-core") && !name.contains("camel-core-")
                        && (libraryOrderEntry.getLibrary() != null && libraryOrderEntry.getLibrary().getFiles(OrderRootType.CLASSES).length > 0)) {
                        setCamelPresent(true);
                    }

                    final Library library = libraryOrderEntry.getLibrary();
                    if (library == null) {
                        continue;
                    }
                    String[] split = name.split(":");
                    String artifactId = split[2].trim();
                    if (containsLibrary(artifactId)) {
                        continue;
                    }
                    addLibrary(artifactId);
                }
            }
        }
    }

    /**
     * Scan for Custom Camel Libraries and update the cache and camel catalog with custom components discovered
     */
    public void scanForCustomCamelDependencies(@NotNull Module module) {
        for (OrderEntry entry : ModuleRootManager.getInstance(module).getOrderEntries()) {
            if (entry instanceof LibraryOrderEntry) {
                LibraryOrderEntry libraryOrderEntry = (LibraryOrderEntry) entry;

                String name = libraryOrderEntry.getPresentableName().toLowerCase();
                if (libraryOrderEntry.getScope().isForProductionCompile() || libraryOrderEntry.getScope().isForProductionRuntime()) {
                    final Library library = libraryOrderEntry.getLibrary();
                    if (library == null) {
                        continue;
                    }
                    String[] split = name.split(":");
                    String artifactId = split[2].trim();
                    if (containsLibrary(artifactId)) {
                        continue;
                    }

                    CamelCatalog camelCatalog = ServiceManager.getService(CamelCatalogService.class).get();

                    // is there any custom Camel components in this library?
                    Properties properties = loadComponentProperties(library);
                    if (properties != null) {
                        String components = (String) properties.get("components");
                        if (components != null) {
                            String[] part = components.split("\\s");
                            for (String scheme : part) {
                                if (!camelCatalog.findComponentNames().contains(scheme)) {
                                    // find the class name
                                    String javaType = extractComponentJavaType(library, scheme);
                                    if (javaType != null) {
                                        String json = loadComponentJSonSchema(library, scheme);
                                        if (json != null) {
                                            camelCatalog.addComponent(scheme, javaType, json);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    addLibrary(artifactId);
                }
            }
        }
    }

    public static Properties loadComponentProperties(Library library) {
        Properties answer = new Properties();

        try {
            VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
            Optional<VirtualFile> vf = Arrays.stream(files).filter((f) -> f.getName().equals("component.properties")).findFirst();
            if (vf.isPresent()) {
                InputStream is = vf.get().getInputStream();
                if (is != null) {
                    answer.load(is);
                }
            }
        } catch (Throwable e) {
            // ignore
        }

        return answer;
    }

    public static String extractComponentJavaType(Library library, String scheme) {
        try {
            VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
            Optional<VirtualFile> vf = Arrays.stream(files).filter((f) -> f.getName().equals(scheme)).findFirst();
            if (vf.isPresent()) {
                InputStream is = vf.get().getInputStream();
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    return (String) props.get("class");
                }
            }
        } catch (Throwable e) {
            // ignore
        }

        return null;
    }

    public static String loadComponentJSonSchema(Library library, String scheme) {
        String answer = null;

        try {
            // is it a JAR file
            VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);
            Optional<VirtualFile> vf = Arrays.stream(files).filter((f) -> f.getName().equals(scheme + ".json")).findFirst();
            if (vf.isPresent()) {
                InputStream is = vf.get().getInputStream();
                if (is != null) {
                    answer = loadText(is);
                }
            }
        } catch (Throwable e) {
            // ignore
        }

        return answer;
    }

    @Override
    public void dispose() {
        processedLibraries.clear();
    }
}
