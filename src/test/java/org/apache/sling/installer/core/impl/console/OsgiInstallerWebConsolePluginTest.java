/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.installer.core.impl.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.info.InfoProvider;
import org.apache.sling.installer.api.info.InstallationState;
import org.apache.sling.installer.api.info.Resource;
import org.apache.sling.installer.api.info.ResourceGroup;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.testing.mock.sling.junit5.SlingContext;
import org.apache.sling.testing.mock.sling.junit5.SlingContextExtension;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.osgi.framework.Version;

import static org.junit.jupiter.api.Assertions.*;

/**
 *
 */
@ExtendWith(SlingContextExtension.class)
class OsgiInstallerWebConsolePluginTest {
    public final SlingContext context = new SlingContext();

    private OsgiInstallerWebConsolePlugin plugin;

    private InfoProvider mockInfoProvider;

    @BeforeEach
    void beforeEach() {
        mockInfoProvider = context.registerService(InfoProvider.class, Mockito.mock(InfoProvider.class));

        plugin = context.registerInjectActivateService(OsgiInstallerWebConsolePlugin.class);
    }

    /**
     * Test method for {@link org.apache.sling.installer.core.impl.console.OsgiInstallerWebConsolePlugin#getRelativeResourcePrefix()}.
     */
    @Test
    void testGetRelativeResourcePrefix() {
        assertNotNull(plugin.getRelativeResourcePrefix());
    }

    /**
     * Test method for {@link org.apache.sling.installer.core.impl.console.OsgiInstallerWebConsolePlugin#service(jakarta.servlet.ServletRequest, jakarta.servlet.ServletResponse)}.
     */
    @Test
    void testServiceWithNoResources() throws IOException {
        // mock InfoProvider
        mockInstallationState();

        final @NotNull MockSlingHttpServletRequest req = context.request();
        final @NotNull MockSlingHttpServletResponse resp = context.response();
        plugin.service(req, resp);
        final String outputAsString = resp.getOutputAsString();
        assertNotNull(outputAsString);
        assertTrue(outputAsString.contains("no resources registered"));
    }

    @Test
    void testServiceWithActiveResources() throws IOException {
        // mock InfoProvider
        final InstallationState mockInstallationState = mockInstallationState();
        mockBasicActiveResources(mockInstallationState);

        final @NotNull MockSlingHttpServletRequest req = context.request();
        final @NotNull MockSlingHttpServletResponse resp = context.response();
        plugin.service(req, resp);
        final String outputAsString = resp.getOutputAsString();
        assertNotNull(outputAsString);
        assertFalse(outputAsString.contains("no resources registered"));
    }

    @Test
    void testServiceWithInstalledResources() throws IOException {
        // mock InfoProvider
        final InstallationState mockInstallationState = mockInstallationState();
        mockBasicInstalledResources(mockInstallationState);

        final @NotNull MockSlingHttpServletRequest req = context.request();
        final @NotNull MockSlingHttpServletResponse resp = context.response();
        plugin.service(req, resp);
        final String outputAsString = resp.getOutputAsString();
        assertNotNull(outputAsString);
        assertFalse(outputAsString.contains("no resources registered"));
    }

    @Test
    void testServiceWithUntransformedResources() throws IOException {
        // mock InfoProvider
        final InstallationState mockInstallationState = mockInstallationState();
        mockBasicUntransformedResources(mockInstallationState);

        final @NotNull MockSlingHttpServletRequest req = context.request();
        final @NotNull MockSlingHttpServletResponse resp = context.response();
        plugin.service(req, resp);
        final String outputAsString = resp.getOutputAsString();
        assertNotNull(outputAsString);
        assertFalse(outputAsString.contains("no resources registered"));
    }

    /**
     * Test method for {@link org.apache.sling.installer.core.impl.console.OsgiInstallerWebConsolePlugin#printConfiguration(java.io.PrintWriter, java.lang.String)}.
     */
    @ParameterizedTest
    @ValueSource(strings = {"zip", "txt"})
    void testPrintConfigurationWithActiveResources(String mode) {
        // mock InfoProvider
        final InstallationState mockInstallationState = mockInstallationState();
        mockBasicActiveResources(mockInstallationState);

        final @NotNull MockSlingHttpServletResponse resp = context.response();
        final PrintWriter pw = resp.getWriter();
        plugin.printConfiguration(pw, mode);
        final String outputAsString = resp.getOutputAsString();
        assertNotNull(outputAsString);
        assertTrue(outputAsString.contains("Apache Sling OSGi Installer"));
    }

    @Test
    void testPrintConfigurationWithInstalledResources() {
        // mock InfoProvider
        final InstallationState mockInstallationState = mockInstallationState();
        mockBasicInstalledResources(mockInstallationState);

        final @NotNull MockSlingHttpServletResponse resp = context.response();
        final PrintWriter pw = resp.getWriter();
        plugin.printConfiguration(pw, "txt");
        final String outputAsString = resp.getOutputAsString();
        assertNotNull(outputAsString);
        assertTrue(outputAsString.contains("Apache Sling OSGi Installer"));
    }

    @Test
    void testPrintConfigurationWithUntransformedResources() {
        // mock InfoProvider
        final InstallationState mockInstallationState = mockInstallationState();
        mockBasicUntransformedResources(mockInstallationState);

        final @NotNull MockSlingHttpServletResponse resp = context.response();
        final PrintWriter pw = resp.getWriter();
        plugin.printConfiguration(pw, "zip");
        final String outputAsString = resp.getOutputAsString();
        assertNotNull(outputAsString);
        assertTrue(outputAsString.contains("Apache Sling OSGi Installer"));
    }

    @Test
    void testPrintConfigurationForInvalidMode() {
        final @NotNull MockSlingHttpServletResponse resp = context.response();
        final PrintWriter pw = resp.getWriter();
        plugin.printConfiguration(pw, "invalid");
        final String outputAsString = resp.getOutputAsString();
        assertNotNull(outputAsString);
        assertTrue(outputAsString.isEmpty());
    }

    private InstallationState mockInstallationState() {
        InstallationState mockInstallationState = Mockito.mock(InstallationState.class);
        Mockito.doReturn(mockInstallationState).when(mockInfoProvider).getInstallationState();
        return mockInstallationState;
    }

    private RegisteredResource mockInstallRegisteredResource(
            String type, String entityId, String scheme, Map<String, Object> dictionary) {
        RegisteredResource mockInstalledResource = Mockito.mock(RegisteredResource.class);
        Mockito.doReturn(type).when(mockInstalledResource).getType();
        Mockito.doReturn(entityId).when(mockInstalledResource).getEntityId();
        Mockito.doReturn(scheme).when(mockInstalledResource).getScheme();
        Mockito.doReturn(new Hashtable<>(dictionary))
                .when(mockInstalledResource)
                .getDictionary();
        return mockInstalledResource;
    }

    private Resource mockInstallResoure(
            String type,
            ResourceState state,
            String entityId,
            String scheme,
            Map<String, Object> dictionary,
            String error,
            String url,
            Version version) {
        Resource mockInstalledResource = Mockito.mock(Resource.class);
        Mockito.doReturn(type).when(mockInstalledResource).getType();
        Mockito.doReturn(state).when(mockInstalledResource).getState();
        Mockito.doReturn(entityId).when(mockInstalledResource).getEntityId();
        Mockito.doReturn(scheme).when(mockInstalledResource).getScheme();
        Mockito.doReturn(new Hashtable<>(dictionary))
                .when(mockInstalledResource)
                .getDictionary();
        Mockito.doReturn(error).when(mockInstalledResource).getError();
        Mockito.doReturn(url).when(mockInstalledResource).getURL();
        Mockito.doReturn(version).when(mockInstalledResource).getVersion();
        return mockInstalledResource;
    }

    private void mockBasicUntransformedResources(final InstallationState mockInstallationState) {
        // mock untransformed resources
        final RegisteredResource mockUntransformedResource1 = mockInstallRegisteredResource(
                InstallableResource.TYPE_CONFIG, "test1", "launchpad", Map.of("key1", "value1"));

        // and a second for code coverage
        final RegisteredResource mockUntransformedResource2 = mockInstallRegisteredResource(
                InstallableResource.TYPE_CONFIG, "test2", "launchpad", Map.of("key2", "value2"));

        // and a third for code coverage
        final RegisteredResource mockUntransformedResource3 = mockInstallRegisteredResource(
                InstallableResource.TYPE_PROPERTIES, "test3", "launchpad", Map.of("key3", "value3"));

        Mockito.doReturn(List.of(mockUntransformedResource1, mockUntransformedResource2, mockUntransformedResource3))
                .when(mockInstallationState)
                .getUntransformedResources();
    }

    private void mockBasicInstalledResources(final InstallationState mockInstallationState) {
        // mock installed resources
        ResourceGroup mockInstalledRG1 = Mockito.mock(ResourceGroup.class);
        Mockito.doReturn("alias1").when(mockInstalledRG1).getAlias();

        Resource mockInstalledResource1 = mockInstallResoure(
                InstallableResource.TYPE_CONFIG,
                ResourceState.INSTALLED,
                "config:factory1~test1",
                "launchpad",
                Map.of("key1", "value1", "key2", "value2"),
                "error1",
                "url1",
                new Version(1, 0, 0));
        // mock a last change timestamp for code coverage
        Mockito.doReturn(System.currentTimeMillis())
                .when(mockInstalledResource1)
                .getLastChange();
        // add mock attributes for code coverage
        Mockito.doReturn("excluded1").when(mockInstalledResource1).getAttribute(TaskResource.ATTR_INSTALL_EXCLUDED);
        Mockito.doReturn("info1").when(mockInstalledResource1).getAttribute(TaskResource.ATTR_INSTALL_INFO);

        Resource mockInstalledResource2 = mockInstallResoure(
                InstallableResource.TYPE_CONFIG,
                ResourceState.INSTALLED,
                "test2",
                "launchpad",
                Map.of("key2", "value2"),
                null,
                "url2",
                null);
        Mockito.doReturn(List.of(mockInstalledResource1, mockInstalledResource2))
                .when(mockInstalledRG1)
                .getResources();

        // and a second group for code coverage
        ResourceGroup mockInstalledRG2 = Mockito.mock(ResourceGroup.class);

        Resource mockInstalledResource3 = mockInstallResoure(
                InstallableResource.TYPE_CONFIG,
                ResourceState.INSTALLED,
                "test3",
                "launchpad",
                Map.of("key3", "value3"),
                null,
                null,
                null);
        Mockito.doReturn(List.of(mockInstalledResource3)).when(mockInstalledRG2).getResources();

        // and a third group for code coverage
        ResourceGroup mockInstalledRG3 = Mockito.mock(ResourceGroup.class);

        Resource mockInstalledResource4 = mockInstallResoure(
                InstallableResource.TYPE_BUNDLE,
                ResourceState.INSTALLED,
                "test4",
                "launchpad",
                Map.of("key4", "value4"),
                null,
                null,
                null);
        Mockito.doReturn(List.of(mockInstalledResource4)).when(mockInstalledRG3).getResources();

        // and a fourth group for code coverage
        ResourceGroup mockInstalledRG4 = Mockito.mock(ResourceGroup.class);

        Resource mockInstalledResource5 = mockInstallResoure(
                InstallableResource.TYPE_FILE,
                ResourceState.UNINSTALLED,
                "test5",
                "launchpad",
                Map.of("key5", "value5"),
                null,
                null,
                null);
        Mockito.doReturn(List.of(mockInstalledResource5)).when(mockInstalledRG4).getResources();

        // and a fifth group for code coverage
        ResourceGroup mockInstalledRG5 = Mockito.mock(ResourceGroup.class);

        Resource mockInstalledResource6 = mockInstallResoure(
                "invalid", ResourceState.INSTALLED, "test6", "launchpad", Map.of("key6", "value6"), null, null, null);
        Mockito.doReturn(List.of(mockInstalledResource6)).when(mockInstalledRG5).getResources();

        // and a sixth (empty) group for code coverage
        ResourceGroup mockInstalledRG6 = Mockito.mock(ResourceGroup.class);
        Mockito.doReturn(List.of()).when(mockInstalledRG6).getResources();

        Mockito.doReturn(List.of(
                        mockInstalledRG1,
                        mockInstalledRG2,
                        mockInstalledRG3,
                        mockInstalledRG4,
                        mockInstalledRG5,
                        mockInstalledRG6))
                .when(mockInstallationState)
                .getInstalledResources();
    }

    private void mockBasicActiveResources(final InstallationState mockInstallationState) {
        // mock active resources
        ResourceGroup mockActiveRG1 = Mockito.mock(ResourceGroup.class);
        Resource mockActiveResource1 = mockInstallResoure(
                InstallableResource.TYPE_CONFIG,
                ResourceState.INSTALLED,
                "config:factory1~test1",
                "launchpad",
                Map.of("key1", "value1", "key2", "value2"),
                null,
                null,
                null);
        Mockito.doReturn(List.of(mockActiveResource1)).when(mockActiveRG1).getResources();

        // and a second group for code coverage
        ResourceGroup mockActiveRG2 = Mockito.mock(ResourceGroup.class);
        Resource mockActiveResource2 = mockInstallResoure(
                InstallableResource.TYPE_CONFIG,
                ResourceState.INSTALLED,
                "test2",
                "launchpad",
                Map.of("key2", "value2"),
                null,
                null,
                null);
        Mockito.doReturn(List.of(mockActiveResource2)).when(mockActiveRG2).getResources();

        // and a third group for code coverage
        ResourceGroup mockActiveRG3 = Mockito.mock(ResourceGroup.class);
        Resource mockActiveResource3 = mockInstallResoure(
                InstallableResource.TYPE_PROPERTIES,
                ResourceState.INSTALLED,
                "test3",
                "launchpad",
                Map.of("key3", "value3"),
                null,
                null,
                null);
        Mockito.doReturn(List.of(mockActiveResource3)).when(mockActiveRG3).getResources();
        Mockito.doReturn(List.of(mockActiveRG1, mockActiveRG2, mockActiveRG3))
                .when(mockInstallationState)
                .getActiveResources();
    }
}
