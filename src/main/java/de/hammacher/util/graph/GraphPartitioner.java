package de.hammacher.util.graph;

import java.util.Collection;
import java.util.Iterator;

import de.hammacher.util.Pair;

/**
 * Used to partition a {@link Graph} into several sub-graphs.
 * Used for example in {@link Graph2Dot}.
 *
 * @author Clemens Hammacher
 *
 * @param <NodeType> the type of the graphs nodes
 */
public interface GraphPartitioner<NodeType> {

	/**
	 * Return an iterator over all subgraphs to export.
	 *
	 * The pair returned by the iterator is interpreted as the name of the
	 * subgraph, plus all nodes contained in this subgraph. One node should only
	 * be contained in one subgraph.
	 *
	 * If the returned name is <code>null</code>, this means that the corresponding
	 * nodes are not contained in any subgraph, so they belong directly to the
	 * parent graph.
	 *
	 * Note that in the dot format, there are special names denoting that a rectangle
	 * should be drawn around the subgraph ("cluster*").
	 *
	 * @return an iterator over all subgraphs
	 */
	Iterator<Pair<String, Collection<? extends NodeType>>> subGraphIterator();

}