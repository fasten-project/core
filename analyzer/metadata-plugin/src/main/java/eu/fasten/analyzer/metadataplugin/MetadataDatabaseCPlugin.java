/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.fasten.analyzer.metadataplugin;

import eu.fasten.core.data.CScope;
import eu.fasten.core.data.CNode;
import eu.fasten.core.data.Constants;
import eu.fasten.core.data.ExtendedRevisionJavaCallGraph;
import eu.fasten.core.data.ExtendedRevisionCCallGraph;
import eu.fasten.core.data.ExtendedRevisionCallGraph;
import eu.fasten.core.data.Graph;
import eu.fasten.core.data.JavaType;
import eu.fasten.core.data.JavaScope;
import eu.fasten.core.data.FastenURI;
import eu.fasten.core.data.graphdb.GidGraph;
import eu.fasten.core.data.metadatadb.MetadataDao;
import eu.fasten.core.data.metadatadb.codegen.enums.ReceiverType;
import eu.fasten.core.data.metadatadb.codegen.tables.records.CallablesRecord;
import eu.fasten.core.data.metadatadb.codegen.tables.records.EdgesRecord;
import eu.fasten.core.data.metadatadb.codegen.udt.records.ReceiverRecord;
import eu.fasten.core.plugins.DBConnector;
import eu.fasten.core.plugins.KafkaPlugin;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import org.jooq.DSLContext;
import org.jooq.JSONB;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.BatchUpdateException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MetadataDatabaseCPlugin extends Plugin {
    public MetadataDatabaseCPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Extension
    public static class MetadataDBCExtension extends MetadataDBExtension {

        /**
         * Saves a callgraph of new format to the database to appropriate tables.
         * We override this method because we want to save the architecture
         * of the package.
         *
         * @param callGraph   Call graph to save to the database.
         * @param metadataDao Data Access Object to insert records in the database
         * @return Package ID saved in the database
         */
        protected long saveToDatabase(ExtendedRevisionCallGraph callGraph, MetadataDao metadataDao) {
            ExtendedRevisionCCallGraph CCallGraph = (ExtendedRevisionCCallGraph) callGraph;
            // Insert package record
            final long packageId = metadataDao.insertPackage(CCallGraph.product, CCallGraph.forge);

            // Insert package version record
            final long packageVersionId = metadataDao.insertPackageVersion(packageId,
                    CCallGraph.getCgGenerator(), CCallGraph.version, CCallGraph.architecture,
                    getProperTimestamp(CCallGraph.timestamp), new JSONObject());

            var callables = instertDataExtractCallables(callGraph, metadataDao, packageVersionId);
            final var numInternal = callables.size();

            var callablesIds = new LongArrayList(callables.size());
            // Save all callables in the database
            callablesIds.addAll(metadataDao.insertCallablesSeparately(callables, numInternal));

            // Build a map from callable Local ID to Global ID
            var lidToGidMap = new Long2LongOpenHashMap();
            for (int i = 0; i < callables.size(); i++) {
                lidToGidMap.put(callables.get(i).getId().longValue(), callablesIds.getLong(i));
            }

            // Insert all the edges
            var edges = insertEdges(callGraph.getGraph(), lidToGidMap, metadataDao);

            // Create a GID Graph for production
            this.gidGraph = new GidGraph(packageVersionId, callGraph.product, callGraph.version,
                    callablesIds, numInternal, edges);
            return packageVersionId;
        }

        /**
         * Get callables from CScope, and insert files and modules for internal
         * scopes and external static functions.
         */
        public ArrayList<CallablesRecord> getCallables(final Map<CScope, Map<String, Map<Integer, CNode>>> cha, CScope scope,
                boolean isInternal, boolean saveFiles, long packageVersionId, MetadataDao metadataDao) {
            var callables = new ArrayList<CallablesRecord>();
            for (final var name : cha.get(scope).entrySet()) {
                for (final var method : name.getValue().entrySet()) {
                    // We use dummy modules to connect files to callables
                    // otherwise we set the global "C" as the namespace.
                    String namespace = saveFiles ? null : "C";
                    var moduleId = -1L;
                    // Save module
                    if (saveFiles) {
                        // We save only the first file of a CNode
                        var fileId = metadataDao.insertFile(packageVersionId, method.getValue().getFile());
                        // Check if dummy module already exist for this file.
                        moduleId = metadataDao.getModuleContent(fileId);
                        if (moduleId == -1L) {
                            moduleId = metadataDao.insertModule(packageVersionId, namespace, null, null, true);
                        }
                        metadataDao.insertModuleContent(moduleId, fileId);
                        // Save binary Module
                        if (scope.equals(CScope.internalBinary)) {
                            var binModuleId = metadataDao.insertBinaryModule(packageVersionId, name.getKey(), null, null);
                            metadataDao.insertBinaryModuleContent(binModuleId, fileId);
                        }
                    }
                    // Save Callable
                    var localId = (long) method.getKey();
                    String uri = method.getValue().getUri().toString();
                    var callableMetadata = new JSONObject(method.getValue().getMetadata());
                    Integer firstLine = null;
                    if (callableMetadata.has("first")) {
                        if (!(callableMetadata.get("first") instanceof String))
                            firstLine = callableMetadata.getInt("first");
                        else
                            firstLine = Integer.parseInt(callableMetadata.getString("first"));
                        callableMetadata.remove("first");
                    }
                    Integer lastLine = null;
                    if (callableMetadata.has("last")) {
                        if (!(callableMetadata.get("last") instanceof String))
                            lastLine = callableMetadata.getInt("last");
                        else
                            lastLine = Integer.parseInt(callableMetadata.getString("last"));
                        callableMetadata.remove("last");
                    }
                    callableMetadata.put("type", scope);
                    callables.add(new CallablesRecord(localId, moduleId, uri, isInternal, null,
                        firstLine, lastLine, JSONB.valueOf(callableMetadata.toString())));
                }
            }
            return callables;
        }

        public ArrayList<CallablesRecord> instertDataExtractCallables(ExtendedRevisionCallGraph callgraph, MetadataDao metadataDao, long packageVersionId) {
            ExtendedRevisionCCallGraph CCallGraph = (ExtendedRevisionCCallGraph) callgraph;
            var callables = new ArrayList<CallablesRecord>();
            var cha = CCallGraph.getClassHierarchy();

            callables.addAll(getCallables(cha, CScope.internalBinary, true, true, packageVersionId, metadataDao));
            callables.addAll(getCallables(cha, CScope.internalStaticFunction, true, true, packageVersionId, metadataDao));
            callables.addAll(getCallables(cha, CScope.externalProduct, false, false, packageVersionId, metadataDao));
            callables.addAll(getCallables(cha, CScope.externalUndefined, false, false, packageVersionId, metadataDao));
            callables.addAll(getCallables(cha, CScope.externalStaticFunction, false, true, packageVersionId, metadataDao));

            return callables;
        }

        protected List<EdgesRecord> insertEdges(Graph graph,
                                 Long2LongOpenHashMap lidToGidMap, MetadataDao metadataDao) {
            final var numEdges = graph.getInternalCalls().size() + graph.getExternalCalls().size();

            // Map of all edges (internal and external)
            var graphCalls = graph.getInternalCalls();
            graphCalls.putAll(graph.getExternalCalls());

            var edges = new ArrayList<EdgesRecord>(numEdges);
            for (var edgeEntry : graphCalls.entrySet()) {

                // Get Global ID of the source callable
                var source = lidToGidMap.get((long) edgeEntry.getKey().get(0));
                // Get Global ID of the target callable
                var target = lidToGidMap.get((long) edgeEntry.getKey().get(1));

                // Create receivers
                var receivers = new ReceiverRecord[0];

                // Add edge record to the list of records
                edges.add(new EdgesRecord(source, target, receivers, JSONB.valueOf("{}")));
            }

            // Batch insert all edges
            final var edgesIterator = edges.iterator();
            while (edgesIterator.hasNext()) {
                var edgesBatch = new ArrayList<EdgesRecord>(Constants.insertionBatchSize);
                while (edgesIterator.hasNext()
                        && edgesBatch.size() < Constants.insertionBatchSize) {
                    edgesBatch.add(edgesIterator.next());
                }
                metadataDao.batchInsertEdges(edgesBatch);
            }
            return edges;
        }
    }
}

