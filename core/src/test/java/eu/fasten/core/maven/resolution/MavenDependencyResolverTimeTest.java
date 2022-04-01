/*
 * Copyright 2022 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.fasten.core.maven.resolution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import eu.fasten.core.maven.data.Dependency;
import eu.fasten.core.maven.data.Pom;
import eu.fasten.core.maven.data.Revision;
import eu.fasten.core.maven.data.VersionConstraint;

public class MavenDependencyResolverTimeTest {

    // target GAV is too new
    // multi-GAV is covered
    // timestamp <= matches
    // timestamp > does not match
    // simple direct dependency is too new
    // simple transitive dependency is too new
    // newest matching time from version ranges

    private MavenDependencyGraph data;
    private MavenDependencyResolver sut;

    private ResolverConfig config;

    @BeforeEach
    public void setup() {
        data = new MavenDependencyGraph();
        sut = new MavenDependencyResolver();
        sut.setData(data);
        config = new ResolverConfig();
    }

    @Test
    public void failsWhenSinglePomCannotBeFound() {
        sut.setData(mock(MavenDependencyGraph.class));
        var e = assertThrows(MavenResolutionException.class, () -> {
            sut.resolve(Set.of("a:a:1"), config);
        });
        assertEquals("Cannot find coordinate a:a:1", e.getMessage());
    }

    @Test
    public void checkThatConfigFieldIsUsedForSinglePoms() {
        var resolveAt = 1;

        mockDepGraph();
        addMockData("a", "b", "1", resolveAt, getPom("a", "b", "1", 0));

        resolve(resolveAt, "a:b:1");
        verify(data).find("a:b", Set.of(new VersionConstraint("1")), resolveAt);
    }

    @Test
    public void resolutionOfMultiPomsDoesNotFail() {
        mockDepGraph();
        resolve(10L, "a:b:1", "b:c:2");
    }

    @Test
    public void resolutionOfDependenciesPropagatesTimestamp() {
        var resolveAt = 10L;
        mockDepGraph();
        resolve(resolveAt, "a:b:1", "b:c:2");
        verify(data).find(eq("a:b"), anySet(), eq(resolveAt));
        verify(data).find(eq("b:c"), anySet(), eq(resolveAt));
    }

    @Test
    public void failIfFoundSinglePomIsTooNew() {
        var resolveAt = 0;

        mockDepGraph();
        addMockData("a", "b", "1", resolveAt, getPom("a", "b", "1", 1));

        var e = assertThrows(MavenResolutionException.class, () -> {
            resolve(resolveAt, "a:b:1");
        });
        assertEquals("Requested POM has been released after resolution timestamp", e.getMessage());
    }

    @Test
    public void basic() {
        add(1, "a:1", "b:1");
        add(2, "b:1");
        assertThrows(MavenResolutionException.class, () -> {
            assertDepSet(0, "a:1");
        });
        assertDepSet(1, "a:1");
        assertDepSet(2, "a:1", "b:1");
    }

    @Test
    public void version() {
        add(1, "a:1", "b:[1,2]");
        add(2, "b:1.1");
        add(3, "b:1.2");
        add(4, "b:1.3");
        assertDepSet(1, "a:1");
        assertDepSet(2, "a:1", "b:1.1");
        assertDepSet(3, "a:1", "b:1.2");
        assertDepSet(4, "a:1", "b:1.3");
    }

    private void add(long releaseDate, String from, String... tos) {
        var pom = new Pom();
        var parts = from.split(":");
        pom.groupId = parts[0];
        pom.artifactId = parts[0];
        pom.version = parts[1];
        pom.releaseDate = releaseDate;

        for (var to : tos) {
            var partsTo = to.split(":");
            var d = new Dependency(partsTo[0], partsTo[0], partsTo[1]);
            pom.dependencies.add(d);
        }

        data.add(pom);
    }

    private void assertDepSet(long resolveAt, String shortBase, String... deps) {
        var baseParts = shortBase.split(":");
        var base = String.format("%s:%s:%s", baseParts[0], baseParts[0], baseParts[1]);

        config.timestamp = resolveAt;
        var actuals = sut.resolve(Set.of(base), config);
        var expecteds = Arrays.stream(deps) //
                .map(dep -> dep.split(":")) //
                .map(parts -> new Revision(parts[0], parts[0], parts[1], new Timestamp(-1L))) //
                .collect(Collectors.toSet());
        expecteds.add(new Revision(baseParts[0], baseParts[0], baseParts[1], new Timestamp(-1L)));

        assertEquals(expecteds, actuals);
    }

    private static Pom getPom(String g, String a, String v, long releaseDate) {
        var pom = new Pom();
        pom.groupId = g;
        pom.artifactId = a;
        pom.version = v;
        pom.releaseDate = releaseDate;
        return pom;
    }

    private void mockDepGraph() {
        data = mock(MavenDependencyGraph.class);
        sut.setData(data);
    }

    private void addMockData(String g, String a, String v, long resolveAt, Pom pom) {
        var ga = String.format("%s:%s", g, a);
        when(data.find(ga, Set.of(new VersionConstraint(v)), resolveAt)).thenReturn(pom);
    }

    private void resolve(long resolveAt, String... coords) {
        config.timestamp = resolveAt;
        sut.resolve(Set.of(coords), config);
    }
}