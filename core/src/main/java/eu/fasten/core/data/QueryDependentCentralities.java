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

package eu.fasten.core.data;

import java.io.IOException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import it.unimi.dsi.fastutil.doubles.AbstractDoubleList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.longs.Long2DoubleFunction;
import it.unimi.dsi.fastutil.longs.Long2DoubleMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongCollection;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.law.rank.KatzParallelGaussSeidel;
import it.unimi.dsi.law.rank.PageRank;
import it.unimi.dsi.law.rank.PageRankParallelGaussSeidel;
import it.unimi.dsi.law.rank.PageRankPush;
import it.unimi.dsi.law.rank.SpectralRanking;
import it.unimi.dsi.law.rank.SpectralRanking.StoppingCriterion;
import it.unimi.dsi.webgraph.algo.HyperBall;

/**
 * A containers for static utility methods computing query-dependent centrality measures on instances of
 * {@link DirectedGraph}.
 *
 * <p>
 * A {@linkplain Centralities Query-<em>independent</em>} centrality measure associate a score
 * with each node of a graph. Query-<em>dependent</em> centrality measure use additional
 * information specific to a query to make the centrality dependent on a set of <em>query nodes</em>.
 *
 * <p>
 * The influence of the query nodes on the final result vary depending on the type of centrality
 * considered. For example, in the case of PageRank it is customary to set the {@link PageRank#preference <em>preference vector</em>}
 * to a probability distribution concentrated on the query nodes (in the methods provided
 * by this class, the preference vector is set to the uniform distribution on the query nodes).
 *
 * <p>
 * In the case of <em>geometric centralities</em>, such as closeness of harmonic centrality,
 * we consider a weight on the node that is zero outside of the query nodes and one on the
 * query nodes. The methods of this class assume that the query nodes are in relatively
 * small number, and thus perform a number breadth-first visit from the query nodes,
 * accumulating the results, rather than use the {@link HyperBall} approximation algorithm.
 *
 * <p>
 * We provide a method for each implementation part of D5.3. Many implementations provide a wide
 * number of options, that should be explored, if necessary, by looking at the code in this class
 * and at the related Javadoc documentation.
 *
 * <p>
 * All centralities are implemented in their <em>negative</em> form, which is the most commonly
 * used: geometric centralities use <em>incoming paths</em>, PageRank is based on <em>incoming
 * arcs</em>, etc. To obtain the positive version, it is sufficient the pass the transpose.
 *
 * <p>
 * Iterative processes stop with a threshold of 10<sup>&minus;7</sup> or after a thousand
 * iterations. The second condition is useful in case damping / attenuation factors out of range or
 * provided to {@link #pageRank(DirectedGraph, double)}, {@link #katz(DirectedGraph, double)}, etc.
 *
 * <p>
 * All methods return uniformly a {@link Long2DoubleFunction} mapping node identifiers to the
 * associated centrality score.
 *
 * <p>
 * Javadoc documentation consistently uses &ldquo;compute&rdquo; for exact computations, and
 * &ldquo;approximates&rdquo; for approximated computations; the latter can be of iterative type
 * (e.g., {@link #pageRank(DirectedGraph, double)}) or of statistical type (e.g.,
 * {@link #harmonicApproximateParallel(DirectedGraph, double)}).
 */

public class QueryDependentCentralities {

	public static final double DEFAULT_L1_THRESHOLD = 1E-7;
	private static final int MAX_ITERATIONS = 1000;
	public static final StoppingCriterion DEFAULT_STOPPING_CRITERION = SpectralRanking.or(new SpectralRanking.IterationNumberStoppingCriterion(MAX_ITERATIONS), new SpectralRanking.NormStoppingCriterion(DEFAULT_L1_THRESHOLD));

	/**
	 * Computes weighed query-dependent closeness centrality using parallel breadth-first visits.
	 *
	 * @param graph a directed graph.
	 * @param queryNodeWeights a map from the query nodes (breadth-first visits will start form these
	 *            nodes) to their weight.
	 * @return a function mapping node identifiers to their query-dependent closeness score.
	 */
	public static Long2DoubleFunction closeness(final DirectedGraph graph, final Long2DoubleMap queryNodeWeights) throws InterruptedException {
		final Long2DoubleOpenHashMap sumOfWeightedDistances = new Long2DoubleOpenHashMap();
		final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final ExecutorCompletionService<Void> executorCompletionService = new ExecutorCompletionService<>(executorService);
		for (final it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry e : queryNodeWeights.long2DoubleEntrySet()) executorCompletionService.submit(() -> {
			final long node = e.getLongKey();
			final double invWeight = 1. / e.getDoubleValue();
			final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
			queue.enqueue(node);
			final LongOpenHashSet seen = new LongOpenHashSet();
			seen.add(node);
			int d = -1;
			long sentinel = queue.firstLong();

			while (!queue.isEmpty()) {
				final long gid = queue.dequeueLong();
				if (gid == sentinel) {
					d++;
					sentinel = -1;
				}

				synchronized (sumOfWeightedDistances) {
					sumOfWeightedDistances.addTo(gid, d * invWeight);
				}

				// Note that we are reversing the computation
				final LongIterator iterator = graph.successors(gid).iterator();

				while (iterator.hasNext()) {
					final long x = iterator.nextLong();
					if (seen.add(x)) {
						if (sentinel == -1) sentinel = x;
						queue.enqueue(x);
					}
				}
			}

			return null;
		});

		for (int i = 0; i < queryNodeWeights.size(); i++) executorCompletionService.take();

		final Long2DoubleOpenHashMap result = new Long2DoubleOpenHashMap();
		for (final it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry e : sumOfWeightedDistances.long2DoubleEntrySet()) {
			final double s = e.getDoubleValue();
			if (s != 0) result.put(e.getLongKey(), 1. / s);
		}
		return result;
	}

	/**
	 * Computes query-dependent harmonic centrality using parallel breadth-first visits.
	 *
	 * @param graph a directed graph.
	 * @param queryNodes the query nodes (breadth-first visits will start form these nodes).
	 * @return a function mapping node identifiers to their query-dependent harmonic score.
	 */
	public static Long2DoubleFunction harmonic(final DirectedGraph graph, final Long2DoubleMap queryNodeWeights) throws InterruptedException {
		final Long2DoubleOpenHashMap result = new Long2DoubleOpenHashMap();
		final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final ExecutorCompletionService<Void> executorCompletionService = new ExecutorCompletionService<>(executorService);
		for (final it.unimi.dsi.fastutil.longs.Long2DoubleMap.Entry e : queryNodeWeights.long2DoubleEntrySet()) executorCompletionService.submit(() -> {
			final long node = e.getLongKey();
			final double weight = e.getDoubleValue();
			final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
			queue.enqueue(node);
			final LongOpenHashSet seen = new LongOpenHashSet();
			seen.add(node);
			int d = -1;
			long sentinel = queue.firstLong();

			while (!queue.isEmpty()) {
				final long gid = queue.dequeueLong();
				if (gid == sentinel) {
					d++;
					sentinel = -1;
				}

				synchronized (result) {
					if (gid != node) result.addTo(gid, weight / d);
				}

				// Note that we are reversing the computation
				final LongIterator iterator = graph.successors(gid).iterator();

				while (iterator.hasNext()) {
					final long x = iterator.nextLong();
					if (seen.add(x)) {
						if (sentinel == -1) sentinel = x;
						queue.enqueue(x);
					}
				}
			}

			return null;
		});

		for (int i = 0; i < queryNodeWeights.size(); i++) executorCompletionService.take();
		return result;
	}

	/**
	 * Computes query-dependent closeness centrality using parallel breadth-first visits.
	 *
	 * @param graph a directed graph.
	 * @param queryNodes the query nodes (breadth-first visits will start form these nodes).
	 * @return a function mapping node identifiers to their query-dependent closeness score.
	 */
	public static Long2DoubleFunction closeness(final DirectedGraph graph, final LongCollection queryNodes) throws InterruptedException {
		final Long2LongOpenHashMap sumOfDistances = new Long2LongOpenHashMap();
		final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		final ExecutorCompletionService<Void> executorCompletionService = new ExecutorCompletionService<>(executorService);
		for (final long node : queryNodes) executorCompletionService.submit(() -> {
			final LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
			queue.enqueue(node);
			final LongOpenHashSet seen = new LongOpenHashSet();
			seen.add(node);
			int d = -1;
			long sentinel = queue.firstLong();

			while (!queue.isEmpty()) {
				final long gid = queue.dequeueLong();
				if (gid == sentinel) {
					d++;
					sentinel = -1;
				}

				synchronized (sumOfDistances) {
					sumOfDistances.addTo(gid, d);
				}

				// Note that we are reversing the computation
				final LongIterator iterator = graph.successors(gid).iterator();

				while (iterator.hasNext()) {
					final long x = iterator.nextLong();
					if (seen.add(x)) {
						if (sentinel == -1) sentinel = x;
						queue.enqueue(x);
					}
				}
			}

			return null;
		});

		for (final Long queryNode : queryNodes) executorCompletionService.take();

		final Long2DoubleOpenHashMap result = new Long2DoubleOpenHashMap();
		for (final Entry e : sumOfDistances.long2LongEntrySet()) {
			final long s = e.getLongValue();
			if (s != 0) result.put(e.getLongKey(), 1. / s);
		}
		return result;
	}

	/**
	 * Computes query-dependent harmonic centrality using parallel breadth-first visits.
	 *
	 * @param graph a directed graph.
	 * @param queryNodes the query nodes (breadth-first visits will start form these nodes).
	 * @return a function mapping node identifiers to their query-dependent harmonic score.
	 */
	public static Long2DoubleFunction harmonic(final DirectedGraph graph, final LongCollection queryNodes) throws InterruptedException {
		final Long2DoubleOpenHashMap queryNodeWeights = new Long2DoubleOpenHashMap();
		for (final long node : queryNodes) queryNodeWeights.put(node, 1.);
		return harmonic(graph, queryNodeWeights);
	}


	/** Given a graph (represented as an {@link ImmutableGraphAdapter}) and a collection of
	 *  node identifiers, it returns a preference vector with as many elements as there are nodes
	 *  in the graph, where the value associated to a node is either 0 (if the node is outside
	 *  of the collection) or 1./c (if the node is inside the collection).
	 *
	 * @param immutableGraphAdapter the graph.
	 * @param queryNodes the nodes that should have nonzero preference.
	 * @return the preference vector.
	 */
	private static DoubleList preferenceVector(final ImmutableGraphAdapter immutableGraphAdapter, final LongCollection queryNodes) {
		final int n = immutableGraphAdapter.numNodes();
		final double c = 1. / queryNodes.size();
		final var x = new AbstractDoubleList() {
			@Override
			public double getDouble(final int u) {
				return queryNodes.contains(immutableGraphAdapter.node2Id(u)) ? c : 0;
			}

			@Override
			public int size() {
				return n;
			}
		};
		//final double[] pref = new double[n];
		//for (long id: queryNodes) pref[immutableGraphAdapter.id2Node(id)] = c;
		return x;
	}


	/**
	 * Approximates Katz centrality using a parallel implementation of the Gauss&ndash;Seidel method.
	 *
	 * @param directedGraph a directed graph.
	 * @param queryNodes the query nodes. The preference vector is set to zero everywhere, except
	 * for the queryNodes where it is uniform.
	 * @return a function mapping node identifiers to their centrality score.
	 */
	public static Long2DoubleFunction katzParallel(final DirectedGraph directedGraph, final LongCollection queryNodes, final double alpha) throws IOException {
		final ImmutableGraphAdapter immutableGraphAdapter = new ImmutableGraphAdapter(directedGraph);
		final KatzParallelGaussSeidel katzParallelGaussSeidel = new KatzParallelGaussSeidel(immutableGraphAdapter.transpose());
		katzParallelGaussSeidel.preference = preferenceVector(immutableGraphAdapter, queryNodes);
		katzParallelGaussSeidel.alpha = alpha;
		katzParallelGaussSeidel.stepUntil(DEFAULT_STOPPING_CRITERION);
		return id -> katzParallelGaussSeidel.rank[immutableGraphAdapter.id2Node(id)];
	}

	/**
	 * Approximates PageRank using a parallel implementation of the Gauss&ndash;Seidel method.
	 *
	 * @param directedGraph a directed graph.
	 * @param queryNodes the query nodes. The preference vector is set to zero everywhere, except
	 * for the queryNodes where it is uniform.
	 * @param alpha the damping factor.
	 * @return a function mapping node identifiers to their centrality score.
	 */
	public static Long2DoubleFunction pageRankParallel(final DirectedGraph directedGraph, final LongCollection queryNodes, final double alpha) throws IOException {
		final ImmutableGraphAdapter immutableGraphAdapter = new ImmutableGraphAdapter(directedGraph);
		final PageRankParallelGaussSeidel pageRankParallelGaussSeidel = new PageRankParallelGaussSeidel(immutableGraphAdapter.transpose());
		pageRankParallelGaussSeidel.preference = preferenceVector(immutableGraphAdapter, queryNodes);
		pageRankParallelGaussSeidel.alpha = alpha;
		pageRankParallelGaussSeidel.stepUntil(DEFAULT_STOPPING_CRITERION);
		return id -> pageRankParallelGaussSeidel.rank[immutableGraphAdapter.id2Node(id)];
	}

	/**
	 * Approximates PageRank using the push method; it can only be called for a single query node.
	 *
	 * @param directedGraph a directed graph.
	 * @param queryNode the query nodes. 
	 * @param alpha the damping factor.
	 * @return a function mapping node identifiers to their centrality score.
	 */
	public static Long2DoubleFunction pageRankPush(final DirectedGraph directedGraph, final long queryNode, final double alpha) throws IOException {
		final ImmutableGraphAdapter immutableGraphAdapter = new ImmutableGraphAdapter(directedGraph);
		final PageRankPush pageRankPush = new PageRankPush(immutableGraphAdapter, false);
		pageRankPush.root = immutableGraphAdapter.id2Node(queryNode);
		pageRankPush.alpha = alpha;
		pageRankPush.threshold = DEFAULT_L1_THRESHOLD;
		pageRankPush.stepUntil(new PageRankPush.EmptyQueueStoppingCritertion());

		return id -> pageRankPush.rank[pageRankPush.node2Seen.get(immutableGraphAdapter.id2Node(id))] / pageRankPush.pNorm;
	}


}
