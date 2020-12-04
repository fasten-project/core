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

package eu.fasten.analyzer.restapiplugin.mvn.api.impl;

import eu.fasten.analyzer.restapiplugin.mvn.KnowledgeBaseConnector;
import eu.fasten.analyzer.restapiplugin.mvn.api.PackageApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PackageApiServiceImpl implements PackageApiService {

    @Override
    public ResponseEntity<String> getPackageLastVersion(String package_name) {
        String result = KnowledgeBaseConnector.kbDao.getPackageLastVersion(package_name);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> getPackageVersions(String package_name,
                                                     short offset,
                                                     short limit) {
        String result = KnowledgeBaseConnector.kbDao.getPackageVersions(package_name, offset, limit);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> getPackageVersion(String package_name,
                                                    String package_version,
                                                    short offset,
                                                    short limit) {
        String result = KnowledgeBaseConnector.kbDao.getPackageVersion(
                package_name, package_version, offset, limit);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> getPackageMetadata(String package_name,
                                                     String package_version,
                                                     short offset,
                                                     short limit) {
        String result = KnowledgeBaseConnector.kbDao.getPackageMetadata(
                package_name, package_version, offset, limit);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<String> getPackageCallgraph(String package_name,
                                                      String package_version,
                                                      short offset,
                                                      short limit) {
        String result = KnowledgeBaseConnector.kbDao.getPackageCallgraph(
                package_name, package_version, offset, limit);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }
}
