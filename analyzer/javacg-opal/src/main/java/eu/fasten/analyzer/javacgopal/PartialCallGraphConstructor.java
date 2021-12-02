/*
 * Copyright 2021 Delft University of Technology
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
package eu.fasten.analyzer.javacgopal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringEscapeUtils;
import org.opalj.br.Annotation;
import org.opalj.br.ClassFile;
import org.opalj.br.ElementValuePair;
import org.opalj.br.Method;
import org.opalj.br.ObjectType;
import org.opalj.br.analyses.Project;
import org.opalj.tac.AITACode;
import org.opalj.tac.ComputeTACAIKey$;
import org.opalj.tac.DUVar;
import org.opalj.tac.Stmt;
import org.opalj.tac.TACMethodParameter;
import org.opalj.value.ValueInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import eu.fasten.analyzer.javacgopal.data.OPALCallGraph;
import eu.fasten.analyzer.javacgopal.data.OPALClassHierarchy;
import eu.fasten.analyzer.javacgopal.data.OPALMethod;
import eu.fasten.analyzer.javacgopal.data.OPALType;
import eu.fasten.core.data.JavaGraph;
import eu.fasten.core.data.callgraph.CallPreservationStrategy;
import eu.fasten.core.data.callgraph.PartialCallGraph;
import eu.fasten.core.data.opal.exceptions.OPALException;
import scala.Function1;
import scala.collection.JavaConverters;

/**
 * Call graphs that are not still fully resolved. i.e. isolated call graphs
 * which within-artifact calls (edges) are known as internal calls and
 * Cross-artifact calls are known as external calls.
 */
public class PartialCallGraphConstructor {

	private static final Logger logger = LoggerFactory.getLogger(PartialCallGraph.class);

	private PartialCallGraph pcg;

	/**
	 * Given a file, algorithm and main class (in case of application package) it
	 * creates a {@link PartialCallGraph} for it using OPAL.
	 *
	 * @param ocg call graph constructor
	 */
	public PartialCallGraph construct(OPALCallGraph ocg, CallPreservationStrategy callSiteOnly) {
		pcg = new PartialCallGraph();
		pcg.graph = new JavaGraph();

		try {
			final var cha = createInternalCHA(ocg.project);

			createGraphWithExternalCHA(ocg, cha, callSiteOnly);

			pcg.nodeCount = cha.getNodeCount();
			pcg.classHierarchy = cha.asURIHierarchy(ocg.project.classHierarchy());
		} catch (Exception e) {
			if (e.getStackTrace().length > 0) {
				var stackTrace = e.getStackTrace()[0];
				if (stackTrace.toString().startsWith("org.opalj")) {
					throw new OPALException(e);
				}
			}
			throw e;
		}

		return pcg;
	}

	/**
	 * Creates a class hierarchy for the given call graph's artifact with entries
	 * only in internalCHA. ExternalCHA to be added at a later stage.
	 *
	 * @param project OPAL {@link Project}
	 * @return class hierarchy for a given package
	 * @implNote Inside {@link OPALType} all of the methods are indexed.
	 */
	private OPALClassHierarchy createInternalCHA(final Project<?> project) {
		final Map<ObjectType, OPALType> result = new HashMap<>();
		final AtomicInteger methodNum = new AtomicInteger();

		var opalAnnotations = new HashMap<String, List<Pair<String, String>>>();

		project.allClassFiles().foreach(classFile -> {

			opalAnnotations.putAll(extractAnnotations(classFile));

			final var currentClass = classFile.thisType();
			final var methods = getMethodsMap(methodNum.get(), JavaConverters.asJavaIterable(classFile.methods()));
			var namespace = OPALMethod.getPackageName(classFile.thisType());
			var filepath = namespace != null ? namespace.replace(".", "/") : "";
			final var type = new OPALType(methods, OPALType.extractSuperClasses(project.classHierarchy(), currentClass),
					OPALType.extractImplementedInterfaces(project.classHierarchy(), currentClass),
					classFile.sourceFile().isDefined() ? filepath + "/" + classFile.sourceFile().get() : "NotFound",
					classFile.isPublic() ? "public" : "packagePrivate", classFile.isFinal(), opalAnnotations);

			result.put(currentClass, type);
			methodNum.addAndGet(methods.size());
			
			return null;
		});
		return new OPALClassHierarchy(result, new HashMap<>(), methodNum.get());
	}

	private HashMap<String, List<Pair<String, String>>> extractAnnotations(ClassFile classFile) {

		var annotations = new HashMap<String, List<Pair<String, String>>>();

		classFile.annotations().foreach(annotation -> {
			
			final var annotationPackage = OPALMethod.getPackageName(annotation.annotationType());
			final var annotationClass = OPALMethod.getClassName(annotation.annotationType());

			var valueList = new ArrayList<Pair<String, String>>();
			// TODO do we need values?!
			final var values = JavaConverters.asJavaIterable(annotation.elementValuePairs());
			if (values != null) {
				for (ElementValuePair value : values) {
					try {
						final var valuePackage = OPALMethod.getPackageName(value.value().valueType());
						final var valueClass = OPALMethod.getClassName(value.value().valueType());
						final var valueContent = StringEscapeUtils.escapeJava(value.value().toJava());
						valueList.add(Pair.of(valuePackage + "/" + valueClass, valueContent));
					} catch (NullPointerException ignored) {
						// TODO fix swallowed exception
						logger.error("!! SWALLOWED EXCEPTION !!");
					}
				}
			}
			annotations.put(annotationPackage + "/" + annotationClass, valueList);

			return null;
		});

		return annotations;

	}

	/**
	 * Assign each method an id. Ids start from the the first parameter and increase
	 * by one number for every method.
	 *
	 * @param methods Iterable of {@link Method} to get mapped to ids.
	 * @return A map of passed methods and their ids.
	 * @implNote Methods are keys of the result map and values are the generated
	 *           Integer keys.
	 */
	private Map<Method, Integer> getMethodsMap(final int keyStartsFrom, final Iterable<Method> methods) {
		final Map<Method, Integer> result = new HashMap<>();
		final AtomicInteger i = new AtomicInteger(keyStartsFrom);
		for (final var method : methods) {
			result.put(method, i.get());
			i.addAndGet(1);
		}
		return result;
	}

	/**
	 * Given a call graph generated by OPAL and class hierarchy iterates over
	 * methods declared in the package that call external methods and add them to
	 * externalCHA of a call hierarchy. Build a graph for both internal and external
	 * calls in parallel.
	 * 
	 * @param ocg          call graph from OPAL generator
	 * @param cha          class hierarchy
	 * @param callSiteOnly
	 */
	private void createGraphWithExternalCHA(final OPALCallGraph ocg, final OPALClassHierarchy cha,
			CallPreservationStrategy callSiteOnly) {
		// TODO instead of relying on pcg field, use parameter
		final var cg = ocg.callGraph;
		final var tac = ocg.project.get(ComputeTACAIKey$.MODULE$);

		cg.reachableMethods().foreach(sourceDeclaration -> {

			// remember all incomplete(?) call sites
			final List<Integer> incompletes = new ArrayList<>();
			cg.incompleteCallSitesOf(sourceDeclaration).foreach(pc -> incompletes.add((int) pc));

			final Set<Integer> visitedPCs = new HashSet<>();

			var calleesOf = cg.calleesOf(sourceDeclaration);

			if (sourceDeclaration.hasMultipleDefinedMethods()) {
				sourceDeclaration.definedMethods().foreach(source -> {
					var dm = sourceDeclaration.definedMethod();
					var stmts = getStmts(tac, dm);
					cha.appendGraph(source, calleesOf, stmts, pcg.graph, incompletes, visitedPCs, callSiteOnly);
					return null;
				});
			} else if (sourceDeclaration.hasSingleDefinedMethod()) {
				final var definedMethod = sourceDeclaration.definedMethod();
				cha.appendGraph(definedMethod, calleesOf, getStmts(tac, definedMethod), pcg.graph, incompletes,
						visitedPCs, callSiteOnly);

			} else if (sourceDeclaration.isVirtualOrHasSingleDefinedMethod()) {

				var stmts = getStmts(tac, null);
				cha.appendGraph(sourceDeclaration, calleesOf, stmts, pcg.graph, incompletes, visitedPCs, callSiteOnly);
			}
			if (!incompletes.isEmpty()) {

				String msg = "Incomplete call sites discovered by OPAL (in {}): {}";
				logger.warn(msg, sourceDeclaration, incompletes);
			}

			return null;
		});
	}

	private Stmt<DUVar<ValueInformation>>[] getStmts(
			Function1<Method, AITACode<TACMethodParameter, ValueInformation>> tac, Method definedSource) {
		Stmt<DUVar<ValueInformation>>[] stmts = null;
		if (definedSource != null) {
			AITACode<TACMethodParameter, ValueInformation> sourceTac = null;
			try {
				sourceTac = tac.apply(definedSource);

			} catch (NoSuchElementException e) {
				// TODO investigate this warning, as it happens frequently in practice!
				logger.warn("couldn't find the stmt");
			}
			if (sourceTac != null) {
				stmts = sourceTac.stmts();
			}
		}
		return stmts;
	}
}