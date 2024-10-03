/*
 * Copyright 2019 Web3 Labs Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.console.services;

import java.io.IOException;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.web3j.console.config.CliConfig;
import org.web3j.console.config.ConfigManager;
import org.web3j.console.utils.CliVersion;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class UpdaterTest {

    private WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().port(8089));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);
    }

    @AfterEach
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void testPromptIfUpdateAvailableWhenUpdateIsAvailable() throws IOException {
        // Mock static CliVersion class
        try (MockedStatic<CliVersion> cliVersionMock = mockStatic(CliVersion.class)) {
            cliVersionMock.when(CliVersion::getVersion).thenReturn("1.0.0");
            CliConfig mockConfig = mock(CliConfig.class);
            ConfigManager.config = mockConfig;

            // Mock config's getUpdatePrompt() method
            when(mockConfig.getUpdatePrompt())
                    .thenReturn("curl -L get.web3j.io | sh && source ~/.web3j/source.sh");

            // Set up WireMock to simulate GitHub response for the latest version
            wireMockServer.stubFor(
                    get(urlEqualTo("/repos/hyperledger/web3j-cli/releases/latest"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withBody("{ \"tag_name\": \"v1.1.0\" }")));
            String originalUrl = Updater.GITHUB_API_URL;
            Updater.GITHUB_API_URL =
                    "http://localhost:8089/repos/hyperledger/web3j-cli/releases/latest";

            try {
                Updater.promptIfUpdateAvailable();
            } finally {
                Updater.GITHUB_API_URL = originalUrl;
            }

            // Verify that the update prompt was called with the correct message
            Mockito.verify(mockConfig).getUpdatePrompt();
        }
    }

    @Test
    public void testPromptIfUpdateAvailableWhenNoUpdateIsAvailable() throws IOException {
        // Mock static CliVersion class
        try (MockedStatic<CliVersion> cliVersionMock = mockStatic(CliVersion.class)) {
            cliVersionMock.when(CliVersion::getVersion).thenReturn("1.1.0");
            CliConfig mockConfig = mock(CliConfig.class);
            ConfigManager.config = mockConfig;

            // Set up WireMock to simulate GitHub response for the latest version
            wireMockServer.stubFor(
                    get(urlEqualTo("/repos/hyperledger/web3j-cli/releases/latest"))
                            .willReturn(
                                    aResponse()
                                            .withStatus(200)
                                            .withBody("{ \"tag_name\": \"v1.1.0\" }")));

            String originalUrl = Updater.GITHUB_API_URL;
            Updater.GITHUB_API_URL =
                    "http://localhost:8089/repos/hyperledger/web3j-cli/releases/latest";

            try {
                Updater.promptIfUpdateAvailable();
            } finally {
                Updater.GITHUB_API_URL = originalUrl;
            }
            Mockito.verify(mockConfig, Mockito.never()).getUpdatePrompt();
        }
    }
}
