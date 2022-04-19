package eu.fasten.core.search;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.SubmissionPublisher;

import org.junit.jupiter.api.Test;

import eu.fasten.core.data.ArrayImmutableDirectedGraph;
import eu.fasten.core.search.SearchEngine.Result;
import it.unimi.dsi.bits.Fast;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterators;
import it.unimi.dsi.fastutil.longs.LongList;
import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;

public class SearchEngineTest {


    @Test
    void testBfs() throws InterruptedException {
		final ArrayImmutableDirectedGraph.Builder builder = new ArrayImmutableDirectedGraph.Builder();
		for (int x = 0; x < 5; x++) builder.addInternalNode(x);
		builder.addArc(0, 1);
		builder.addArc(1, 2);
		builder.addArc(1, 3);
		builder.addArc(2, 4);
		builder.addArc(3, 4);
		builder.addArc(4, 1);
		final ArrayImmutableDirectedGraph graph = builder.build();
    	final ObjectRBTreeSet<Result> results = new ObjectRBTreeSet<>();
    	final SubmissionPublisher<Result> publisher = new SubmissionPublisher<>();
		final SearchEngine.FullyCollectSubscriber subscriber = new SearchEngine.FullyCollectSubscriber();
		publisher.subscribe(subscriber);
		
		SearchEngine.bfs(graph, true, LongList.of(1), x -> true, TrivialScorer.getInstance(), results, 100, publisher);
		// #node #indegree #outdegree #distance (from 1)
		// 1 2 2 0
		// 2 1 1 1
		// 3 1 1 1
		// 4 2 1 2
		final int[] node      = new int[] {2,3,4};
		final int[] indegree  = new int[] {1,1,2};
		final int[] outdegree = new int[] {1,1,1};
		final int[] dist      = new int[] {1,1,2};
		final Long2DoubleOpenHashMap node2score = new Long2DoubleOpenHashMap();
		for (int i = 0; i < node.length; i++)
			node2score.put(node[i], (indegree[i] + outdegree[i]) / Fast.log2(dist[i] + 2));
		assertEquals(node.length, results.size());
		for (final Result r: results) {
			assertTrue(node2score.containsKey(r.gid));
			assertEquals(node2score.get(r.gid), r.score);
		}
		synchronized(subscriber) {
			while (!subscriber.ready) subscriber.wait();
			for (final Result r: subscriber.result) {
				assertTrue(node2score.containsKey(r.gid));
				assertEquals(node2score.get(r.gid), r.score);
			}
		
		}
		
    }


}
